package com.github.grishberg.cleanconstructorlintplugin.graph

class DependencyNode(val name: String, var isExpensive: Boolean) {

    val children = mutableListOf<DependencyNode>()
        get() = ArrayList(field)

    fun addInjectedClass(injectedClass: DependencyNode) {
        children.add(injectedClass)
    }

    fun hasElement(element: DependencyNode) : Boolean = children.contains(element)

    override fun equals(other: Any?): Boolean {
        if (other !is DependencyNode) {
            return false
        }
        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for (i in 0 until children.size) {
            sb.append(children[i].name)
            if (i < children.size - 1) {
                sb.append(" -> ")
            }
        }
        return sb.toString()
    }
}