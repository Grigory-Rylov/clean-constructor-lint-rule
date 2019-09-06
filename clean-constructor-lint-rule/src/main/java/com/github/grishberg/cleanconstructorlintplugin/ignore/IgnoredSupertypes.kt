package com.github.grishberg.cleanconstructorlintplugin.ignore

class IgnoredSupertypes {
    companion object {
        val IGNORED_PARENTS = listOf(
            "android.graphics.drawable.Drawable",
            "android.view.View",
            "android.support.v7.widget.RecyclerView.ViewHolder",
            "androidx.recyclerview.widget.RecyclerView.ViewHolder",
            "RecyclerView.ViewHolder"
        )
    }
}