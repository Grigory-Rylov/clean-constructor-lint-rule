package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.detector.api.TypeEvaluator
import com.intellij.psi.PsiType
import org.jetbrains.uast.UCallExpression

class ExcludedClasses {
    companion object {
        private val IGNORED_TYPES = listOf(
            "java.util.AbstractMap",
            "java.util.AbstractList",
            "java.util.List",
            "java.util.Map"
        )

        fun isExcludedClassInExpression(node: UCallExpression): Boolean {
            val type: PsiType? = TypeEvaluator.evaluate(node.receiver)
            if (type != null) {
                if (IGNORED_TYPES.contains(type.getCanonicalText(false))) {
                    return true
                }
                for (subtype in type.superTypes) {
                    if (IGNORED_TYPES.contains(subtype.getCanonicalText(false))) {
                        return true
                    }
                }
            }

            return false
        }
    }
}