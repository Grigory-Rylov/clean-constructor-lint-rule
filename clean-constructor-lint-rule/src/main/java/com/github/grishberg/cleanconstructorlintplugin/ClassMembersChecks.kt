package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.TypeEvaluator
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod

class ClassMembersChecks {
    fun isAllowedMethod(expression: UMethod): Boolean {
        val methodName = expression.name
        return isAllowedIdentifier(methodName)
    }

    fun isAllowedIdentifier(elementName: String): Boolean {
        return ACCEPTED_METHODS.contains(elementName)
    }

    fun isCallInAnonymousClass(node: UCallExpression): Boolean {
        var parent: UElement? = node.uastParent
        while (parent != null) {
            if (parent is UClass && parent.name == null) {
                return true
            }
            parent = parent.uastParent
        }
        return false
    }

    fun isIgnoredSupertype(node: UClass, context: JavaContext): Boolean {
        for (superType in node.uastSuperTypes) {
            val name = superType.getQualifiedName() ?: superType.toString()
            if (IGNORED_PARENTS.contains(name)) {
                return true
            } else {
                val className = superType.getQualifiedName() ?: continue
                val clazz = context.evaluator.findClass(className) ?: continue
                val superTypeClass = context.uastContext.getClass(clazz)
                if (isIgnoredSupertype(superTypeClass, context)) {
                    return true
                }
            }
        }
        return false
    }

    fun isPrivateClass(uClass: UClass): Boolean {
        val modifiers = uClass.modifierList ?: return false
        return modifiers.hasModifierProperty(PsiModifier.PRIVATE)
    }

    companion object {
        private val ACCEPTED_METHODS = listOf("this", "super")

        private val IGNORED_PARENTS = listOf(
            "android.graphics.drawable.Drawable",
            "android.view.View",
            "android.support.v7.widget.RecyclerView.ViewHolder",
            "androidx.recyclerview.widget.RecyclerView.ViewHolder",
            "RecyclerView.ViewHolder"
        )

        private val IGNORED_TYPES = listOf(
            "java.util.Collection",
            "java.util.Map",
            "android.support.v4.util.SparseArrayCompat",
            "android.util.SparseIntArray",
            "androidx.lifecycle.ViewModel",
            "SparseArrayCompat"
        )

        fun isExcludedClassInExpression(node: UCallExpression): Boolean {
            val type: PsiType = TypeEvaluator.evaluate(node.receiver) ?: return false
            return isAvailableType(type)
        }

        private fun isAvailableType(type: PsiType): Boolean {
            val typeName = extractTypeWithoutGenericSubtype(type.getCanonicalText(false))
            if (IGNORED_TYPES.contains(typeName)) {
                return true
            }
            for (subtype in type.superTypes) {
                if (isAvailableType(subtype)) {
                    return true
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