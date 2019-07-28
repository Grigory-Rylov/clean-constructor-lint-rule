package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.*


class CleanConstructorsRegistry : IssueRegistry() {
    override val issues: List<Issue>
        get() = listOf(ISSUE, INJECT_ISSUE)

    override val api: Int
        get() = CURRENT_API
    override val minApi: Int
        get() = 2

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