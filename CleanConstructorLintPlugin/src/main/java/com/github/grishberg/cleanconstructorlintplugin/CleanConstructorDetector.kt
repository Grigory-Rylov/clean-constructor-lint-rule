package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Detector.UastScanner
import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiReferenceExpression
import org.jetbrains.uast.*
import org.jetbrains.uast.util.isConstructorCall
import org.jetbrains.uast.util.isMethodCall
import org.jetbrains.uast.visitor.AbstractUastVisitor


/**
 * Detects heavy constructors.
 */
class CleanConstructorDetector : Detector(), UastScanner {
    private val expensiveClasses = mutableSetOf<String>()
    override fun getApplicableUastTypes() =
        listOf<Class<out UElement>>(UCallExpression::class.java, UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return NamingPatternHandler(context, expensiveClasses)
    }

    class NamingPatternHandler(
        private val context: JavaContext,
        private val expensiveClasses: MutableSet<String>
    ) : UElementHandler() {

        override fun visitCallExpression(call: UCallExpression) {
            if (!call.isConstructorCall()) {
                return
            }
            val callerClass = call.getContainingUClass()

            val classReference = call.classReference
            if (classReference != null && callerClass != null) {
                val resolved = classReference.resolveToUElement()
                if ((resolved is UClass)) {
                    for (method in resolved.methods) {
                        if (!method.isConstructor) {
                            continue
                        }
                        checkReferenceConstructors(callerClass, method, call)
                    }
                }
            }
        }

        private fun checkReferenceConstructors(callerClass: UClass, constructor: UMethod, call: UCallExpression) {
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
            if (node.uastParent is UClass && isIgnoredSupertype(node.uastParent as UClass)) {
                return
            }
            checkConstructor(node)
        }

        private fun checkConstructor(constructorMethod: UMethod) {
            constructorMethod.accept(ConstructorsMethodsVisitor(context, constructorMethod.uastParent!!))
            if (hasInjectAnnotation(constructorMethod.annotations)) {
                val diGraph = DependencyGraph(constructorMethod.name)
                checkArgsHasExpensiveConstructor(constructorMethod, diGraph, shouldReport = true)
            }
        }

        private fun hasInjectAnnotation(annotations: List<UAnnotation>): Boolean {
            for (a in annotations) {
                val nameReferenceElement = a.qualifiedName
                if (nameReferenceElement != null && (nameReferenceElement == "javax.inject.Inject" || nameReferenceElement == "Inject")) {
                    return true
                }
            }
            return false
        }

        private fun checkArgsHasExpensiveConstructor(
            constructor: UMethod,
            diGraph: DependencyGraph,
            shouldReport: Boolean = false
        ): Boolean {
            for (param in constructor.uastParameters) {
                val injectedClassName: String = param.typeReference?.getQualifiedName() ?: continue
                //if (expensiveClasses.contains(injectedClassName)) {
                //    if (shouldReport) {
                //        reportExpensiveInjectedParameter(constructor, param, diGraph)
                //    }
                //    return true
                //}
                val clazz = context.evaluator.findClass(injectedClassName) ?: continue
                val uClass = context.uastContext.getClass(clazz)
                val constructorVisitor = ReferenceConstructorChecker()
                uClass.accept(constructorVisitor)
                if (constructorVisitor.hasExpensiveConstructor()) {
                    diGraph.addElement(injectedClassName)
                    expensiveClasses.add(injectedClassName)
                    if (shouldReport) {
                        reportExpensiveInjectedParameter(constructor, param, diGraph)
                    }
                    return true
                }

                for (c in uClass.methods) {
                    if (!c.isConstructor) {
                        continue
                    }
                    if (hasInjectAnnotation(c.annotations)) {
                        val subclassGraph = DependencyGraph(injectedClassName)
                        if (checkArgsHasExpensiveConstructor(c, subclassGraph)) {
                            diGraph.addGraph(subclassGraph)
                            if (shouldReport) {
                                context.report(
                                    CleanConstructorsRegistry.INJECT_ISSUE, constructor,
                                    context.getNameLocation(param),
                                    "Constructor with @Inject annotation injected object that has expensive constructor: $diGraph"
                                )
                            }
                            return true
                        }
                    }
                }
            }
            return false
        }

        private fun reportExpensiveInjectedParameter(
            constructor: UMethod,
            param: UParameter,
            diGraph: DependencyGraph
        ) {
            context.report(
                CleanConstructorsRegistry.INJECT_ISSUE, constructor,
                context.getNameLocation(param),
                "Constructor with @Inject annotation injected object that has expensive constructor: $diGraph"
            )
        }

        private fun isIgnoredSupertype(node: UClass): Boolean {
            for (superType in node.uastSuperTypes) {
                if (IGNORED_PARENTS.contains(superType.getQualifiedName())) {
                    return true
                }
            }
            return false
        }
    }

    /**
     * Visit method calls inside current method.
     */
    private class ReferenceConstructorChecker : AbstractUastVisitor() {
        private var hasExpensiveConstructor = false


        override fun visitCallExpression(node: UCallExpression): Boolean {
            if (isCallInAnonymousClass(node)) {
                return false
            }
            if (node.isMethodCall()) {
                val methodName = node.methodName
                if (methodName != null && !isAllowedIdentifier(methodName)) {
                    hasExpensiveConstructor = true
                    return false
                }
                return false
            }
            return true
        }

        fun hasExpensiveConstructor() = hasExpensiveConstructor
    }

    class ConstructorsMethodsVisitor(
        private val context: JavaContext,
        private val parent: UElement
    ) : AbstractUastVisitor() {

        override fun visitCallExpression(node: UCallExpression): Boolean {
            if (isCallInAnonymousClass(node)) {
                return false
            }
            if (ExcludedClasses.isExcludedClassInExpression(node)) {
                return false
            }

            if (node.isMethodCall()) {
                val methodName = node.methodName
                if (methodName != null && !isAllowedIdentifier(methodName)) {
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
                    if (!method.isConstructor && !isAllowedMethod(method)) {
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

        private fun isAllowedMethod(expression: UReferenceExpression): Boolean {
            val methodName = expression.resolvedName ?: return false
            return isAllowedIdentifier(methodName)
        }

        private fun isAllowedMethod(expression: PsiReferenceExpression): Boolean {
            val methodName = expression.referenceName ?: return false
            return isAllowedIdentifier(methodName)
        }

        private fun isAllowedMethod(expression: UMethod): Boolean {
            val methodName = expression.name
            return isAllowedIdentifier(methodName)
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

        private fun isCallInAnonymousClass(node: UCallExpression): Boolean {
            var parent: UElement? = node.uastParent
            while (parent != null) {
                if (parent is UClass && parent.name == null) {
                    return true
                }
                parent = parent.uastParent
            }
            return false
        }
    }
}



