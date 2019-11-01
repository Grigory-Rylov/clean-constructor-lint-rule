package com.github.grishberg.myapplication.di

import android.content.Context
import com.github.grishberg.myapplication.details.DetailsScreenController
import com.github.grishberg.myapplication.details.DetailsToolbarController
import dagger.Module
import dagger.Provides

@Module
class DetailsScreenModule(
    val context: Context
) {
    @Provides
    fun provideDetailsController(): DetailsScreenController {
        return DetailsScreenController()
    }

    @Provides
    fun provideDetailsToolbarController(): DetailsToolbarController {
        return DetailsToolbarController()
    }
}