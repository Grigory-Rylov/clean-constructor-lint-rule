package com.github.grishberg.myapplication.di

import com.github.grishberg.myapplication.StartController
import dagger.Component

@Component(modules = [ActivityModule::class, AppModule::class, MainScreenModule::class, DetailsScreenModule::class])
interface ApplicationComponent {
    fun startController(): StartController
}