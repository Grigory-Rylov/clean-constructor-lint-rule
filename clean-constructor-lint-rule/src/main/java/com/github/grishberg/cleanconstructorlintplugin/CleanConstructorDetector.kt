package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.android.tools.lint.detector.api.Detector.UastScanner
import com.github.grishberg.cleanconstructorlintplugin.report.ExpensiveConstructorReport
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod


/**
 * Detects heavy constructors.
 */
class CleanConstructorDetector : Detector(), UastScanner {
    override fun getApplicableUastTypes() =
        listOf<Class<out UElement>>(UCallExpression::class.java, UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        val membersChecks = ClassMembersChecks(context)
        return NamingPatternHandler(
            context, membersChecks,
            ExpensiveConstructorReport(context, membersChecks)
        )
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



