package com.github.grishberg.myapplication.di

import android.content.Context
import android.view.LayoutInflater
import com.github.grishberg.myapplication.main.CardsController
import com.github.grishberg.myapplication.main.MainButtonsController
import com.github.grishberg.myapplication.main.MainScreenController
import dagger.Module
import dagger.Provides
import javax.inject.Provider

@Module
class MainScreenModule(
    private val inflater: LayoutInflater
) {
    @Provides
    fun provideMainScreenController(
        context: Provider<Context>,
        buttonsProvider: Provider<MainButtonsController>,
        cardsProvider: Provider<CardsController>
    ): MainScreenController {
        return MainScreenController(context.get(), buttonsProvider.get(), cardsProvider.get())
    }

    @Provides
    fun provideMainButtonsController(): MainButtonsController {
        return MainButtonsController(inflater)
    }

    @Provides
    fun provideCardsController(): CardsController {
        return CardsController(inflater)
    }
}