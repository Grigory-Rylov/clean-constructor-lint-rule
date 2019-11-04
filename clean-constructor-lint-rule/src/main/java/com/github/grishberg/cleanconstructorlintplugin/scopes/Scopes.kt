package com.github.grishberg.cleanconstructorlintplugin.scopes

import org.jetbrains.uast.UAnnotation

/**
 * Order of scopes.
 */
private const val LINT_SCOPES_ENABLED_KEY = "LINT_SCOPES_ENABLED"
private const val LINT_SCOPES_VALUE_KEY_ = "LINT_SCOPES"
private const val TRUE_VALUE = "true"

class Scopes {
    private val scopesEnabled: Boolean

    init {
        val scopesState = System.getenv(LINT_SCOPES_ENABLED_KEY)
        scopesEnabled = if (scopesState == null) {
            true
        } else {
            TRUE_VALUE == scopesState
        }
    }

    /**
     * return {@code true} when root node scope chan is the same as injected node scope
     * and root node scope level is greater or the same as injected node scope level.
     *
     */
    fun isAllowedScope(
        rootNodeAnnotations: List<UAnnotation>,
        injectedNodeAnnotations: List<UAnnotation>
    ): Boolean {
        if (!scopesEnabled) {
            return false
        }

        for (a in rootNodeAnnotations) {
            val rootNodeScopeChain = chainByScopeAnnotation(a) ?: continue
            val rootNodeLevel = rootNodeScopeChain.scopeLevel(a.qualifiedName)

            for (b in injectedNodeAnnotations) {
                val injectedNodeScopeChain = chainByScopeAnnotation(b) ?: continue
                return rootNodeLevel >= injectedNodeScopeChain.scopeLevel(b.qualifiedName)
            }
        }
        return false
    }

    private fun chainByScopeAnnotation(annotation: UAnnotation): ScopeChain? {
        for (chain in SCOPES) {
            val name = annotation.qualifiedName ?: continue
            if (chain.hasScope(name)) {
                return chain
            }
        }
        return null
    }

    companion object {
        private val SCOPES = listOf(
            ScopeChain(
                "main screen",
                listOf(
                    "MainScreenScope",
                    "MainScreenCardsScope",
                    "NavigationButtonBarScope"
                )
            ),
            ScopeChain(
                "details screen",
                listOf(
                    "DetailsScreenScope",
                    "NavigationButtonBarScope",
                    "DetailsToolbarScope"
                )
            )
        )
    }
}