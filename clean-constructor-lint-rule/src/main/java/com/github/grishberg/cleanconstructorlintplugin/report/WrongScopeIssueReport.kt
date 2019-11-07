package com.github.grishberg.cleanconstructorlintplugin.report

import com.android.tools.lint.detector.api.JavaContext
import com.github.grishberg.cleanconstructorlintplugin.WrongScopeConstructorDetector
import com.github.grishberg.cleanconstructorlintplugin.graph.DependencyNode
import com.github.grishberg.cleanconstructorlintplugin.graph.InjectedIssueString
import org.jetbrains.uast.*

class WrongScopeIssueReport(
    private val context: JavaContext
) : IssueReportStrategy {

    override fun isWrongParameter(
        diGraph: DependencyNode,
        parameterNode: DependencyNode,
        parameterClassMethod: UMethod
    ): Boolean {
        return diGraph.isWrongScope(parameterNode)
    }

    override fun report(constructor: UMethod, param: UParameter, diGraph: DependencyNode) {
        val sb = InjectedIssueString()
        diGraph.printPath(sb)
        context.report(
            WrongScopeConstructorDetector.WRONG_SCOPE_ISSUE, constructor,
            context.getNameLocation(param.toUElement()!!),
            "Constructor with @Inject annotation injected object that has different scope: $sb"
        )
    }

    override fun reportInjectedIssue(
        constructor: UMethod,
        parentGraph: DependencyNode,
        uElement: UElement
    ) {
        /* do nothing */
    }

    override fun reportExpensiveConstructor(call: UCallExpression, callerClass: UClass) {
        /* do nothing */
    }
}