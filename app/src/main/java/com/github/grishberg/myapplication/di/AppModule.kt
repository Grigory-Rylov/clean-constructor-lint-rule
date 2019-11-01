package com.github.grishberg.myapplication.di

import android.app.Application
import com.github.grishberg.myapplication.StartController
import com.github.grishberg.myapplication.details.DetailsScreenController
import com.github.grishberg.myapplication.main.MainScreenController
import dagger.Module
import dagger.Provides
import javax.inject.Provider
import javax.inject.Singleton

@Module
class AppModule(val mApplication: Application) {

    @Provides
    @Singleton
    internal fun providesApplication(): Application {
        return mApplication
    }

    @Provides
    internal fun provideStartController(
        mainScreenController: Provider<MainScreenController>,
        detailsController: Provider<DetailsScreenController>
    ): StartController {
        return StartController(mainScreenController, detailsController)
    }

}
