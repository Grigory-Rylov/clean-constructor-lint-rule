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
            "java.util.Map",
            "android.support.v4.util.SparseArrayCompat",
            "android.util.SparseIntArray",
            "SparseArrayCompat"
        )

        fun isExcludedClassInExpression(node: UCallExpression): Boolean {
            val type: PsiType? = TypeEvaluator.evaluate(node.receiver)
            if (type != null) {
                val typeName = extractTypeWithoutGenericSubtype(type.getCanonicalText(false))
                if (IGNORED_TYPES.contains(typeName)) {
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

        private fun extractTypeWithoutGenericSubtype(name: String): String {
            val pos = name.indexOf("<")
            if (pos > 0) {
                return name.substring(0, pos)
            }
            return name
        }
    }
}