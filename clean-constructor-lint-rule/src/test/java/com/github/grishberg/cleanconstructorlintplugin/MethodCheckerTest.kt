package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

class MethodCheckerTest {
    private val observableInterface = LintDetectorTest.java(
        """
      package foo;
      public interface ObservableInterface { 
        void addListener(Runnable r);
      }"""
    ).indented()

    private val superClass = LintDetectorTest.java(
        """
      package foo;
      import java.util.ArrayList;
      public class ParentClass {
        private Runnable listener;
        private boolean condition = false;
        public void setSomeListener(Runnable r) {
            listener = r;
        }
      }"""
    ).indented()

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

    private val abstractObservable = LintDetectorTest.java(
        """
      package foo;
      import java.util.ArrayList;
      public abstract class SomeObservable {
        private Runnable listener;
        private boolean condition = false;
        private final ArrayList<Runnable> observers = new ArrayList<>();
        public void setSomeListener(Runnable r) {
            listener = r;
        }
        
        public void addObserver(Runnable r) {
            observers.add(r);
            invalidateListeningState();
        }
        
        protected abstract void invalidateListeningState();
      }"""
    ).indented()

    private val observableWithStateInvalidation = LintDetectorTest.java(
        """
      package foo;
      import java.util.ArrayList;
      public class UpdateListener {
        private ArrayList<Runnable> listener = new ArrayList<>();
        private boolean isUpdateListening;
        private boolean isDestroyed;
        private final SomeObservable observable;
        private final UpdateListener listener = new UpdateListener();
        public UpdateListener(SomeObservable o) {
            observable = o;
            invalidateState();
        }
        
        public void addListener(Runnable r) {
            listener.add(r);
            invalidateState();
        }
        
        private void invalidateState() {
            if (shouldListening()) {
                return;
            }
            if (isUpdateListening || isDestroyed) {
                return;
            }
            isUpdateListening = true;

            observable.addObserver(listener);
        }
        
        private boolean shouldListening() {
            return !listener.isEmpty();
        }
        
        private class UpdateListener implements Runnable {
            @Override
            public void run() {
                // do something.
            }
        }
      }"""
    ).indented()

    @Test
    fun allowedIfExpression() {
        TestLintTask.lint()
            .files(abstractObservable, observableWithStateInvalidation)
            .issues(CleanConstructorDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun allowedSetters() {
        TestLintTask.lint()
            .files(
                observable, observableInterface,
                LintDetectorTest.java(
                    """
          package foo;
          
          class Example {
            
            public Example(SomeObservable observable, ObservableInterface observableInterface) {
                observable.setSomeListener(new Listener());
                observable.addObserver(new Listener());
                observable.addSmartObserver(new Listener());
                
                observableInterface.addListener(new Listener());
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
            .issues(CleanConstructorDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun allowedSetterInAbstractClass() {
        TestLintTask.lint()
            .files(
                observableInterface,
                LintDetectorTest.java(
                    """
          package foo;
          abstract class Example<C extends ObservableInterface> {
            public Example(C observable) {
                observable.addListener(new Listener());
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
            .issues(CleanConstructorDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun allowedSettersFromSuper() {
        TestLintTask.lint()
            .files(
                superClass,
                LintDetectorTest.java(
                    """
          package foo;
          class Example extends ParentClass {
            
            public Example() {
                setSomeListener(new Listener());
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
            .issues(CleanConstructorDetector.ISSUE)
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
            .issues(CleanConstructorDetector.ISSUE)
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
            .issues(CleanConstructorDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun allowedEqualsInConstructor() {
        TestLintTask.lint()
            .files(
                observable,
                LintDetectorTest.java(
                    """
          package foo;
          class Example {
            private static final String CONST = "value1";
            private final boolean f1;
            public Example() {
                f1 = CONST.equals("val");
            }
          }"""
                ).indented()
            )
            .issues(CleanConstructorDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun ignoreMethodsInIf() {
        TestLintTask.lint()
            .files(
                /*observable, observableWithStateInvalidation,*/
                LintDetectorTest.java(
                    """
          package foo;
          import java.util.ArrayList;
          import java.util.List;
          import java.util.Collections;
          class Example {
            private ArrayList<String> cache;
            private List<String> pages;
            public Example(/*UpdateListener ul*/) {
               // ul.addListener(new Runnable(){
               //     @Override
               //     public void run() { }
               // });
                pages = getPages();
            }
            
            List<String> getPages() {
                return cache == null ?
                Collections.emptyList() :
                new ArrayList<>(cache);
            }
          }
          """
                ).indented()
            )
            .issues(CleanConstructorDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun allowFor() {
        TestLintTask.lint()
            .files(
                LintDetectorTest.java(
                    """
          package foo;
          class Example {
            public Example(UpdateListener ul) {
                init();
            }
            
            private void init() {
                int counter = 0;
                for(int i = 0; i<100; i++) {
                    counter++;
                }
            }
          }
          """
                ).indented()
            )
            .issues(CleanConstructorDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun allowArrayAccess() {
        TestLintTask.lint()
            .files(
                LintDetectorTest.java(
                    """
          package foo;
          class Example {
            public Example(UpdateListener ul) {
                init();
            }
            
            private void init() {
                int[] a = new int[100];
                int firstElement = a[0];
            }
          }
          """
                ).indented()
            )
            .issues(CleanConstructorDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun dontAllowForWithExpensiveBody() {
        TestLintTask.lint()
            .files(
                LintDetectorTest.java(
                    """
          package foo;
          class Example {
            public Example(UpdateListener ul) {
                init();
            }
            
            private void init() {
                int counter = 0;
                for(int i = 0; i<100; i++) {
                    counter++;
                    Thread.sleep(1000);
                }
            }
          }
          """
                ).indented()
            )
            .issues(CleanConstructorDetector.ISSUE)
            .run()
            .expect("""
src/foo/Example.java:4: Warning: Constructor has expensive method calls: init [ExpensiveConstructor]
      init();
      ~~~~
0 errors, 1 warnings
            """.trimIndent())
    }
}
