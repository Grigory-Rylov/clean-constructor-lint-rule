package com.github.grishberg.cleanconstructorlintplugin.scopes

/**
 * Abstraction of scope dependency priority.
 * First elements in {@param scopes} is created earlier than subsequent elements.
 */
class ScopeChain(
    private val name: String,
    private val scopes: List<String>
) {
    fun hasScope(name: String): Boolean = scopes.contains(name)

    /**
     * Returns {@code true} if {@param parentScope} is created earlier or at the same time
     * as {@param dependencyScope}.
     */
    fun scopeLevel(annotation: String?): Int {
        if (annotation == null) {
            return -1
        }
        return scopes.indexOf(annotation)
    }
}