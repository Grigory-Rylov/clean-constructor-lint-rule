package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Detector.UastScanner
import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.*
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.impl.source.tree.java.PsiNewExpressionImpl
import org.jetbrains.uast.*
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
            if (!call.isConstructorCall()) {
                return
            }
            val callerClass = call.getContainingUClass()

            val classReference = call.classReference
            if (classReference != null && callerClass != null) {
                val resolved = classReference.resolve()
                if ((resolved is PsiClass)) {
                    for (constructor in resolved.constructors) {
                        checkReferenceConstructors(callerClass, constructor, call)
                    }
                }
            }
        }

        private fun checkReferenceConstructors(callerClass: UClass, constructor: PsiMethod, call: UCallExpression) {
            val constructorVisitor = ReferenceConstructorChecker()
            constructor.accept(constructorVisitor)
            if (constructorVisitor.hasExpensiveConstructor()) {
                context.report(
                    CleanConstructorsRegistry.ISSUE, call,
                    context.getNameLocation(call),
                    "Constructor creates object that has expensive constructor: ${callerClass.name}"
                )
            }
        }

        override fun visitMethod(node: UMethod) {
            if (!node.isConstructor) {
                return
            }
            if (node.parent is PsiClass && isIgnoredSupertype(node.parent as PsiClass)) {
                return
            }
            checkConstructor(node)
        }

        private fun checkConstructor(constructorMethod: UMethod) {
            constructorMethod.accept(ConstructorsMethodsVisitor(context, constructorMethod.parent))

            if (hasInjectAnnotation(constructorMethod.getAnnotations())) {
                checkConstructorParametersHasExpensiveConstructor(constructorMethod, shouldReport = true)
            }
        }

        private fun checkConstructorParametersHasExpensiveConstructor(
            constructor: UMethod,
            shouldReport: Boolean = false
        ): Boolean {
            for (param in constructor.uastParameters) {
                val cn = param.typeReference?.getQualifiedName()
                if (cn != null) {
                    val clazz = context.evaluator.findClass(cn) ?: continue
                    val constructorVisitor = ReferenceConstructorChecker()
                    clazz.accept(constructorVisitor)
                    if (constructorVisitor.hasExpensiveConstructor()) {
                        if (shouldReport) {
                            context.report(
                                CleanConstructorsRegistry.ISSUE, constructor,
                                context.getNameLocation(param),
                                "Constructor with @Inject annotation injected object that has expensive constructor: $cn"
                            )
                        }
                        return true
                    }

                    for (c in clazz.constructors) {
                        if (hasInjectAnnotation(c.annotations)) {
                            if (checkConstructorParametersHasExpensiveConstructor(c)) {
                                if (shouldReport) {
                                    context.report(
                                        CleanConstructorsRegistry.ISSUE, constructor,
                                        context.getNameLocation(constructor),
                                        "Constructor with @Inject annotation injected object that has expensive constructor: ${c.name}"
                                    )
                                }
                                return true
                            }
                        }
                    }

                }
            }
            return false
        }

        private fun checkConstructorParametersHasExpensiveConstructor(constructor: PsiMethod): Boolean {
            for (param in constructor.parameters) {
                val type: PsiType = (param as PsiParameter).type
                val cn = type.canonicalText
                val clazz = context.evaluator.findClass(cn) ?: continue
                val constructorVisitor = ReferenceConstructorChecker()
                clazz.accept(constructorVisitor)
                if (constructorVisitor.hasExpensiveConstructor()) {
                    return true
                }

                for (c in clazz.constructors) {
                    if (hasInjectAnnotation(c.annotations)) {
                        if (checkConstructorParametersHasExpensiveConstructor(c)) {
                            return true
                        }
                    }
                }
            }
            return false
        }

        private fun hasInjectAnnotation(annotations: Array<out PsiAnnotation>): Boolean {
            for (a in annotations) {
                val nameReferenceElement = a.nameReferenceElement
                if (nameReferenceElement != null && nameReferenceElement.qualifiedName == "Inject") {
                    return true
                }
            }
            return false
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

    /**
     * Visit method calls inside current method.
     */
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

    class ParamTypeVisitor {

    }

    companion object {
        private val ACCEPTED_METHODS = listOf("this", "super")

        private val IGNORED_PARENTS = listOf(
            "android.graphics.drawable.Drawable",
            "android.view.View"
        )

        private val REGEX_LISTENERS = listOf(
            "add\\w*Listener".toRegex(),
            "add\\w*Observer".toRegex(),
            "remove\\w*Observer".toRegex(),
            "set\\w*Listener".toRegex(),
            "register\\w*Listener".toRegex(),
            "register\\w*Observer".toRegex(),
            "unregister\\w*Listener".toRegex(),
            "unregister\\w*Observer".toRegex()
        )

        private fun isAllowedMethod(expression: PsiReferenceExpression): Boolean {
            val methodName = expression.referenceName ?: return false
            return isAllowedIdentifier(methodName)
        }

        private fun isAllowedMethod(expression: PsiMethod): Boolean {
            val methodName = expression.name
            return isAllowedIdentifier(methodName)
        }

        private fun isAllowedMethod(expression: UCallExpression): Boolean {
            val methodIdentifier = expression.methodIdentifier ?: return false
            val elementName = methodIdentifier.name
            return isAllowedIdentifier(elementName)
        }

        private fun isAllowedIdentifier(elementName: String): Boolean {
            if (ACCEPTED_METHODS.contains(elementName)) {
                return true
            }
            for (reg in REGEX_LISTENERS) {
                if (reg.find(elementName) != null) {
                    return true
                }
            }
            return false
        }
    }
}



