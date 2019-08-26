package com.github.grishberg.cleanconstructorlintplugin.graph

import java.lang.IllegalStateException

data class ExpensiveClassContainer(val className: String, val diGraph: DependencyGraph) {
    override fun equals(other: Any?): Boolean {
        if (other !is ExpensiveClassContainer) {
            return false
        }
        return className == other.className
    }

    override fun hashCode(): Int {
        return className.hashCode()
    }
}

class ExpensiveConstructorsRepository {
    private val classes = mutableSetOf<ExpensiveClassContainer>()

    fun add(className: String, diGraph: DependencyGraph) {
        classes.add(ExpensiveClassContainer(className, diGraph))
    }

    fun contains(className: String): Boolean {
        for (c in classes) {
            if (c.className == className) {
                return true
            }
        }
        return false
    }

    fun getGraphByName(className: String): DependencyGraph {
        for (c in classes) {
            if (c.className == className) {
                return c.diGraph
            }
        }
        throw IllegalStateException("class name with name=$className not found")
    }
}