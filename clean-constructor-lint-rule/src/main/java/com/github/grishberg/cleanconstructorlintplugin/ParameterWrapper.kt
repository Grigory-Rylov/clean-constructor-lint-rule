package com.github.grishberg.cleanconstructorlintplugin

import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UParameter

private const val FEATURE_OPTIONAL_NAME = "com.test.FeatureOptional"

class ParameterWrapper(
    private val membersChecks: ClassMembersChecks,
    constructorsParam: UParameter
) {
    var isGeneric: Boolean = false
        private set

    var genericClass: UClass? = null
        private set

    var uClass: UClass? = null
        private set

    init {
        val typeReference = constructorsParam.typeReference
        if (typeReference != null && typeReference.type is PsiClassReferenceType) {
            val type = typeReference.type as PsiClassReferenceType
            val rawTypeName = membersChecks.extractRawType(typeReference.type.canonicalText)

            uClass = membersChecks.findUClassByName(rawTypeName)
            if (type.parameters.isNotEmpty()) {
                isGeneric = true
                genericClass = membersChecks.findUClassByName(type.parameters.first().canonicalText)
                if (rawTypeName == FEATURE_OPTIONAL_NAME) {
                    uClass = genericClass
                }
            }
        }
    }
}