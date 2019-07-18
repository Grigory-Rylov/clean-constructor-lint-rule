package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.java
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
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

    private val simpleParentClass = java(
        """
      package foo;
      public class SimpleParent {
        public SimpleParent() {
            // empty
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
src/foo/Example.java:6: Warning: Constructor has expensive method calls: foo [CleanConstructor]
      foo();
      ~~~
src/foo/Example.java:7: Warning: Constructor has expensive method calls: slowMethod [CleanConstructor]
      anotherClass.slowMethod();
                   ~~~~~~~~~~
0 errors, 2 warnings
                """
                    .trimMargin()
            )
    }

    @Test
    fun constructorHasRegisterObserverMethodCalls() {
        lint()
            .files(
                simpleParentClass,
                java(
                    """
          package foo;
          class Example extends SimpleParent{
            private SomeAnotherClass anotherClass;
            public Example() {
                super();
            }
            public Example() {
                this();
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
src/foo/Example.java:6: Warning: Constructor creates object that has expensive constructor: Example [CleanConstructor]
      ExpensiveConstructor val = new ExpensiveConstructor();
                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/test/ExpensiveConstructor.java:5: Warning: Constructor has expensive method calls: getDrawable [CleanConstructor]
          getDrawable(R.drawable.test);
          ~~~~~~~~~~~
0 errors, 2 warnings
                """.trimMargin()
            )
    }

    @Test
    fun ignoreWhenParentIsDrawable() {
        lint()
            .files(
                java(
                    """
          package foo;
          import android.graphics.drawable.Drawable;
          import android.content.res.Resources;

          class Example extends Drawable {
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
            .expect("No warnings.")
    }


    @Test
    fun kotlinConstructorHasMethodCalls() {
        lint()
            .files(
                kotlin(
                    """
          package foo
          import android.content.res.Resources
          class Example(private val resources: Resources) {
            init {
                foo()
            }
            fun foo() {
              resources.getDrawable(0)
            }
          }"""
                ).indented()
            )
            .issues(CleanConstructorsRegistry.ISSUE)
            .run()
            .expect(
                """
src/foo/Example.kt:5: Warning: Constructor has expensive method calls: foo [CleanConstructor]
      foo()
      ~~~
0 errors, 1 warnings
                """
                    .trimMargin()
            )
    }

    @Test
    fun kotlinConstructorHasExpensiveConstructorCall() {
        lint()
            .files(
                expensiveConstructorClass,
                kotlin(
                    """
          package com.test
          class Example {
            private val field : ExpensiveConstructor
            init {
                field = ExpensiveConstructor()
            }
          }"""
                ).indented()
            )
            .issues(CleanConstructorsRegistry.ISSUE)
            .run()
            .expect(
                """
src/com/test/Example.kt:5: Warning: Constructor creates object that has expensive constructor: Example [CleanConstructor]
      field = ExpensiveConstructor()
              ~~~~~~~~~~~~~~~~~~~~
src/com/test/ExpensiveConstructor.java:5: Warning: Constructor has expensive method calls: getDrawable [CleanConstructor]
          getDrawable(R.drawable.test);
          ~~~~~~~~~~~
0 errors, 2 warnings
                """
                    .trimMargin()
            )
    }


    @Test
    fun warningWhenInjectedExpensiveConstructor() {
        lint()
            .files(
                expensiveConstructorClass,
                java(
                    """
          package foo;
          import com.test.ExpensiveConstructor;
          import javax.inject.Inject;
          class Example extends SimpleParent{
            private ExpensiveConstructor anotherClass;
            @Inject
            public Example(ExpensiveConstructor c) {
                anotherClass = c;
            }
          }"""
                ).indented()
            )
            .issues(CleanConstructorsRegistry.ISSUE)
            .run()
            .expect(
                """
src/foo/Example.java:7: Warning: Constructor with @Inject annotation injected object that has expensive constructor: com.test.ExpensiveConstructor [CleanConstructor]
  public Example(ExpensiveConstructor c) {
                                      ~
src/com/test/ExpensiveConstructor.java:5: Warning: Constructor has expensive method calls: getDrawable [CleanConstructor]
          getDrawable(R.drawable.test);
          ~~~~~~~~~~~
0 errors, 2 warnings
                """
                    .trimMargin()
            )
    }
}