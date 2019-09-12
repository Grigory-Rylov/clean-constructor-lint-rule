package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

class DontWarningWhenExpensiveConstructorInMethodTest {
    private val expensiveConstructorClass = LintDetectorTest.java(
        """
      package foo;
      public class ExpensiveConstructor {
        public ExpensiveConstructor() {
            Thread.sleep(1000);
        }
      }"""
    ).indented()

    @Test
    fun createObjectWithExpensiveConstructor() {
        TestLintTask.lint()
            .files(
                expensiveConstructorClass,
                LintDetectorTest.java(
                    """
          package foo;
          import com.test.FeatureOptional;
          import javax.inject.Inject;
          class Example {
            public ExpensiveConstructor create() {
                return new ExpensiveConstructor();
            }
          }"""
                ).indented()
            )
            .issues(CleanConstructorDetector.ISSUE)
            .run()
            .expect("""
src/foo/ExpensiveConstructor.java:4: Warning: Constructor has expensive method calls: sleep [ExpensiveConstructor]
      Thread.sleep(1000);
             ~~~~~
0 errors, 1 warnings
            """.trimIndent())
    }
}