package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.detector.api.JavaContext
import com.github.grishberg.cleanconstructorlintplugin.CleanConstructorDetector.Companion.ISSUE
import org.jetbrains.uast.*
import org.jetbrains.uast.util.isConstructorCall
import org.jetbrains.uast.util.isMethodCall
import org.jetbrains.uast.visitor.AbstractUastVisitor

interface ReportingStrategy {
    fun report(node: UCallExpression, message: String)
}

class ContextReportingStrategy(
    private val context: JavaContext,
    private val parent: UElement
) : ReportingStrategy {
    override fun report(node: UCallExpression, message: String) {
        context.report(
            ISSUE, parent,
            context.getNameLocation(node),
            message
        )
    }
}

private class NonReportingStrategy : ReportingStrategy {
    override fun report(node: UCallExpression, message: String) {
        /* stub */
    }
}

class ConstructorsMethodsVisitor(
    private val context: JavaContext,
    private val membersChecks: ClassMembersChecks,
    private val strategy: ReportingStrategy = NonReportingStrategy()
) : AbstractUastVisitor() {
    private var isExpensiveConstructor: Boolean = false

    fun isExpensiveConstructor() = isExpensiveConstructor

    override fun visitCallExpression(node: UCallExpression): Boolean {
        if (membersChecks.isCallInAnonymousClass(node)) {
            return false
        }
        val uClass = node.getContainingUClass() as UClass

        if (membersChecks.isIgnoredSupertype(uClass, context)) {
            return false
        }
        if (membersChecks.isExcludedClassInExpression(node)) {
            return false
        }
        if (node.isMethodCall()) {
            val methodName =
                if (node.methodName != null) node.methodName else node.methodIdentifier?.name
            if (methodName != null && !membersChecks.isAllowedIdentifier(methodName)) {
                val methodChecker = MethodChecker(context, membersChecks, node)
                if (!methodChecker.isExpensive()) {
                    return true
                }

                isExpensiveConstructor = true

                if (!membersChecks.isPrivateClass(uClass)) {
                    strategy.report(node, "Constructor has expensive method calls: $methodName")
                }
            }
            return false
        }
        val identifier = node.methodIdentifier

        identifier?.uastParent?.let {
            val method = it.tryResolve() as? UMethod
            if (method != null) {
                if (!method.isConstructor && !membersChecks.isAllowedMethod(method)) {
                    isExpensiveConstructor = true
                    if (!membersChecks.isPrivateClass(uClass)) {
                        strategy.report(
                            node,
                            "Constructor has expensive method calls: ${method.name}"
                        )
                    }
                }
            }
        }
        if (node.isConstructorCall()) {
            if (checkConstructorsOfConstructorCall(node)) {
                isExpensiveConstructor = true
                return false
            }
        }
        return true
    }

    private fun checkConstructorsOfConstructorCall(call: UCallExpression): Boolean {
        var result = false
        val callerClass = call.getContainingUClass()

        val classReference = call.classReference
        if (classReference != null && callerClass != null) {
            val resolved = classReference.resolveToUElement()
            if ((resolved is UClass)) {
                for (method in resolved.methods) {
                    if (!method.isConstructor) {
                        continue
                    }
                    if (checkReferenceConstructors(method)) {
                        result = true
                    }
                }
            }
        }
        return result
    }

    private fun checkReferenceConstructors(constructor: UMethod): Boolean {
        val constructorVisitor = ConstructorsMethodsVisitor(context, membersChecks)
        constructor.accept(constructorVisitor)
        if (constructorVisitor.isExpensiveConstructor()) {
            return true
        }
        return false
    }
}