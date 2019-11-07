package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue


class CleanConstructorsRegistry : IssueRegistry() {
    override val issues: List<Issue>
        get() = listOf(
            WrongScopeConstructorDetector.WRONG_SCOPE_ISSUE,
            CleanConstructorDetector.ISSUE,
            CleanConstructorDetector.INJECT_ISSUE
        )

    override val api: Int
        get() = CURRENT_API
    override val minApi: Int
        get() = 2
}