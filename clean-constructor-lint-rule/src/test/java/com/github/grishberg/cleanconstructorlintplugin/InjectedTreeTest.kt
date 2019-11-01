package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

class InjectedTreeTest {
    private val testedClass = LintDetectorTest.java(
        """
      package foo;
      import javax.inject.Inject;
      public class SampleClass {
        @Inject
        public SampleClass(InjectedClass c) {
        }
      }"""
    ).indented()

    private val injectedClass = LintDetectorTest.java(
        """
      package foo;
      import javax.inject.Inject;
      public class InjectedClass {
        @Inject
        public InjectedClass(ExpensiveClassA a, ExpensiveClassB b, ExpensiveClassC c) {
        }
      }"""
    ).indented()

    private val expensiveClassA = LintDetectorTest.java(
        """
      package foo;
      public class ExpensiveClassA {
        public ExpensiveClassA() {
            Thread.sleep(1000);
        }
      }"""
    ).indented()

    private val expensiveClassB = LintDetectorTest.java(
        """
      package foo;
      public class ExpensiveClassB {
        public ExpensiveClassB() {
            Thread.sleep(1000);
        }
      }"""
    ).indented()

    private val expensiveClassC = LintDetectorTest.java(
        """
      package foo;
      public class ExpensiveClassC {
        public ExpensiveClassC() {
            Thread.sleep(1000);
        }
      }"""
    ).indented()


    @Test
    fun allowIgnoredStaticMethods() {
        TestLintTask.lint()
            .files(testedClass, injectedClass, expensiveClassA, expensiveClassB, expensiveClassC)
            .issues(CleanConstructorDetector.INJECT_ISSUE)
            .run()
            .expect("""
src/foo/InjectedClass.java:5: Warning: Constructor with @Inject annotation injected object that has expensive constructor: foo.ExpensiveClassA [InjectedExpensiveConstructor]
  public InjectedClass(ExpensiveClassA a, ExpensiveClassB b, ExpensiveClassC c) {
                                       ~
src/foo/InjectedClass.java:5: Warning: Constructor with @Inject annotation injected object that has expensive constructor: foo.ExpensiveClassB [InjectedExpensiveConstructor]
  public InjectedClass(ExpensiveClassA a, ExpensiveClassB b, ExpensiveClassC c) {
                                                          ~
src/foo/InjectedClass.java:5: Warning: Constructor with @Inject annotation injected object that has expensive constructor: foo.ExpensiveClassC [InjectedExpensiveConstructor]
  public InjectedClass(ExpensiveClassA a, ExpensiveClassB b, ExpensiveClassC c) {
                                                                             ~
src/foo/SampleClass.java:5: Warning: Constructor with @Inject annotation injected object that has expensive constructor: foo.InjectedClass -> foo.ExpensiveClassA
foo.InjectedClass -> foo.ExpensiveClassB
foo.InjectedClass -> foo.ExpensiveClassC [InjectedExpensiveConstructor]
  public SampleClass(InjectedClass c) {
                                   ~
0 errors, 4 warnings
            """.trimIndent())
    }
}