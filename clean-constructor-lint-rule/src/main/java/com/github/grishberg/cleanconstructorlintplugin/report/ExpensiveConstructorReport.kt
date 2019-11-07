package com.github.grishberg.cleanconstructorlintplugin.report

import com.android.tools.lint.detector.api.JavaContext
import com.github.grishberg.cleanconstructorlintplugin.ClassMembersChecks
import com.github.grishberg.cleanconstructorlintplugin.CleanConstructorDetector
import com.github.grishberg.cleanconstructorlintplugin.ConstructorsMethodsVisitor
import com.github.grishberg.cleanconstructorlintplugin.graph.DependencyNode
import com.github.grishberg.cleanconstructorlintplugin.graph.InjectedIssueString
import org.jetbrains.uast.*

class ExpensiveConstructorReport(
    private val context: JavaContext,
    private val membersChecks: ClassMembersChecks
) : IssueReportStrategy {

    override fun isWrongParameter(
        diGraph: DependencyNode,
        parameterNode: DependencyNode,
        parameterClassMethod: UMethod
    ): Boolean {
        val classVisitor = ConstructorsMethodsVisitor(context, membersChecks)
        parameterClassMethod.accept(classVisitor)
        return classVisitor.isExpensiveConstructor()
    }

    override fun report(
        constructor: UMethod,
        param: UParameter,
        diGraph: DependencyNode
    ) {
        val sb = InjectedIssueString()
        diGraph.printPath(sb)
        context.report(
            CleanConstructorDetector.INJECT_ISSUE, constructor,
            context.getNameLocation(param.toUElement()!!),
            "Constructor with @Inject annotation injected object that has expensive constructor: $sb"
        )
    }

    override fun reportInjectedIssue(
        constructor: UMethod,
        parentGraph: DependencyNode,
        uElement: UElement
    ) {
        val sb = InjectedIssueString()
        parentGraph.printPath(sb)

        context.report(
            CleanConstructorDetector.INJECT_ISSUE, constructor,
            context.getNameLocation(uElement),
            "Constructor with @Inject annotation injected object that has expensive constructor: $sb"
        )
    }

    override fun reportExpensiveConstructor(call: UCallExpression, callerClass: UClass) {
        context.report(
            CleanConstructorDetector.ISSUE, call,
            context.getNameLocation(call),
            "Constructor creates object that has expensive constructor: ${callerClass.name}"
        )
    }
}