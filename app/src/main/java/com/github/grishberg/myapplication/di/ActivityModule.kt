package com.github.grishberg.myapplication.di

import android.content.Context
import dagger.Module
import dagger.Provides

@Module
class ActivityModule(
    val context: Context
) {
    @Provides
    fun provideContext(): Context = context
}