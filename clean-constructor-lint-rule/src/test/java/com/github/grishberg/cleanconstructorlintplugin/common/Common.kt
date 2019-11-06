package com.github.grishberg.cleanconstructorlintplugin.common

import com.android.tools.lint.checks.infrastructure.LintDetectorTest

object Common {
    val injectAnnotation = LintDetectorTest.kotlin(
        """
            package javax.inject
            annotation class Inject
        """
    ).indented()
}