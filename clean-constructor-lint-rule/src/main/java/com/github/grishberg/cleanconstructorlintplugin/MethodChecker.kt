package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.detector.api.JavaContext
import org.jetbrains.uast.*
import org.jetbrains.uast.util.isConstructorCall
import org.jetbrains.uast.util.isMethodCall
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * Check if target method is expensive.
 */
class MethodChecker(
    private val context: JavaContext,
    private val membersChecks: ClassMembersChecks,
    private val targetMethodCall: UCallExpression
) {

    fun isExpensive(): Boolean {
        val containingUClass = targetMethodCall.getContainingUClass()
        if (containingUClass != null && targetMethodCall.receiver == null) {
            // in this case is called this.someMethod()
            return checkMethodOfClass(containingUClass)
        }
        // ignore if this is abstract method
        val uElement = targetMethodCall.resolveToUElement()
        if (uElement is UMethod) {
            if (membersChecks.isAbstractMethod(uElement)) {
                return false
            }
        }
        val className = targetMethodCall.receiver?.getExpressionType()?.getCanonicalText(false) ?: return true
        val clazz = context.evaluator.findClass(className) ?: return true
        val uClass = context.uastContext.getClass(clazz)
        return checkMethodOfClass(uClass)
    }

    private fun checkMethodOfClass(uClass: UClass): Boolean {
        val methodVisitor = MethodVisitor(membersChecks, targetMethodCall)
        uClass.accept(methodVisitor)
        return methodVisitor.isExpensive
    }

    private class MethodVisitor(
        private val membersChecks: ClassMembersChecks,
        private val targetMethodCall: UCallExpression
    ) : AbstractUastVisitor() {
        private var _isExpensive = false
        val isExpensive: Boolean
            get() = _isExpensive

        override fun visitMethod(node: UMethod): Boolean {
            if (!isTargetMethod(node)) {
                return true
            }
            _isExpensive = !isAvailableMethod(node)
            return true
        }

        private fun isAvailableMethod(node: UMethod): Boolean {
            val uastBody = node.uastBody
            if (uastBody is UBlockExpression) {
                return isAvailableBlockExpression(uastBody)
            }
            if (uastBody == null) {
                /**
                 * There is unknown method or interface.
                 */
                return isAvailableMethodWithoutBody(node)
            }
            return true
        }

        private fun isAvailableMethodWithoutBody(node: UMethod): Boolean {
            if (membersChecks.isAllowedMethod(node)) {
                return true
            }

            val uastParent = node.uastParent
            if (uastParent is UClass) {
                if (uastParent.isInterface || membersChecks.isAbstractClass(uastParent)) {
                    // TODO(grishberg) : check methods of all implementations.
                    return true
                }
            }
            // This is some unknown method.
            return false
        }


        private fun isAvailableBlockExpression(blockExpression: UBlockExpression): Boolean {
            val expressions = blockExpression.expressions

            for (expr in expressions) {
                if (!isAvailableExpression(expr)) {
                    return false
                }
                // if not found any allowed expressions then there is not allowed.
            }
            return true
        }

        private fun isAvailableQualifiedReferenceExpression(
            expr: UQualifiedReferenceExpression
        ): Boolean {
            val callingSelector = expr.selector
            if (callingSelector.isMethodCall() && callingSelector is UCallExpression) {
                if (membersChecks.isExcludedClassInExpression(callingSelector)) {
                    return true
                }
                val uElement = callingSelector.resolveToUElement()
                if (uElement is UMethod) {
                    if (membersChecks.isAllowedMethod(uElement)) {
                        return true
                    }
                    val visitor = MethodVisitor(membersChecks, callingSelector)
                    uElement.accept(visitor)
                    if (!visitor.isExpensive) {
                        return true
                    }
                } else {
                    // not valid situation.
                    return false
                }
            }
            return false
        }

        private fun isAvailableDeclarationsExpression(expr: UDeclarationsExpression): Boolean {
            for (declaration in expr.declarations) {
                if (declaration is ULocalVariable && declaration.uastInitializer != null) {
                    val initializer = declaration.uastInitializer
                    if (initializer is UQualifiedReferenceExpression) {
                        if (isAvailableQualifiedReferenceExpression(initializer)) {
                            continue
                        }
                    }
                }
            }
            return true
        }

        private fun isAvailableExpression(expr: UExpression?): Boolean {
            if (expr == null) {
                return true
            }
            return when (expr) {
                is UastEmptyExpression,
                is ULiteralExpression,
                is USimpleNameReferenceExpression -> true
                is UPrefixExpression -> isAvailableExpression(expr.operand)
                is UPostfixExpression -> isAvailableExpression(expr.operand)
                is UBinaryExpression -> isAvailableBinaryExpression(expr)
                is UPolyadicExpression -> isAvailablePolyadicExpression(expr)
                is UCallExpression -> isAvailableCallExpression(expr)
                is UParenthesizedExpression -> isAvailableExpression(expr.expression)
                is UIfExpression -> isAvailableIfExpression(expr)
                is UBlockExpression -> isAvailableBlockExpression(expr)
                is UReturnExpression -> {
                    val returnExpression = expr.returnExpression ?: return true
                    return isAvailableExpression(returnExpression)
                }
                is UQualifiedReferenceExpression -> return isAvailableQualifiedReferenceExpression(expr)
                is UDeclarationsExpression -> return isAvailableDeclarationsExpression(expr)
                is UForExpression -> return isAvailableForExpression(expr)
                is UForEachExpression -> return isAvailableForEachExpression(expr)
                else -> return false
            }

        }

        private fun isAvailableForEachExpression(expr: UForEachExpression): Boolean {
            if (!isAvailableExpression(expr.body)) {
                return false
            }
            return false
        }

        private fun isAvailableForExpression(expr: UForExpression): Boolean {
            if (!isAvailableExpression(expr.condition)) {
                return false
            }
            if (!isAvailableExpression(expr.declaration)) {
                return false
            }
            if (!isAvailableExpression(expr.update)) {
                return false
            }
            if (!isAvailableExpression(expr.body)) {
                return false
            }
            return true
        }

        private fun isAvailableIfExpression(expr: UIfExpression): Boolean {
            if (!isAvailableExpression(expr.condition)) {
                return false
            }
            if (!isAvailableExpression(expr.thenExpression)) {
                return false
            }
            if (!isAvailableExpression(expr.elseExpression)) {
                return false
            }
            return true
        }

        private fun isAvailableCallExpression(expr: UCallExpression): Boolean {
            if (!expr.isMethodCall() && !expr.isConstructorCall()) {
                return false
            }
            val methodCall = expr.resolveToUElement() as? UMethod ?: return false
            return isAvailableMethod(methodCall)
        }

        private fun isAvailablePolyadicExpression(expr: UPolyadicExpression): Boolean {
            for (operand in expr.operands) {
                if (!isAvailableExpression(operand)) {
                    return false
                }
            }
            return true
        }

        private fun isAvailableBinaryExpression(expr: UBinaryExpression): Boolean {
            return isAvailableExpression(expr.leftOperand) && isAvailableExpression(
                expr.rightOperand
            )
        }

        private fun isTargetMethod(node: UMethod): Boolean {
            return node.name == targetMethodCall.methodName
        }
    }
}
