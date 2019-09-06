package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.detector.api.JavaContext
import org.jetbrains.uast.*
import org.jetbrains.uast.util.isMethodCall
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * Check if target method is expensive.
 */
class MethodChecker(
    private val context: JavaContext,
    private val targetMethodCall: UCallExpression
) {

    fun isExpensive(): Boolean {
        val containingUClass = targetMethodCall.getContainingUClass()
        if (containingUClass != null && targetMethodCall.receiver == null) {
            // this.method()
            return checkMethodOfClass(containingUClass)
        }
        val className = targetMethodCall.receiver?.getExpressionType()?.getCanonicalText(false) ?: return true
        val clazz = context.evaluator.findClass(className) ?: return true
        val uClass = context.uastContext.getClass(clazz)
        return checkMethodOfClass(uClass)
    }

    private fun checkMethodOfClass(uClass: UClass): Boolean {
        val methodVisitor = MethodVisitor(targetMethodCall)
        uClass.accept(methodVisitor)
        return methodVisitor.isExpensive
    }

    private class MethodVisitor(
        private val targetMethodCall: UCallExpression
    ) : AbstractUastVisitor() {
        private var _isExpensive = true
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
                val expressions = uastBody.expressions

                for (expr in expressions) {
                    if (expr is UReturnExpression && isAvailableExpression(expr.returnExpression!!)) {
                        continue
                    }
                    if (expr is UBinaryExpression && (isAvailableExpression(expr.leftOperand) &&
                                isAvailableExpression(expr.rightOperand))
                    ) {
                        continue
                    }
                    if (expr is UQualifiedReferenceExpression) {
                        val callingSelector = expr.selector
                        if (callingSelector.isMethodCall() && callingSelector is UCallExpression) {
                            if (ClassMembersChecks.isExcludedClassInExpression(callingSelector)) {
                                continue
                            }
                        }
                    }
                    if (expr is UIfExpression) {
                        // TODO: should use some parameter to disable this exclusions.
                        continue
                    }
                    // if not found any allowed expressions then there is not allowed.
                    return false
                }
            }
            return true
        }

        private fun isAvailableExpression(expr: UExpression): Boolean {
            if (expr is ULiteralExpression) {
                return true
            }

            if (expr is USimpleNameReferenceExpression) {
                return true
            }

            if (expr is UBinaryExpression) {
                return isAvailableBinaryExpression(expr)
            }

            if (expr is UPolyadicExpression) {
                return isAvailablePolyadicExpression(expr)
            }

            if (expr is UCallExpression) {
                return isAvailableCallExpression(expr)
            }

            if (expr is UParenthesizedExpression) {
                return isAvailableExpression(expr.expression)
            }

            return false
        }

        private fun isAvailableCallExpression(expr: UCallExpression): Boolean {
            if (!expr.isMethodCall()) {
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
            return isAvailableExpression(expr.leftOperand) && isAvailableExpression(expr.rightOperand)
        }

        private fun isTargetMethod(node: UMethod): Boolean {
            if (node.name == targetMethodCall.methodName) {
                return true
            }
            return false
        }
    }
}
