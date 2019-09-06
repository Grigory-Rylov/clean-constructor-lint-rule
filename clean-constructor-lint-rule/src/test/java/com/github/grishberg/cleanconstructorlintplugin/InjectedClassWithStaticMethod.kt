package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.java
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

class InjectedClassWithStaticMethod {
    private val cleanConstructorClass = java(
        """
      package com.test;
      public class CleanConstructorWithStaticMethod {
        public CleanConstructorWithStaticMethod() {

        }

        public void addListener(Runnable runnable) {
            // do something
        }
        public static void foo() {
            Thread.sleep(1000L);
        }
      }"""
    ).indented()

    private val expensiveConstructorClass = java(
        """
      package com.test;
      public class ExpensiveConstructor {
        public ExpensiveConstructor() {
           foo();
        }

        public void foo() {
            Thread.sleep(1000L);
        }
      }"""
    ).indented()

    private val testedClass1 = java(
        """
        package com.test;
        import javax.inject.Inject;
        public class TestedClass {
        @Inject
        public TestedClass(CleanConstructorWithStaticMethod a1, ExpensiveConstructor a2) {
           foo();
        }

        public void foo() {
            Thread.sleep(1000L);
        }
      }"""
    ).indented()

    @Test
    fun injectedClassHasExpensiveExpression() {
        TestLintTask.lint()
            .files(cleanConstructorClass, expensiveConstructorClass, testedClass1)
            .issues(CleanConstructorDetector.ISSUE, CleanConstructorDetector.INJECT_ISSUE)
            .run()
            .expect(
                """
src/com/test/TestedClass.java:5: Warning: Constructor with @Inject annotation injected object that has expensive constructor: com.test.ExpensiveConstructor [InjectedExpensiveConstructor]
  public TestedClass(CleanConstructorWithStaticMethod a1, ExpensiveConstructor a2) {
                                                                               ~~
src/com/test/ExpensiveConstructor.java:4: Warning: Constructor has expensive method calls: foo [ExpensiveConstructor]
     foo();
     ~~~
src/com/test/TestedClass.java:6: Warning: Constructor has expensive method calls: foo [ExpensiveConstructor]
     foo();
     ~~~
0 errors, 3 warnings
                """
                    .trimMargin()
            )
    }

    @Test
    fun ignoreMethodReferenceInAddListener() {
        TestLintTask.lint()
            .files(
                cleanConstructorClass,
                java(
                    """
      package com.test;
      public class CleanConstructorWithMethodReference {
        public CleanConstructorWithMethodReference(CleanConstructorWithStaticMethod foo) {
            foo.addListener(this::someListenerMethod);
        }
        
        private void someListenerMethod(){
            //do something
        }

        public static void foo() {
            Thread.sleep(1000L);
        }
      }"""
                ).indented()
            )
            .issues(CleanConstructorDetector.ISSUE, CleanConstructorDetector.INJECT_ISSUE)
            .run()
            .expect("No warnings.")
    }

}
