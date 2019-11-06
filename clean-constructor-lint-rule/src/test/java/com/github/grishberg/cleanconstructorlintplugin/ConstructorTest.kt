package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

class ConstructorTest {
    private val cardsController = LintDetectorTest.kotlin(
        """
        package foo
        import javax.inject.Inject

        class CardsController @Inject constructor() {
            fun testMethod() {
                Thread.sleep(1000)
            }    
        }"""
    ).indented()


    @Test
    fun allowIgnoredStaticMethods() {
        TestLintTask.lint()
            .files(
                cardsController
            )
            .issues(CleanConstructorDetector.INJECT_ISSUE)
            .run()
            .expectClean()
    }
}