package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.apache.commons.io.IOUtils
import org.junit.Test

class GetterTest {
    @Test
    fun ignoreSimpleFieldGetter() {
        TestLintTask.lint()
            .files(
                LintDetectorTest.java(readFromResource("GetterUserClass.java")),
                LintDetectorTest.java(readFromResource("SimpleProvider.java"))
            )
            .issues(CleanConstructorDetector.ISSUE)
            .run()
            .expect("No warnings.")
    }

    private fun readFromResource(fn: String): String {
        val resourceAsStream = javaClass.classLoader.getResourceAsStream(fn)
        return IOUtils.toString(resourceAsStream)
    }
}