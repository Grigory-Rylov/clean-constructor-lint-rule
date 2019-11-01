package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.android.tools.lint.detector.api.Detector.UastScanner
import com.github.grishberg.cleanconstructorlintplugin.graph.DependencyNode
import com.github.grishberg.cleanconstructorlintplugin.graph.InjectedIssueString
import com.github.grishberg.cleanconstructorlintplugin.scopes.Scopes
import org.jetbrains.uast.*


/**
 * Detects heavy constructors.
 */
class CleanConstructorDetector : Detector(), UastScanner {
    override fun getApplicableUastTypes() =
        listOf<Class<out UElement>>(UCallExpression::class.java, UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return NamingPatternHandler(context, ClassMembersChecks(context))
    }

    class NamingPatternHandler(
        private val context: JavaContext,
        private val membersChecks: ClassMembersChecks
    ) : UElementHandler() {
        private val scopes = Scopes()

        /**
         * TODO: Use this method only for field checking.
         */
        override fun visitCallExpression(expr: UCallExpression) {
            if (!isCallInsideConstructorOrFieldDeclaration(expr.uastParent)) {
                return
            }
            checkConstructorsOfConstructorCall(expr)
        }

        private fun isCallInsideConstructorOrFieldDeclaration(parent: UElement?): Boolean {
            if (parent == null) {
                return false
            }
            if (parent is UMethod && parent.isConstructor) {
                return true
            }
            if (parent is UField) {
                return true
            }
            return isCallInsideConstructorOrFieldDeclaration(parent.uastParent)
        }

        private fun checkConstructorsOfConstructorCall(call: UCallExpression) {
            val callerClass = call.getContainingUClass()

            val classReference = call.classReference
            if (classReference != null && callerClass != null) {
                val resolved = classReference.resolveToUElement()
                if ((resolved is UClass)) {
                    resolved.methods.asSequence()
                        .filter { it.isConstructor }
                        .forEach { checkReferenceConstructors(callerClass, it, call) }
                }
            }
        }

        private fun checkReferenceConstructors(
            callerClass: UClass,
            constructor: UMethod,
            call: UCallExpression
        ) {
            val constructorVisitor = ConstructorsMethodsVisitor(context, membersChecks)
            constructor.accept(constructorVisitor)
            if (constructorVisitor.isExpensiveConstructor()) {
                context.report(
                    ISSUE, call,
                    context.getNameLocation(call),
                    "Constructor creates object that has expensive constructor: ${callerClass.name}"
                )
            }
        }

        /**
         * Use this method only for constructors checking.
         */
        override fun visitMethod(node: UMethod) {
            if (!node.isConstructor) {
                return
            }
            if (node.uastParent is UClass && membersChecks.isIgnoredSupertype(
                    node.uastParent as UClass,
                    context
                )
            ) {
                return
            }
            checkConstructor(node)
        }

        private fun checkConstructor(constructorMethod: UMethod) {
            constructorMethod.accept(
                ConstructorsMethodsVisitor(
                    context, membersChecks,
                    ContextReportingStrategy(context, constructorMethod.uastParent!!)
                )
            )
            if (hasInjectAnnotation(constructorMethod)) {
                checkArgsHasExpensiveConstructor(constructorMethod, null, shouldReport = true)
            }
        }

        private fun hasInjectAnnotation(constructor: UMethod): Boolean {
            return constructor.hasAnnotation("javax.inject.Inject") || constructor.hasAnnotation("Inject")
        }

        private fun checkArgsHasExpensiveConstructor(
            constructor: UMethod,
            parentDiGraph: DependencyNode?,
            shouldReport: Boolean = false
        ): Boolean {
            // check each injected class in parameters.
            return constructor.uastParameters.any { uParameter ->
                isExpensiveConstructorParameter(
                    constructor,
                    parentDiGraph,
                    shouldReport,
                    uParameter
                )
            }
        }

        private fun isExpensiveConstructorParameter(
            constructor: UMethod,
            parentDiGraph: DependencyNode?,
            shouldReport: Boolean,
            constructorsParam: UParameter
        ): Boolean {
            var hasExpensiveConstructor = false
            // 1) check if injected constructors in current {@param constructor} is expensive
            val parameterWrapper = ParameterWrapper(membersChecks, constructorsParam)
            val uClass = parameterWrapper.uClass ?: return false
            val injectedClassName: String = uClass.qualifiedName ?: return false

            val annotations = uClass.annotations
            val diGraph = parentDiGraph ?: DependencyNode(scopes,
                membersChecks.extractRawTypeFromConstructor(constructor),
                annotations
            )
            val parameterNode = DependencyNode(scopes, injectedClassName, annotations)

            if (diGraph.hasElement(parameterNode)) {
                return false
            }
            var paramConstructorHasExpensiveMethod = false
            for (parameterClassMethod in uClass.methods) {
                if (!parameterClassMethod.isConstructor) {
                    continue // hasExpensiveConstructor = false
                }
                val classVisitor = ConstructorsMethodsVisitor(context, membersChecks)
                parameterClassMethod.accept(classVisitor)
                if (classVisitor.isExpensiveConstructor() && diGraph.isWrongScope(parameterNode)) {
                    diGraph.addChild(parameterNode)

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

            // 2) check injected dependencies of current {@param constructor}
            for (method in uClass.methods) {
                if (checkInjectedClassMethod(
                        injectedClassName,
                        annotations,
                        constructorsParam,
                        method,
                        diGraph,
                        shouldReport
                    )
                ) {
                    return true
                }
            }
            return hasExpensiveConstructor
        }

        private fun checkInjectedClassMethod(
            injectedClassName: String,
            annotations: List<UAnnotation>,
            constructorsParam: UParameter,
            constructor: UMethod,
            parentGraph: DependencyNode,
            shouldReport: Boolean
        ): Boolean {
            if (!constructor.isConstructor) {
                return false
            }
            if (!hasInjectAnnotation(constructor)) {
                return false
            }
            // check injected parameters.
            val subclassGraph = DependencyNode(scopes, injectedClassName, annotations, parentGraph)
            if (checkArgsHasExpensiveConstructor(constructor, subclassGraph)) {
                parentGraph.addChild(subclassGraph)
                val constructorParamAsUElement = constructorsParam.toUElement()
                if (shouldReport && constructorParamAsUElement != null) {
                    val sb = InjectedIssueString()
                    parentGraph.printPath(sb)
                    context.report(
                        INJECT_ISSUE, constructor,
                        context.getNameLocation(constructorParamAsUElement),
                        "Constructor with @Inject annotation injected object that has expensive constructor: $sb"
                    )
                }
                return true
            }
            return false
        }

        private fun reportExpensiveInjectedParameter(
            constructor: UMethod,
            param: UParameter,
            diGraph: DependencyNode
        ) {
            val sb = InjectedIssueString()
            diGraph.printPath(sb)
            context.report(
                INJECT_ISSUE, constructor,
                context.getNameLocation(param.toUElement()!!),
                "Constructor with @Inject annotation injected object that has expensive constructor: $sb"
            )
        }
    }

    companion object {
        /** Issue describing the problem and pointing to the detector implementation  */
        val ISSUE = Issue.create(
            // ID: used in @SuppressLint warnings etc
            "ExpensiveConstructor",

            // Title -- shown in the IDE's preference dialog, as category headers in the
            // Analysis results window, etc
            "Expensive constructors",

            // Full explanation of the issue; you can use some markdown markup such as
            // `monospace`, *italic*, and **bold**.
            "This check highlights expensive constructor. " +
                    "Constructors must only initiate fields\n",
            Category.PERFORMANCE,
            8,
            Severity.WARNING,
            Implementation(
                CleanConstructorDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        val INJECT_ISSUE = Issue.create(
            // ID: used in @SuppressLint warnings etc
            "InjectedExpensiveConstructor",

            // Title -- shown in the IDE's preference dialog, as category headers in the
            // Analysis results window, etc
            "Injected expensive constructors",

            // Full explanation of the issue; you can use some markdown markup such as
            // `monospace`, *italic*, and **bold**.
            "This check highlights injecting expensive constructor. " +
                    "Need to use Lazy wrapper for this cases.",
            Category.PERFORMANCE,
            9,
            Severity.WARNING,
            Implementation(
                CleanConstructorDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}



