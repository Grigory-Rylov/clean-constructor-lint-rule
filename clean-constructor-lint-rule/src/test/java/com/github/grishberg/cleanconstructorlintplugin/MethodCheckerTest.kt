package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

class MethodCheckerTest {
    private val observable = LintDetectorTest.java(
        """
      package foo;
      import java.util.ArrayList;
      public class SomeObservable {
        private Runnable listener;
        private boolean condition = false;
        private final ArrayList<Runnable> observers = new ArrayList<>();
        public void setSomeListener(Runnable r) {
            listener = r;
        }
        
        public void addObserver(Runnable r) {
            observers.add(r);
        }
        
        public void addSmartObserver(Runnable r) {
            observers.add(r);
            if (condition) {
                r.run();
            }
        }
        
        public void addExpensiveObserver(Runnable r) {
            Thread.sleep(1000);
            observers.add(r);
        }
      }"""
    ).indented()

    @Test
    fun allowedSetters() {
        TestLintTask.lint()
            .files(
                observable,
                LintDetectorTest.java(
                    """
          package foo;
          class Example {
            
            public Example(SomeObservable observable) {
                observable.setSomeListener(new Listener());
                observable.addObserver(new Listener());
                observable.addSmartObserver(new Listener());
            }
            
            private class Listener implements Runnable {
                @Override
                public void run() {
                    System.out.println(100500);
                }
            }
            
          }"""
                ).indented()
            )
            .issues(CleanConstructorsRegistry.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun notAllowedSetter() {
        TestLintTask.lint()
            .files(
                observable,
                LintDetectorTest.java(
                    """
          package foo;
          class Example implements Runnable {
            public Example(SomeObservable observable) {
                observable.addExpensiveObserver(this);
            }
           
            @Override
            public void run() {
                System.out.println(100500);
            }
                
          }"""
                ).indented()
            )
            .issues(CleanConstructorsRegistry.ISSUE)
            .run()
            .expect(
                """
src/foo/Example.java:4: Warning: Constructor has expensive method calls: addExpensiveObserver [ExpensiveConstructor]
      observable.addExpensiveObserver(this);
                 ~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
            """.trimIndent()
            )
    }

    @Test
    fun allowedGetters() {
        TestLintTask.lint()
            .files(
                LintDetectorTest.java(
                    """
          package foo;
          class Example {
            private boolean f1 = true;
            private boolean f2 = false;
            private boolean f3 = true;
            
            private final boolean f1;
            private final boolean f2;
            private final boolean f3;
            
            public Example() {
                f1 = getBooleanExpr1();
                f2 = getBooleanExpr2();
                f3 = getBooleanExpr3();
            }
            
            private boolean getBooleanExpr1() {
                return f1 && f2 || f3;
            }
            
            private boolean getBooleanExpr2() {
                return f1 && f2 && f3;
            }
            
            private boolean getBooleanExpr3() {
                return f1 && f2 && (getBooleanExpr1() || getBooleanExpr2());
            }
            
          }"""
                ).indented()
            )
            .issues(CleanConstructorsRegistry.ISSUE)
            .run()
            .expectClean()
    }
}