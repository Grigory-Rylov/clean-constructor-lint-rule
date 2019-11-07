package com.github.grishberg.cleanconstructorlintplugin.report

import com.github.grishberg.cleanconstructorlintplugin.graph.DependencyNode
import org.jetbrains.uast.*

interface IssueReportStrategy {
    /**
     * return {@code true} when current injected parameter is no valide.
     */
    fun isWrongParameter(
        diGraph: DependencyNode,
        parameterNode: DependencyNode,
        parameterClassMethod: UMethod
    ): Boolean

    /**
     * Report issue.
     */
    fun report(
        constructor: UMethod,
        param: UParameter,
        diGraph: DependencyNode
    )

    fun reportInjectedIssue(constructor: UMethod, parentGraph: DependencyNode, uElement: UElement)
    fun reportExpensiveConstructor(call: UCallExpression, callerClass: UClass)
}