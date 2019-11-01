package com.github.grishberg.myapplication.main

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.github.grishberg.myapplication.annotations.list.MainScreenScope
import javax.inject.Inject

@MainScreenScope
class MainScreenController @Inject constructor(
    context: Context,
    private val mainButtonsController: MainButtonsController,
    private val cardsController: CardsController
) {
    private val content = FrameLayout(context)

    fun show(parentContent: ViewGroup) {
        parentContent.addView(content)
        mainButtonsController.show(content)
    }

    /**
     * Is call when cards received.
     */
    fun onCardsDownloaded(cards: List<String>) {
        cardsController.showCards(content, cards)
    }
}