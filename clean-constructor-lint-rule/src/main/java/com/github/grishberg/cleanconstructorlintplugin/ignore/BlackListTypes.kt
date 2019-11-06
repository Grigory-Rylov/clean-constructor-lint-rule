package com.github.grishberg.cleanconstructorlintplugin.ignore

object BlackListTypes {
    val TYPES = mapOf(
        Pair("android.view.LayoutInflater", listOf("inflate"))
    )
}