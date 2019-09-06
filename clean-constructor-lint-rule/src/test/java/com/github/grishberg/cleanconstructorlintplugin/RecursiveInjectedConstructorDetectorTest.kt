package com.github.grishberg.cleanconstructorlintplugin


import com.android.tools.lint.checks.infrastructure.LintDetectorTest.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class RecursiveInjectedConstructorDetectorTest {
    private val expensiveConstructorClass = java(
        """
      package com.test;
      public class ExpensiveConstructor {
        public ExpensiveConstructor() {
            foo();
        }

        private void foo() {
            Thread.sleep(1000L);
        }
      }"""
    ).indented()

    private val expensiveConstructor2Class = java(
        """
      package com.test;
      public class ExpensiveConstructor2 {
        public ExpensiveConstructor2() {
            foo();
        }

        private void foo() {
            Thread.sleep(1000L);
        }
      }"""
    ).indented()

    private val expensiveConstructorClass2 = java(
        """
      package com.test;
      import javax.inject.Inject;
      public class InjectedConstructor {
        @Inject
        public InjectedConstructor(ExpensiveConstructor c) { }
      }"""
    ).indented()

    private val injectedConstructorClass3 = java(
        """
      package com.test;
      import javax.inject.Inject;
      public class InjectedConstructor2 {
        @Inject
        public InjectedConstructor2(ExpensiveConstructor2 c) { }
      }"""
    ).indented()

    private val expensiveConstructorClass3 = java(
        """
      package com.test;
      import javax.inject.Inject;
      public class TestedClass {
        @Inject
        public TestedClass(InjectedConstructor ic, InjectedConstructor2 ic2) { }
      }"""
    ).indented()

    private val expensiveConstructorClassInKotlin2 = kotlin(
        """
        package com.test
        import javax.annotation.Nonnull
        import javax.inject.Inject
        class InjectedConstructor @Inject @Nonnull constructor(c: ExpensiveConstructor){
      }"""
    ).indented()
    private val expensiveConstructorClassInKotlin = kotlin(
        """
        package com.test
        import javax.inject.Inject
        class TestedClass  @Inject constructor(private val ic: InjectedConstructor){
            fun someMethod() {

            }
      }"""
    ).indented()

    private val expensiveExpressionClass = java(
        """
      package com.test;
      public class ExpensiveExpression {
        public ExpensiveExpression() {
            ExpensiveConstructor c = new ExpensiveConstructor();
        }
      }"""
    ).indented()

    private val injectedExpensiveExpression = java(
        """
      package com.test;
      import javax.inject.Inject;
      public class InjectedExpensiveExpression {
        @Inject
        public InjectedExpensiveExpression(ExpensiveExpression exp) {
        }
      }"""
    ).indented()

    @Test
    fun constructorHasMethodCalls() {
        lint()
            .files(expensiveConstructorClass3, expensiveConstructorClass2, expensiveConstructorClass,
                expensiveConstructor2Class, injectedConstructorClass3)
            .issues(CleanConstructorDetector.ISSUE, CleanConstructorDetector.INJECT_ISSUE)
            .run()
            .expect(
                """
src/com/test/InjectedConstructor.java:5: Warning: Constructor with @Inject annotation injected object that has expensive constructor: com.test.ExpensiveConstructor [InjectedExpensiveConstructor]
  public InjectedConstructor(ExpensiveConstructor c) { }
                                                  ~
src/com/test/InjectedConstructor2.java:5: Warning: Constructor with @Inject annotation injected object that has expensive constructor: com.test.ExpensiveConstructor2 [InjectedExpensiveConstructor]
  public InjectedConstructor2(ExpensiveConstructor2 c) { }
                                                    ~
src/com/test/TestedClass.java:5: Warning: Constructor with @Inject annotation injected object that has expensive constructor: com.test.InjectedConstructor -> com.test.ExpensiveConstructor [InjectedExpensiveConstructor]
  public TestedClass(InjectedConstructor ic, InjectedConstructor2 ic2) { }
                                         ~~
src/com/test/TestedClass.java:5: Warning: Constructor with @Inject annotation injected object that has expensive constructor: com.test.InjectedConstructor2 -> com.test.ExpensiveConstructor2 [InjectedExpensiveConstructor]
  public TestedClass(InjectedConstructor ic, InjectedConstructor2 ic2) { }
                                                                  ~~~
src/com/test/ExpensiveConstructor.java:4: Warning: Constructor has expensive method calls: foo [ExpensiveConstructor]
      foo();
      ~~~
src/com/test/ExpensiveConstructor2.java:4: Warning: Constructor has expensive method calls: foo [ExpensiveConstructor]
      foo();
      ~~~
0 errors, 6 warnings
                """
                    .trimMargin()
            )
    }

    @Test
    fun injectedClassHasExpensiveExpression() {
        lint()
            .files(expensiveExpressionClass, injectedExpensiveExpression, expensiveConstructorClass)
            .issues(CleanConstructorDetector.ISSUE, CleanConstructorDetector.INJECT_ISSUE)
            .run()
            .expect(
                """
src/com/test/InjectedExpensiveExpression.java:5: Warning: Constructor with @Inject annotation injected object that has expensive constructor: com.test.ExpensiveExpression [InjectedExpensiveConstructor]
  public InjectedExpensiveExpression(ExpensiveExpression exp) {
                                                         ~~~
src/com/test/ExpensiveConstructor.java:4: Warning: Constructor has expensive method calls: foo [ExpensiveConstructor]
      foo();
      ~~~
src/com/test/ExpensiveExpression.java:4: Warning: Constructor creates object that has expensive constructor: ExpensiveExpression [ExpensiveConstructor]
      ExpensiveConstructor c = new ExpensiveConstructor();
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 3 warnings
                """
                    .trimMargin()
            )
    }
}