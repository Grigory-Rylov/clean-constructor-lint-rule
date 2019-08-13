package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

class IgnoreViewHolderTest {
    private val viewHolderSample = LintDetectorTest.java(
        """
      package com.test;
      import android.support.v7.widget.RecyclerView;
      import android.view.View;

      public abstract class RecycableViewHolder extends android.support.v7.widget.RecyclerView.ViewHolder  {
        public RecycableViewHolder(View v) {
            super(v);
        }
      }"""
    ).indented()

    private val testedSubclassOfViewHolder = LintDetectorTest.java(
        """
      package com.test;
      import android.support.v7.widget.RecyclerView;
      import android.view.View;

      public class TestedViewHolder extends RecycableViewHolder  {
        public TestedViewHolder(View v) {
            super(v);
            View tv = v.findViewById(R.id.title);
        }
      }"""
    ).indented()


    @Test
    fun constructorHasMethodCalls() {
        TestLintTask.lint()
            .files(viewHolderSample, testedSubclassOfViewHolder)
            .issues(CleanConstructorsRegistry.ISSUE)
            .run()
            .expect(
                "No warnings."
            )
    }
}
