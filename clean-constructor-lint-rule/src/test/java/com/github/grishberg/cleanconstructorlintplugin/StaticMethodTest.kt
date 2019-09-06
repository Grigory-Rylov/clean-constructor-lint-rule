package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

class StaticMethodTest {
    private val staticMethods = LintDetectorTest.java(
        """
      package foo;
      import android.support.v7.widget.RecyclerView;
      import android.view.View;

      public class Utility {
        private static Runnable listener;
        private static Utility sInstance;
        private Utility() { }
        
        public static Utility get() {
            if (sInstance == null) {
                sInstance = new Utility();
            }
            return sInstance;
        }
        public void setListener(Runnable r) {
            listener = r;
        }
        
      }"""
    ).indented()

    @Test
    fun allowStaticSetter() {
        TestLintTask.lint()
            .files(
                staticMethods,
                LintDetectorTest.java(
                    """
      package foo;
      public class Example {
        public Example() {
            init();
        }
        
        private static void init() {
            Utility u = Utility.get();
            u.setListener(new Listener());
        }

        private class Listener implements Runnable {
            @Override
            public void run() {}
        }
      }"""
                ).indented()
            )
            .issues(CleanConstructorDetector.ISSUE, CleanConstructorDetector.INJECT_ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun allowIgnoredStaticMethods() {
        TestLintTask.lint()
            .files(
                LintDetectorTest.java(
                    """
      package foo;
      
      import java.util.Collections;
      import java.util.List;
      public class Example {
        private List<String> list;
        public Example() {
            list = Collections.emptyList();
            init();
        }
        
        private void init() {
            list = Collections.emptyList();
        }
      }"""
                ).indented()
            )
            .issues(CleanConstructorDetector.ISSUE)
            .run()
            .expectClean()
    }
}