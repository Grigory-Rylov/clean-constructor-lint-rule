package com.github.grishberg.myapplication

import android.view.ViewGroup
import com.github.grishberg.myapplication.details.DetailsScreenController
import com.github.grishberg.myapplication.main.MainScreenController
import javax.inject.Inject
import javax.inject.Provider

class StartController @Inject constructor(
    private val mainScreen: Provider<MainScreenController>,
    private val detailsScreenModule: Provider<DetailsScreenController>
) {
    fun startedWithAction(action: String, parentContent: ViewGroup) {
        if (action == DETAILS_ACTION) {
            detailsScreenModule.get().show(parentContent)
        } else {
            mainScreen.get().show(parentContent)
        }
    }

    companion object {
        val DETAILS_ACTION = "DETAILS_ACTION"
    }
}