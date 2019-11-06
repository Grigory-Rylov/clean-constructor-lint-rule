package com.github.grishberg.cleanconstructorlintplugin

import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.TypeEvaluator
import com.github.grishberg.cleanconstructorlintplugin.ignore.BlackListTypes
import com.github.grishberg.cleanconstructorlintplugin.ignore.IgnoredSupertypes
import com.github.grishberg.cleanconstructorlintplugin.ignore.IgnoredTypes
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiType
import org.jetbrains.uast.*

class ClassMembersChecks(
    private val context: JavaContext
) {
    /**
     * return {@code true} if current method is allowed in {@link IgnoredTypes}.
     */
    fun isAllowedMethod(expression: UMethod): Boolean {
        val methodName = expression.name
        if (isAllowedIdentifier(methodName)) {
            return true
        }

        return isExcludedTypeInExpression(expression)
    }

    /**
     * recursively searches excluded method in subtypes.
     */
    private fun isExcludedTypeInExpression(expression: UMethod): Boolean {
        val psiClass = expression.containingClass ?: return false
        val uClass = context.uastContext.getClass(psiClass)
        val typeName = uClass.qualifiedName

        val allowedMethodsOfClass = IgnoredTypes.TYPES[typeName]
        if (allowedMethodsOfClass != null) {
            if (allowedMethodsOfClass.isEmpty() || allowedMethodsOfClass.contains(expression.name)) {
                return true
            }
        }
        for (subtype in uClass.superTypes) {
            if (isAllowedType(subtype, expression.name)) {
                return true
            }
        }
        return false
    }

    fun isTypeOfExpressionInBlackList(expression: UMethod): Boolean {
        val psiClass = expression.containingClass ?: return false
        val uClass = context.uastContext.getClass(psiClass)
        val typeName = uClass.qualifiedName

        val allowedMethodsOfClass = BlackListTypes.TYPES[typeName]
        if (allowedMethodsOfClass != null) {
            if (allowedMethodsOfClass.isEmpty() || allowedMethodsOfClass.contains(expression.name)) {
                return true
            }
        }
        for (subtype in uClass.superTypes) {
            if (isInBlackList(subtype, expression.name)) {
                return true
            }
        }
        return false
    }

    private fun isInBlackList(type: PsiType, methodName: String): Boolean {
        val typeName = extractRawType(type.getCanonicalText(false))
        if (isMethodForTypeInBlackList(typeName, methodName)) {
            return true
        }
        for (subtype in type.superTypes) {
            if (isMethodInBlackList(subtype, methodName)) {
                return true
            }
        }
        return false
    }

    private fun isMethodInBlackList(type: PsiType, methodName: String): Boolean {
        val typeName = extractRawType(type.getCanonicalText(false))
        if (isMethodForTypeInBlackList(typeName, methodName)) {
            return true
        }
        for (subtype in type.superTypes) {
            if (isMethodInBlackList(subtype, methodName)) {
                return true
            }
        }
        return false
    }

    private fun isMethodForTypeInBlackList(typeName: String, methodName: String): Boolean {
        val allowedMethodsOfClass = BlackListTypes.TYPES[typeName]
        if (allowedMethodsOfClass != null) {
            if (allowedMethodsOfClass.isEmpty() || allowedMethodsOfClass.contains(methodName)) {
                return true
            }
        }
        return false
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
            if (IgnoredSupertypes.IGNORED_PARENTS.contains(name)) {
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

    /**
     * return {@code true} if current class.method is exists in IgnoredTypes.TYPES.
     */
    fun isExcludedClassInExpression(expression: UCallExpression): Boolean {
        val receiver = expression.receiver
        val methodName = expression.methodName ?: return false
        val method = expression.resolveToUElement()
        if (method is UMethod && isAllowedMethod(method)) {
            return true
        }
        val type: PsiType? = TypeEvaluator.evaluate(receiver)
        if (type != null) {
            return isAllowedType(type, methodName)
        }
        return false
    }

    private fun isAllowedType(type: PsiType, methodName: String): Boolean {
        val typeName = extractRawType(type.getCanonicalText(false))
        if (isAllowedMethodForType(typeName, methodName)) {
            return true
        }
        for (subtype in type.superTypes) {
            if (isAllowedType(subtype, methodName)) {
                return true
            }
        }
        return false
    }

    private fun isAllowedMethodForType(typeName: String, methodName: String): Boolean {
        val allowedMethodsOfClass = IgnoredTypes.TYPES[typeName]
        if (allowedMethodsOfClass != null) {
            if (allowedMethodsOfClass.isEmpty() || allowedMethodsOfClass.contains(methodName)) {
                return true
            }
        }
        return false
    }

    fun extractRawType(name: String): String {
        val pos = name.indexOf("<")
        if (pos > 0) {
            return name.substring(0, pos)
        }
        return name
    }

    fun isAbstractClass(uClass: UClass): Boolean {
        val modifierList = uClass.modifierList ?: return false
        return hasAbstractModifier(modifierList)
    }

    fun isAbstractMethod(uMethod: UMethod): Boolean {
        val modifierList = uMethod.modifierList
        return hasAbstractModifier(modifierList)
    }

    private fun hasAbstractModifier(modifierList: PsiModifierList) =
        modifierList.hasModifierProperty(PsiModifier.ABSTRACT)

    fun findUClassByName(fullClassName: String): UClass? {
        val clazz = context.evaluator.findClass(fullClassName) ?: return null
        return context.uastContext.getClass(clazz)
    }

    fun extractAnnotationsFromMethod(method: UMethod): List<UAnnotation> {
        val uClass = extractUClassFromMethod(method)
        return uClass?.annotations ?: emptyList()
    }

    fun extractUClassFromMethod(constructorMethod: UMethod): UClass? {
        val psiClass = constructorMethod.containingClass ?: return null
        return context.uastContext.getClass(psiClass)
    }

    fun extractRawTypeFromConstructor(constructorMethod: UMethod): String {
        val uClass = extractUClassFromMethod(constructorMethod)
        val rawName = uClass?.qualifiedName?.let { extractRawType(it) }
        return rawName ?: ""
    }

    companion object {
        private val ACCEPTED_METHODS = listOf("this", "super")
    }
}