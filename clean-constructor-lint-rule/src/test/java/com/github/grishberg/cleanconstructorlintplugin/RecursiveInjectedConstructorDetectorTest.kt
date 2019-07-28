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

    private val expensiveConstructorClass2 = java(
        """
      package com.test;
      import javax.inject.Inject;
      public class InjectedConstructor {
        @Inject
        public InjectedConstructor(ExpensiveConstructor c) { }
      }"""
    ).indented()

    private val expensiveConstructorClass3 = java(
        """
      package com.test;
      import javax.inject.Inject;
      public class TestedClass {
        @Inject
        public TestedClass(InjectedConstructor ic) { }
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
            .files(expensiveConstructorClass3, expensiveConstructorClass2, expensiveConstructorClass)
            .issues(CleanConstructorsRegistry.ISSUE, CleanConstructorsRegistry.INJECT_ISSUE)
            .run()
            .expect(
                """
src/com/test/InjectedConstructor.java:5: Warning: Constructor with @Inject annotation injected object that has expensive constructor: com.test.ExpensiveConstructor [InjectedExpensiveConstructor]
  public InjectedConstructor(ExpensiveConstructor c) { }
                                                  ~
src/com/test/TestedClass.java:5: Warning: Constructor with @Inject annotation injected object that has expensive constructor: com.test.InjectedConstructor -> com.test.ExpensiveConstructor [InjectedExpensiveConstructor]
  public TestedClass(InjectedConstructor ic) { }
                                         ~~
src/com/test/ExpensiveConstructor.java:4: Warning: Constructor has expensive method calls: foo [ExpensiveConstructor]
      foo();
      ~~~
0 errors, 3 warnings
                """
                    .trimMargin()
            )
    }

    @Test
    fun injectedClassHasExpensiveExpression() {
        lint()
            .files(expensiveExpressionClass, injectedExpensiveExpression, expensiveConstructorClass)
            .issues(CleanConstructorsRegistry.ISSUE, CleanConstructorsRegistry.INJECT_ISSUE)
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