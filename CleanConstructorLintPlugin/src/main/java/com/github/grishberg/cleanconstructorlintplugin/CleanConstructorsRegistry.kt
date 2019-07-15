package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.*


class CleanConstructorsRegistry : IssueRegistry() {
    override val issues: List<Issue>
        get() = listOf(ISSUE)

    companion object {
        /** Issue describing the problem and pointing to the detector implementation  */
        val ISSUE = Issue.create(
            // ID: used in @SuppressLint warnings etc
            "CleanConstructor",

            // Title -- shown in the IDE's preference dialog, as category headers in the
            // Analysis results window, etc
            "Lint clean constructors",

            // Full explanation of the issue; you can use some markdown markup such as
            // `monospace`, *italic*, and **bold**.
            "This check highlights string literals in code which mentions " +
                    "the word `lint`. Blah blah blah.\n" +
                    "\n" +
                    "Another paragraph here.\n",
            Category.PERFORMANCE,
            6,
            Severity.WARNING,
            Implementation(
                CleanConstructorDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}