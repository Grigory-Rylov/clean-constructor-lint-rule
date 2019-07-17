package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Detector.UastScanner
import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.*
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.impl.source.tree.java.PsiNewExpressionImpl
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.tryResolve
import org.jetbrains.uast.util.isConstructorCall
import org.jetbrains.uast.visitor.AbstractUastVisitor


/**
 * Detects heavy constructors.
 */
class CleanConstructorDetector : Detector(), UastScanner {

    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java, UMethod::class.java)

    override fun createUastHandler(context: JavaContext) = NamingPatternHandler(context)

    class NamingPatternHandler(
        private val context: JavaContext
    ) : UElementHandler() {
        override fun visitCallExpression(call: UCallExpression) {
            if(!call.isConstructorCall()) {
                return
            }
            //TODO: check constructor.
            //println(call)
        }

        override fun visitMethod(node: UMethod) {
            if (!node.isConstructor) {
                return
            }
            if (node.parent is PsiClass && isIgnoredSupertype(node.parent as PsiClass)) {
                return
            }
            node.accept(ConstructorsMethodsVisitor(context, node.parent))
        }

        private fun isIgnoredSupertype(node: PsiClass): Boolean {
            for (superType in node.supers) {
                if (superType is ClsClassImpl) {
                    if (IGNORED_PARENTS.contains(superType.stub.qualifiedName)) {
                        return true
                    }
                }
            }
            return false
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
                    context.getNameLocation(constructor),
                    "Constructor creates object that has expensive constructor: $className"
                )
            }
        }

        override fun visitExpressionStatement(statement: PsiExpressionStatement) {
            val expression = statement.expression
            if (expression is PsiMethodCallExpression) {
                if (!isAllowedMethod(expression.methodExpression)) {
                    context.report(
                        CleanConstructorsRegistry.ISSUE, clazz,
                        context.getNameLocation(expression.methodExpression),
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

    class ConstructorsMethodsVisitor(
        private val context: JavaContext,
        private val parent: PsiElement
    ) : AbstractUastVisitor() {
        override fun visitCallExpression(node: UCallExpression): Boolean {
            val identifier = node.methodIdentifier

            identifier?.uastParent?.let {
                val method = it.tryResolve() as? PsiMethod
                if (method != null) {
                    if (!method.isConstructor && !isAllowedMethod(method)) {
                        context.report(
                            CleanConstructorsRegistry.ISSUE, parent,
                            context.getNameLocation(node),
                            "Constructor has expensive method calls: ${method.name}"
                        )
                    }
                } else {
                    if (!isAllowedMethod(node)) {
                        context.report(
                            CleanConstructorsRegistry.ISSUE, parent,
                            context.getNameLocation(node),
                            "Constructor has expensive method calls: ${identifier.name}"
                        )
                    }
                }


            }
            return false
        }
    }

    companion object {
        private val LISTENERS_NAME = listOf(
            "setListener", "addListener",
            "addObserver", "registerObserver"
        )

        private val ACCEPTED_METHODS = listOf("this", "super")

        private val IGNORED_PARENTS = listOf(
            "android.graphics.drawable.Drawable",
            "android.view.View"
        )

        private fun isAllowedMethod(expression: PsiReferenceExpression): Boolean {
            val methodName = expression.referenceName
            return LISTENERS_NAME.contains(methodName) || ACCEPTED_METHODS.contains(methodName)
        }

        private fun isAllowedMethod(expression: PsiMethod): Boolean {
            val methodName = expression.name
            return LISTENERS_NAME.contains(methodName) || ACCEPTED_METHODS.contains(methodName)
        }

        private fun isAllowedMethod(expression: UCallExpression): Boolean {
            val methodIdentifier = expression.methodIdentifier ?: return false
            return LISTENERS_NAME.contains(methodIdentifier.name) || ACCEPTED_METHODS.contains(methodIdentifier.name)

        }
    }
}

