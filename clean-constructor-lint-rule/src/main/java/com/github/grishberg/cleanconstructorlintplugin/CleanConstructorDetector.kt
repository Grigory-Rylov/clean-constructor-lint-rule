package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Detector.UastScanner
import com.android.tools.lint.detector.api.JavaContext
import com.github.grishberg.cleanconstructorlintplugin.graph.DependencyGraph
import com.github.grishberg.cleanconstructorlintplugin.graph.ExpensiveConstructorsRepository
import org.jetbrains.uast.*
import org.jetbrains.uast.util.isConstructorCall


/**
 * Detects heavy constructors.
 */
class CleanConstructorDetector : Detector(), UastScanner {
    private val expensiveClasses =
        ExpensiveConstructorsRepository()
    private val membersChecks = ClassMembersChecks()

    override fun getApplicableUastTypes() =
        listOf<Class<out UElement>>(UCallExpression::class.java, UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return NamingPatternHandler(context, membersChecks, expensiveClasses)
    }

    override fun afterCheckRootProject(context: Context) {
        // TODO: show which arguments need to make as Lazy.
    }

    class NamingPatternHandler(
        private val context: JavaContext,
        private val membersChecks: ClassMembersChecks,
        private val expensiveClasses: ExpensiveConstructorsRepository
    ) : UElementHandler() {

        override fun visitCallExpression(expr: UCallExpression) {
            if (!expr.isConstructorCall()) {
                return
            }
            checkConstructorsOfConstructorCall(expr)
        }

        private fun checkConstructorsOfConstructorCall(call: UCallExpression) {
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
            val constructorVisitor = ReferenceConstructorChecker(context, membersChecks)
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
            if (node.uastParent is UClass && membersChecks.isIgnoredSupertype(node.uastParent as UClass, context)) {
                return
            }
            checkConstructor(node)
        }

        private fun checkConstructor(constructorMethod: UMethod) {
            constructorMethod.accept(ConstructorsMethodsVisitor(context, membersChecks, constructorMethod.uastParent!!))
            if (hasInjectAnnotation(constructorMethod)) {
                checkArgsHasExpensiveConstructor(constructorMethod, null, shouldReport = true)
            }
        }

        private fun hasInjectAnnotation(constructor: UMethod): Boolean {
            return constructor.hasAnnotation("javax.inject.Inject") || constructor.hasAnnotation("Inject")
        }

        private fun checkArgsHasExpensiveConstructor(
            constructor: UMethod,
            parentDiGraph: DependencyGraph?,
            shouldReport: Boolean = false
        ): Boolean {
            var hasExpensiveConstructor = false
            // check each injected class in parameters.
            for (constructorsParam in constructor.uastParameters) {
                if (isExpensiveConstructorParameter(constructor, parentDiGraph, shouldReport, constructorsParam)) {
                    hasExpensiveConstructor = true
                }
            }
            return hasExpensiveConstructor
        }

        private fun isExpensiveConstructorParameter(
            constructor: UMethod,
            parentDiGraph: DependencyGraph?,
            shouldReport: Boolean,
            constructorsParam: UParameter
        ): Boolean {
            var hasExpensiveConstructor = false
            val injectedClassName: String = constructorsParam.typeReference?.getQualifiedName() ?: return false
            val diGraph: DependencyGraph = parentDiGraph ?: DependencyGraph(
                constructor.name
            )

            if (diGraph.hasElement(injectedClassName)) {
                return false
            }
            //TODO: check cache of expensive classes.
            // check parameter's constructor
            val clazz = context.evaluator.findClass(injectedClassName) ?: return false
            val uClass = context.uastContext.getClass(clazz)

            var paramConstructorHasExpensiveMethod = false
            for (parameterClassMethod in uClass.methods) {
                if (!parameterClassMethod.isConstructor) {
                    continue // hasExpensiveConstructor = false
                }
                val classVisitor = ReferenceConstructorChecker(context, membersChecks)
                parameterClassMethod.accept(classVisitor)
                if (classVisitor.hasExpensiveConstructor()) {
                    diGraph.addElement(injectedClassName)
                    expensiveClasses.add(injectedClassName, diGraph)
                    if (shouldReport) {
                        reportExpensiveInjectedParameter(constructor, constructorsParam, diGraph)
                    }
                    paramConstructorHasExpensiveMethod = true
                    hasExpensiveConstructor = true
                    break
                }
            }
            if (paramConstructorHasExpensiveMethod) {
                return true
            }

            for (method in uClass.methods) {
                if (checkInjectedClassMethod(injectedClassName, constructorsParam, method, diGraph, shouldReport)) {
                    hasExpensiveConstructor = true
                }
            }
            return hasExpensiveConstructor
        }

        private fun checkInjectedClassMethod(
            injectedClassName: String,
            constructorsParam: UParameter,
            method: UMethod,
            diGraph: DependencyGraph,
            shouldReport: Boolean
        ): Boolean {
            if (!method.isConstructor) {
                return false
            }
            if (!hasInjectAnnotation(method)) {
                return false
            }
            // check injected parameters.
            val subclassGraph =
                DependencyGraph(injectedClassName)
            if (checkArgsHasExpensiveConstructor(method, subclassGraph)) {
                diGraph.addGraph(subclassGraph)
                val constructorParamAsUElement = constructorsParam.toUElement()
                if (shouldReport && constructorParamAsUElement != null) {
                    context.report(
                        CleanConstructorsRegistry.INJECT_ISSUE, method,
                        context.getNameLocation(constructorParamAsUElement),
                        "Constructor with @Inject annotation injected object that has expensive constructor: $diGraph"
                    )
                }
                return true
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
                context.getNameLocation(param.toUElement()!!),
                "Constructor with @Inject annotation injected object that has expensive constructor: $diGraph"
            )
        }
    }
}



