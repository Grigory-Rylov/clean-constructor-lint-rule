package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.github.grishberg.cleanconstructorlintplugin.report.WrongScopeIssueReport
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod

class WrongScopeConstructorDetector : Detector(), Detector.UastScanner {
    override fun getApplicableUastTypes() =
        listOf<Class<out UElement>>(UCallExpression::class.java, UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return NamingPatternHandler(
            context,
            ClassMembersChecks(context),
            WrongScopeIssueReport(context)
        )
    }

    companion object {
        /**
         *
         */
        val WRONG_SCOPE_ISSUE = Issue.create(
            // ID: used in @SuppressLint warnings etc
            "InjectedWrongScopeClass",

            // Title -- shown in the IDE's preference dialog, as category headers in the
            // Analysis results window, etc
            "Injected class with wrong scope",

            // Full explanation of the issue; you can use some markdown markup such as
            // `monospace`, *italic*, and **bold**.
            "This check highlights injecting class that must be created in scope, that happens " +
                    "later than current class scope. " +
                    "You should use Lazy wrapper for this cases.",
            Category.PERFORMANCE,
            9,
            Severity.ERROR,
            Implementation(
                WrongScopeConstructorDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}

