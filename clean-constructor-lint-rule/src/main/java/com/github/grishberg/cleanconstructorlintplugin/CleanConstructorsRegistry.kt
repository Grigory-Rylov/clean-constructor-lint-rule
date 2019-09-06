package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.*
import com.github.grishberg.cleanconstructorlintplugin.CleanConstructorDetector.Companion.ISSUE
import com.github.grishberg.cleanconstructorlintplugin.CleanConstructorDetector.Companion.INJECT_ISSUE


class CleanConstructorsRegistry : IssueRegistry() {
    override val issues: List<Issue>
        get() = listOf(ISSUE, INJECT_ISSUE)

    override val api: Int
        get() = CURRENT_API
    override val minApi: Int
        get() = 2
}