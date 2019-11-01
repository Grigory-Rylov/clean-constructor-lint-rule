package com.github.grishberg.myapplication.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.github.grishberg.myapplication.R
import com.github.grishberg.myapplication.annotations.list.MainScreenScope
import javax.inject.Inject

@MainScreenScope
class MainButtonsController @Inject constructor(
    inflater: LayoutInflater
) {
    private val buttonsView: View

    init {
        val inflateParent = FrameLayout(inflater.context)
        buttonsView = inflater.inflate(R.layout.main_screen_toolbar, inflateParent, false)
    }

    fun show(content: ViewGroup) {
        content.addView(buttonsView)
    }
}
