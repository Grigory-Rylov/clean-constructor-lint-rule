package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.java
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.apache.commons.io.IOUtils
import org.junit.Test

class ConstructorsTest {
    var classLoader = javaClass.classLoader

    @Test
    fun injectedClassHasExpensiveExpression() {
        TestLintTask.lint()
            .files(
                java(readFromResource("GreenAdapterDelegate.java")),
                java(readFromResource("ItemViewHolder.java")),
                java(readFromResource("RecycableViewHolder.java"))
            )
            .issues(CleanConstructorsRegistry.ISSUE, CleanConstructorsRegistry.INJECT_ISSUE)
            .run()
            .expect("No warnings.")
    }

    @Test
    fun noWarningForAvailableConstructors() {
        TestLintTask.lint()
            .files(
                java(readFromResource("MainActivity.java")),
                java(readFromResource("ItemsAdapter.java")),
                java(readFromResource("CompositeDelegateAdapter.java")),
                java(readFromResource("Delegates.java")),
                java(readFromResource("ItemsTracker.java")),
                java(readFromResource("ItemWithId.java")),
                java(readFromResource("ViewTracker.java")),
                java(readFromResource("AdapterDelegate.java"))
            )
            .issues(CleanConstructorsRegistry.ISSUE, CleanConstructorsRegistry.INJECT_ISSUE)
            .run()
            .expect("No warnings.")
    }

    private fun readFromResource(fn: String): String {
        val resourceAsStream = classLoader.getResourceAsStream(fn)
        return IOUtils.toString(resourceAsStream)
    }
}
