package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.detector.api.JavaContext
import org.jetbrains.uast.*
import org.jetbrains.uast.util.isConstructorCall
import org.jetbrains.uast.util.isMethodCall
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * Visit method calls inside current method.
 */
class ReferenceConstructorChecker(
    private val context: JavaContext,
    private val membersChecks: ClassMembersChecks
) : AbstractUastVisitor() {
    private var hasExpensiveConstructor = false

    override fun visitCallExpression(call: UCallExpression): Boolean {
        if (membersChecks.isCallInAnonymousClass(call)) {
            return false
        }

        val uClass = call.getContainingUClass() as UClass
        if (membersChecks.isIgnoredSupertype(uClass, context)) {
            return false
        }
        if (call.isMethodCall()) {
            if (ClassMembersChecks.isExcludedClassInExpression(call)) {
                return false
            }
            val methodName = call.methodName
            if (methodName != null && !membersChecks.isAllowedIdentifier(methodName)) {
                hasExpensiveConstructor = true
                return false
            }
            return false
        }

        if (call.isConstructorCall()) {
            if (checkConstructorsOfConstructorCall(call)) {
                hasExpensiveConstructor = true
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
        val constructorVisitor = ReferenceConstructorChecker(context, membersChecks)
        constructor.accept(constructorVisitor)
        if (constructorVisitor.hasExpensiveConstructor()) {
            return true
        }
        return false
    }

    fun hasExpensiveConstructor() = hasExpensiveConstructor
}