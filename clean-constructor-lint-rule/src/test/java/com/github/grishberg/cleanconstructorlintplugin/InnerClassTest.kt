package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.apache.commons.io.IOUtils
import org.junit.Test

class InnerClassTest {
    @Test
    fun ignoreSimpleFieldGetter() {
        TestLintTask.lint()
            .files(
                LintDetectorTest.java(readFromResource("SampleInnerClass.java"))
            )
            .issues(CleanConstructorDetector.ISSUE)
            .run()
            .expect(
                """
src/com/github/grishberg/delegateadapter/SampleInnerClass.java:9: Warning: Constructor creates object that has expensive constructor: SampleInnerClass [ExpensiveConstructor]
        InnerClass innerClass = new InnerClass();
                                ~~~~~~~~~~~~~~~~
0 errors, 1 warnings                   
            """.trimIndent()
            )
    }

    private fun readFromResource(fn: String): String {
        val resourceAsStream = javaClass.classLoader.getResourceAsStream(fn)
        return IOUtils.toString(resourceAsStream)
    }
}