package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.github.grishberg.cleanconstructorlintplugin.common.Common
import org.junit.Test

class ExpensiveInflating {

    private val annotations = LintDetectorTest.kotlin(
        """
            package foo

            import javax.inject.Scope

            @Scope
            @Retention(AnnotationRetention.SOURCE)
            annotation class MainScreenScope

            @Scope  
            @Retention(AnnotationRetention.SOURCE)
            annotation class MainScreenCardsScope

            """
    ).indented()
    private val cardsController = LintDetectorTest.kotlin(
        """
        package foo
        import android.view.LayoutInflater
        import android.view.View
        import android.view.ViewGroup
        import android.widget.FrameLayout
        import foo.myapplication.R

        import javax.inject.Inject
        
        
        class CardsController @Inject constructor(
            inflater: LayoutInflater
        ) {
            private val cardsView: View

            init {
                cardsView = inflater.inflate(R.layout.main_layout, null, false)
            }
}"""
    ).indented()

    private val mainScreenButtonsController = LintDetectorTest.kotlin(
        """
        package foo
        import android.view.LayoutInflater
        import android.view.View
        import android.view.ViewGroup
        import android.widget.FrameLayout
        import foo.myapplication.R

        import javax.inject.Inject

        @MainScreenScope
        class MainButtonsController @Inject constructor(   
            inflater: LayoutInflater
        ){
            private val view: View
             init {
                view = inflater.inflate(R.layout.main_layout, null, false)
            }
        }"""
    ).indented()


    private val mainScreenController = LintDetectorTest.kotlin(
        """
        package foo
        import javax.inject.Inject

        @MainScreenScope
        class MainScreenController @Inject constructor(
                private val mainButtonsController: MainButtonsController,
                private val cardsController:CardsController
        ){
            fun show(){}
        }"""
    ).indented()


    @Test
    fun ignoreMethodReferenceInAddListener() {
        TestLintTask.lint()
            .files(
                Common.injectAnnotation, annotations,
                cardsController, mainScreenButtonsController,
                mainScreenController
            )
            .issues(CleanConstructorDetector.ISSUE, CleanConstructorDetector.INJECT_ISSUE)
            .run()
            .expect(
                """
src/foo/MainScreenController.kt:7: Warning: Constructor with @Inject annotation injected object that has expensive constructor: foo.CardsController [InjectedExpensiveConstructor]
        private val cardsController:CardsController
                    ~~~~~~~~~~~~~~~
src/foo/CardsController.kt:17: Warning: Constructor has expensive method calls: inflate [ExpensiveConstructor]
                cardsView = inflater.inflate(R.layout.main_layout, null, false)
                                     ~~~~~~~
src/foo/MainButtonsController.kt:16: Warning: Constructor has expensive method calls: inflate [ExpensiveConstructor]
        view = inflater.inflate(R.layout.main_layout, null, false)
                        ~~~~~~~
0 errors, 3 warnings
                """
                    .trimMargin()
            )
    }
}