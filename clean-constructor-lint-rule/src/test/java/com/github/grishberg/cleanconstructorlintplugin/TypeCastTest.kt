package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

class TypeCastTest {
    @Test
    fun allowTypeCast() {
        TestLintTask.lint()
            .files(
                LintDetectorTest.java(
                    """
      package foo;
      public class Example {
        public Example() {
            init("");
        }
        
        private void init(String str) {
            String s = (String) str;
        }
        
      }"""
                ).indented()
            )
            .issues(CleanConstructorDetector.ISSUE, CleanConstructorDetector.INJECT_ISSUE)
            .run()
            .expectClean()
    }
}