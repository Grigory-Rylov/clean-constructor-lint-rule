package com.github.grishberg.cleanconstructorlintplugin.graph

class DependencyNode(
    val name: String,
    parentNode: DependencyNode? = null
) {
    private val path = LinkedHashSet<String>()
    private val children = mutableListOf<DependencyNode>()

    init {
        if (parentNode != null) {
            path.addAll(parentNode.path)
        }
        path.add(name)
    }

    fun addChild(injectedClass: DependencyNode) {
        children.add(injectedClass)
        injectedClass.addRootPath(path)
    }

    private fun addRootPath(rootPath: LinkedHashSet<String>) {
        path.clear()
        path.addAll(rootPath)
        path.add(name)
    }

    fun hasElement(element: DependencyNode): Boolean = path.contains(element.name)

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
        return "{name = " + name + ", children count = " + children.size + "}"
    }

    fun printPath(sb: InjectedIssueString) {
        if (children.isEmpty()) {
            doPrintPath(sb)
            return
        }

        for (node in children) {
            node.printPath(sb)
        }
    }

    private fun doPrintPath(stringCollection: InjectedIssueString) {
        val sb = StringBuilder()
        for ((pos, element) in path.withIndex()) {
            if (pos == 0) {
                continue
            }
            sb.append(element)
            if (pos < path.size - 1) {
                sb.append(" -> ")
            }
        }
        stringCollection.addStringPath(sb.toString())
    }
}