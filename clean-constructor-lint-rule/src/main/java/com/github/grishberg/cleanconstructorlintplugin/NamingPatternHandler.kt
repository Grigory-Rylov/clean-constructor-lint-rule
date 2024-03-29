package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.JavaContext
import com.github.grishberg.cleanconstructorlintplugin.graph.DependencyNode
import com.github.grishberg.cleanconstructorlintplugin.report.IssueReportStrategy
import com.github.grishberg.cleanconstructorlintplugin.scopes.Scopes
import org.jetbrains.uast.*

class NamingPatternHandler(
    private val context: JavaContext,
    private val membersChecks: ClassMembersChecks,
    private val issueStrategy: IssueReportStrategy
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
            issueStrategy.reportExpensiveConstructor(call, callerClass)
        }
    }

    /**
     * Use this method only for constructors checking.
     */
    override fun visitMethod(node: UMethod) {
        if (!isConstructor(node)) {
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

    private fun isConstructor(node: UMethod): Boolean {
        if (node.isConstructor) {
            return true
        }
        return node.name == membersChecks.extractUClassFromMethod(node)?.name
    }

    private fun checkConstructor(constructorMethod: UMethod) {
        constructorMethod.accept(
            ConstructorsMethodsVisitor(
                context, membersChecks,
                // TODO: replace with issueStrategy
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
        var hasExpensiveConstructor = false
        // check each injected class in parameters.
        for (constructorsParam in constructor.uastParameters) {
            if (isExpensiveConstructorParameter(
                    constructor,
                    parentDiGraph,
                    shouldReport,
                    constructorsParam
                )
            ) {
                hasExpensiveConstructor = true
            }
        }
        return hasExpensiveConstructor
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

        val parameterAnnotations = uClass.annotations
        val diGraph = parentDiGraph ?: DependencyNode(
            scopes,
            membersChecks.extractRawTypeFromConstructor(constructor),
            membersChecks.extractAnnotationsFromMethod(constructor)
        )
        val parameterNode = DependencyNode(scopes, injectedClassName, parameterAnnotations)

        if (diGraph.hasElement(parameterNode)) {
            return false
        }
        var paramConstructorHasExpensiveMethod = false
        for (parameterClassMethod in uClass.methods) {
            if (!parameterClassMethod.isConstructor) {
                continue // hasExpensiveConstructor = false
            }


            if (issueStrategy.isWrongParameter(diGraph, parameterNode, parameterClassMethod)) {
                diGraph.addChild(parameterNode)

                if (shouldReport) {
                    issueStrategy.report(constructor, constructorsParam, diGraph)
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
                    parameterAnnotations,
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
                issueStrategy.reportInjectedIssue(
                    constructor,
                    parentGraph,
                    constructorParamAsUElement
                )
            }
            return true
        }
        return false
    }
}