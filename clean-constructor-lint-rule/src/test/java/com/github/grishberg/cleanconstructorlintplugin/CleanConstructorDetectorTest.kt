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
        public void getDrawable(int id) {
            try { Thread.sleep(1000);} catch (Exception e) {}
        }
        public void getColor(int id) {try { Thread.sleep(1000);} catch (Exception e) {}}
        public void getColorStateList(int id) {try { Thread.sleep(1000);} catch (Exception e) {}}
      }"""
    ).indented()

    private val simpleParentClass = java(
        """
      package foo;
      public class SimpleParent {
        public SimpleParent() {
            // empty
        }
        public int getDrawableRes(int id) { return 1;}
        public int getColor(int id) { return 0xFFFFFF; }
      }"""
    ).indented()

    @Test
    fun notWarningWhenCallSuperInConstructor() {
        lint()
            .files(
                simpleParentClass,
                java(
                    """
          package foo;
          class Example extends SimpleParent{
            public Example() {
                super();
            }
            public Example(Runnable r) {
                this();
            }
          }"""
                ).indented()
            )
            .issues(CleanConstructorDetector.ISSUE)
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
            public Example() {
                ExpensiveConstructor val = new ExpensiveConstructor();
            }
          }"""
                ).indented()
            )
            .issues(CleanConstructorDetector.ISSUE)
            .run()
            .expect(
                """
src/foo/Example.java:5: Warning: Constructor creates object that has expensive constructor: Example [ExpensiveConstructor]
      ExpensiveConstructor val = new ExpensiveConstructor();
                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/test/ExpensiveConstructor.java:5: Warning: Constructor has expensive method calls: getDrawable [ExpensiveConstructor]
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
            .issues(CleanConstructorDetector.ISSUE)
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
              Thread.sleep(100)
            }
          }"""
                ).indented()
            )
            .issues(CleanConstructorDetector.ISSUE)
            .run()
            .expect(
                """
src/foo/Example.kt:5: Warning: Constructor has expensive method calls: foo [ExpensiveConstructor]
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
            .issues(CleanConstructorDetector.ISSUE)
            .run()
            .expect(
                """
src/com/test/Example.kt:5: Warning: Constructor creates object that has expensive constructor: Example [ExpensiveConstructor]
      field = ExpensiveConstructor()
              ~~~~~~~~~~~~~~~~~~~~
src/com/test/ExpensiveConstructor.java:5: Warning: Constructor has expensive method calls: getDrawable [ExpensiveConstructor]
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
            .issues(CleanConstructorDetector.ISSUE)
            .run()
            .expect(
                """
src/com/test/ExpensiveConstructor.java:5: Warning: Constructor has expensive method calls: getDrawable [ExpensiveConstructor]
          getDrawable(R.drawable.test);
          ~~~~~~~~~~~
0 errors, 1 warnings
                """
                    .trimMargin()
            )
    }


    @Test
    fun constructorAnonymousClass() {
        lint()
            .files(
                java(
                    """
          package foo;
          import foo.some.SomeListener;
          class Example {
            private View.OnClickListener listener;
            public Example() {
                listener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        foo();
                    }
                };
            }
            private void foo() {
                Thread.sleep(1000);
            }
          }"""
                ).indented()
            )
            .issues(CleanConstructorDetector.ISSUE)
            .run()
            .expect("No warnings.")
    }


    @Test
    fun allowAddToListAndPutToMapInsideConstructor() {
        lint()
            .files(
                java(
                    """
            package com.test;
            import java.util.ArrayList;
            import java.util.HashMap;
            import java.util.List;
            import java.util.Map;
            class Example {
                public Example(List list, Map map) {
                    map.put("1", 0);
                    list.add("1");
                }
            }
            """
                ).indented()
            )
            .issues(CleanConstructorDetector.ISSUE)
            .run()
            .expect("No warnings.")
    }
}