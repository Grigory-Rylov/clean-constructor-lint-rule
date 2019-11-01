package com.github.grishberg.cleanconstructorlintplugin.scopes

import org.jetbrains.uast.UAnnotation

/**
 * Order of scopes.
 */
class Scopes {
    /**
     * return {@code true} when annotations of {@param comparableAnnotations} is from different
     * scope chain.
     */
    fun isDifferentScopes(
        target: List<UAnnotation>,
        comparableAnnotations: List<UAnnotation>
    ): Boolean {

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