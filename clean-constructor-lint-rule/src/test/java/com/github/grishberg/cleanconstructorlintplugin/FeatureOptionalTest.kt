package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

class FeatureOptionalTest {
    private val expensiveConstructorClass = LintDetectorTest.java(
        """
      package foo;
      public class ExpensiveConstructor {
        public ExpensiveConstructor() {
            Thread.sleep(1000);
        }
      }"""
    ).indented()
    private val featureOptional = LintDetectorTest.java(
        """
      package com.test;
      public class FeatureOptional<T> {
        private final T value;
        private FeatureOptional(T v) { 
            value = v;
        }

        public T get() {
            return value;
        }
      }"""
    ).indented()


    @Test
    fun createObjectWithExpensiveConstructor() {
        TestLintTask.lint()
            .files(expensiveConstructorClass,
                featureOptional, LintDetectorTest.java(
                    """
          package foo;
          import com.test.FeatureOptional;
          import javax.inject.Inject;
          class Example {
            @Inject
            public Example(FeatureOptional<ExpensiveConstructor> c) {
            }
          }"""
                ).indented()
            )
            .issues(CleanConstructorDetector.INJECT_ISSUE)
            .run()
            .expect(
                """
src/foo/Example.java:6: Warning: Constructor with @Inject annotation injected object that has expensive constructor: foo.ExpensiveConstructor [InjectedExpensiveConstructor]
  public Example(FeatureOptional<ExpensiveConstructor> c) {
                                                       ~
0 errors, 1 warnings
                """.trimMargin()
            )
    }
}