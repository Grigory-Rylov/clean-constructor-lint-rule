package com.github.grishberg.cleanconstructorlintplugin.ignore

class IgnoredTypes {
    companion object {
        val TYPES = mapOf(
            Pair("java.lang.Object", listOf("equals")),
            Pair("java.util.Collection", emptyList()),
            Pair("java.util.Collections", emptyList()),
            Pair("java.util.Map", emptyList()),
            Pair("android.support.v4.util.SparseArrayCompat", emptyList()),
            Pair("android.util.SparseIntArray", emptyList()),
            Pair("androidx.lifecycle.ViewModel", emptyList()),
            Pair("SparseArrayCompat", emptyList()),
            Pair("android.os.Bundle", emptyList())
        )
    }
}