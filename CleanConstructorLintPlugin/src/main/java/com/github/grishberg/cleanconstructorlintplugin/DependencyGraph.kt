package com.github.grishberg.cleanconstructorlintplugin

class DependencyGraph(private val rootClass: String) {
    private val elements = mutableListOf<String>()

    fun addElement(className: String) {
        elements.add(className)
    }

    fun addGraph(graph: DependencyGraph) {
        elements.add(graph.rootClass)
        elements.addAll(graph.elements)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for (i in 0 until elements.size) {
            sb.append(elements[i])
            if (i < elements.size - 1) {
                sb.append(" -> ")
            }
        }
        return sb.toString()
    }
}