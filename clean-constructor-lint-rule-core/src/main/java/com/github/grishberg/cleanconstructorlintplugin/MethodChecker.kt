package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.detector.api.JavaContext
import org.jetbrains.uast.*
import org.jetbrains.uast.java.JavaUCodeBlockExpression
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
            val uastBody = node.uastBody
            if (uastBody is JavaUCodeBlockExpression) {
                val expressions = uastBody.expressions
                if (expressions.size > 1) {
                    return true
                }
                val expr = expressions.first()
                if (expr is UReturnExpression) {
                    _isExpensive = !isAvailableExpressionForReturning(expr)
                }
            }
            return true
        }

        private fun isAvailableExpressionForReturning(expr: UReturnExpression): Boolean {
            if (expr.returnExpression is ULiteralExpression) {
                return true
            }

            return expr.returnExpression is USimpleNameReferenceExpression

        }

        private fun isTargetMethod(node: UMethod): Boolean {
            if (node.name == targetMethodCall.methodName) {
                return true
            }
            return false
        }
    }
}
