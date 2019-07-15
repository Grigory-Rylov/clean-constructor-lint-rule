package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Detector.UastScanner
import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.java.PsiNewExpressionImpl
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.uast.UClass


/**
 * Detects heavy constructors.
 */
class CleanConstructorDetector : Detector(), UastScanner {

    override fun getApplicableUastTypes() = listOf(UClass::class.java)

    override fun createUastHandler(context: JavaContext) = NamingPatternHandler(context)

    class NamingPatternHandler(
        private val context: JavaContext
    ) : UElementHandler() {

        override fun visitClass(node: UClass) {
            val visitor = ViewMethodElementsVisitor(context, node)
            for (method in node.allMethods) {
                if (method.isConstructor) {
                    checkConstructor(method, visitor)
                }
            }
        }

        private fun checkConstructor(
            method: PsiMethod,
            visitor: PsiElementVisitor
        ) {
            //TODO: check @Inject annotation.
            method.accept(visitor)

            if (method is KtLightMethod) {
                //TODO: make solution for kotlin.
            }
        }
    }

    private class ViewMethodElementsVisitor(
        private val context: JavaContext,
        private val clazz: UClass
    ) : JavaRecursiveElementVisitor() {

        override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
            if (reference.parent !is PsiNewExpressionImpl) return
            val className = reference.qualifiedName
            val resolved = reference.resolve()
            if ((resolved is PsiClass)) {
                for (constructor in resolved.constructors) {
                    checkReferenceConstructors(className, constructor)
                }
            }
        }

        private fun checkReferenceConstructors(className: String, constructor: PsiMethod) {
            val constructorVisitor = ReferenceConstructorChecker()
            constructor.accept(constructorVisitor)
            if (constructorVisitor.hasExpensiveConstructor()) {
                context.report(
                    CleanConstructorsRegistry.ISSUE, clazz,
                    context.getNameLocation(clazz),
                    "Constructor creates object that has expensive constructor: ${className}"
                )
            }
        }

        override fun visitExpressionStatement(statement: PsiExpressionStatement) {
            val expression = statement.expression
            if (expression is PsiMethodCallExpression) {
                if (!isAllowedMethod(expression.methodExpression)) {
                    context.report(
                        CleanConstructorsRegistry.ISSUE, clazz,
                        context.getNameLocation(clazz),
                        "Constructor has expensive method calls: ${expression.methodExpression.referenceName}"
                    )
                }
            }
        }
    }

    private class ReferenceConstructorChecker : JavaRecursiveElementVisitor() {
        private var hasExpensiveConstructor = false

        override fun visitExpressionStatement(statement: PsiExpressionStatement) {
            val expression = statement.expression
            if (expression is PsiMethodCallExpression) {
                if (!isAllowedMethod(expression.methodExpression)) {
                    hasExpensiveConstructor = true
                }
            }
        }

        fun hasExpensiveConstructor() = hasExpensiveConstructor
    }

    companion object {
        private val LISTENERS_NAME = listOf(
            "setListener", "addListener",
            "addObserver", "registerObserver"
        )

        private val ACCEPTED_METHODS = listOf("this", "super")

        private fun isAllowedMethod(expression: PsiReferenceExpression): Boolean {
            val methodName = expression.referenceName
            return LISTENERS_NAME.contains(methodName) || ACCEPTED_METHODS.contains(methodName)
        }
    }
}
