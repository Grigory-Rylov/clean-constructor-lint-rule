package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.java
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class CleanConstructorDetectorTest {
    private val expensiveConstructorClass = java(
        """
      package com.test;
      public class ExpensiveConstructor {
        public ExpensiveConstructor() {
            for(int i = 0; i < 100; i ++) {
                getDrawable(R.drawable.test);
            }
        }
        public void getDrawable(int id) {}
        public void getColor(int id) {}
        public void getColorStateList(int id) {}
      }"""
    ).indented()

    @Test
    fun constructorHasMethodCalls() {
        lint()
            .files(
                java(
                    """
          package foo;
          import android.content.res.Resources;
          class Example {
            private SomeAnotherClass anotherClass;
            public Example() {
                foo();
                anotherClass.slowMethod();
            }

            public void foo() {
              Resources resources = null;
              resources.getDrawable(0);
            }
          }"""
                ).indented()
            )
            .issues(CleanConstructorsRegistry.ISSUE)
            .run()
            .expect(
                """
                    src/foo/Example.java:3: Warning: Constructor has expensive method calls: foo [CleanConstructor]
                    class Example {
                          ~~~~~~~
                    src/foo/Example.java:3: Warning: Constructor has expensive method calls: slowMethod [CleanConstructor]
                    class Example {
                          ~~~~~~~
                    0 errors, 2 warnings
                """
                    .trimMargin()
            )
    }

    @Test
    fun constructorHasRegisterObserverMethodCalls() {
        lint()
            .files(
                java(
                    """
          package foo;
          import android.content.res.Resources;
          class Example {
            private SomeAnotherClass anotherClass;
            public Example() {
                anotherClass.addObserver(new SomeListener());
                anotherClass.setListener(this);
            }
          }"""
                ).indented()
            )
            .issues(CleanConstructorsRegistry.ISSUE)
            .run()
            .expect("No warnings.")
    }

    @Test
    fun createObjectWithExpensiveConstructor() {
        lint()
            .files(
                expensiveConstructorClass, java(
                    """
          package foo;
          import com.test.ExpensiveConstructor;
          class Example {
            private SomeAnotherClass anotherClass;
            public Example() {
                ExpensiveConstructor val = new ExpensiveConstructor();
                anotherClass.addObserver(val);
            }
          }"""
                ).indented()
            )
            .issues(CleanConstructorsRegistry.ISSUE)
            .run()
            .expect(
                """
                src/foo/Example.java:3: Warning: Constructor creates object that has expensive constructor: com.test.ExpensiveConstructor [CleanConstructor]
                class Example {
                      ~~~~~~~
                src/com/test/ExpensiveConstructor.java:2: Warning: Constructor has expensive method calls: getDrawable [CleanConstructor]
                public class ExpensiveConstructor {
                             ~~~~~~~~~~~~~~~~~~~~
                0 errors, 2 warnings
                """.trimMargin()
            )
    }

}