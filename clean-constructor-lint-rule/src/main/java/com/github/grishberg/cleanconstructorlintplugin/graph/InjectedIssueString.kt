package com.github.grishberg.cleanconstructorlintplugin.graph

class InjectedIssueString {
    private val lines = mutableListOf<String>()

    fun addStringPath(path: String) {
        lines.add(path)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for ((pos, element) in lines.withIndex()) {
            sb.append(element)
            if (pos < lines.size - 1) {
                sb.append("\n")
            }
        }
        return sb.toString()
    }
}