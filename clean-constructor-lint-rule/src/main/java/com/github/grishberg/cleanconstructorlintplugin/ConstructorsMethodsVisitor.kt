package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.detector.api.JavaContext
import org.jetbrains.uast.*
import org.jetbrains.uast.util.isMethodCall
import org.jetbrains.uast.visitor.AbstractUastVisitor

class ConstructorsMethodsVisitor(
    private val context: JavaContext,
    private val excludedClasses: ClassMembersChecks,
    private val parent: UElement
) : AbstractUastVisitor() {

    override fun visitCallExpression(node: UCallExpression): Boolean {
        if (excludedClasses.isCallInAnonymousClass(node)) {
            return false
        }
        val uClass = node.getContainingUClass() as UClass
        if (excludedClasses.isPrivateClass(uClass)) {
            return false
        }
        if (excludedClasses.isIgnoredSupertype(uClass, context)) {
            return false
        }
        if (ClassMembersChecks.isExcludedClassInExpression(node)) {
            return false
        }
        if (node.isMethodCall()) {
            val methodName = node.methodName
            if (methodName != null && !excludedClasses.isAllowedIdentifier(methodName)) {
                val methodChecker = MethodChecker(context, node)
                if (!methodChecker.isExpensive()) {
                    return true
                }

                context.report(
                    CleanConstructorsRegistry.ISSUE, parent,
                    context.getNameLocation(node),
                    "Constructor has expensive method calls: $methodName"
                )
            }
            return false
        }
        val identifier = node.methodIdentifier

        identifier?.uastParent?.let {
            val method = it.tryResolve() as? UMethod
            if (method != null) {
                if (!method.isConstructor && !excludedClasses.isAllowedMethod(method)) {
                    context.report(
                        CleanConstructorsRegistry.ISSUE, parent,
                        context.getNameLocation(node),
                        "Constructor has expensive method calls: ${method.name}"
                    )
                }
            }
        }
        return true
    }
}