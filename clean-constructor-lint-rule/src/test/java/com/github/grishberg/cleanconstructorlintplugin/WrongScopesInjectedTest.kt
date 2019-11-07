package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

class WrongScopesInjectedTest {
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

    private val mainScreenController = LintDetectorTest.java(
        """
        package foo;
        import javax.inject.Inject;

        @MainScreenScope
        class MainScreenController {
            @Inject 
            public MainScreenController(
                MainButtonsController mainButtonsController,
                CardsController cardsController
            ) {
        
            }
        }"""
    ).indented()

    private val mainScreenButtonsController = LintDetectorTest.java(
        """
        package foo;
        import javax.inject.Inject;

        @MainScreenScope
        class MainButtonsController {
            @Inject 
            public MainButtonsController() {
                Thread.sleep(1000);
            }    
        }"""
    ).indented()

    private val cardsController = LintDetectorTest.java(
        """
        package foo;
        import javax.inject.Inject;

        @MainScreenCardsScope
        class CardsController {
            @Inject 
            public CardsController() {
                Thread.sleep(1000);
            }    
        }"""
    ).indented()

    @Test
    fun allowIgnoredStaticMethods() {
        TestLintTask.lint()
            .files(
                annotations,
                mainScreenButtonsController, cardsController,
                mainScreenController
            )
            .issues(CleanConstructorDetector.INJECT_ISSUE)
            .run()
            .expect(
                """
src/foo/MainScreenController.java:9: Warning: Constructor with @Inject annotation injected object that has expensive constructor: foo.CardsController [InjectedExpensiveConstructor]
        CardsController cardsController
                        ~~~~~~~~~~~~~~~
0 errors, 1 warnings         
""".trimIndent()
            )
    }
}