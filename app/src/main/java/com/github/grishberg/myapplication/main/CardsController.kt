package com.github.grishberg.myapplication.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.github.grishberg.myapplication.R
import com.github.grishberg.myapplication.annotations.list.MainScreenCardsScope
import javax.inject.Inject

@MainScreenCardsScope
class CardsController @Inject constructor(
    inflater: LayoutInflater
) {
    private val cardsView: View

    init {
        val inflateParent = FrameLayout(inflater.context)
        cardsView = inflater.inflate(R.layout.main_screen_cards, inflateParent, false)
    }

    fun showCards(content: ViewGroup, cards: List<String>) {
        content.addView(cardsView)
    }
}