package com.github.grishberg.myapplication

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.ViewGroup
import com.github.grishberg.myapplication.di.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val content = findViewById<ViewGroup>(R.id.content)
        val appComponent = DaggerApplicationComponent.builder()
            .activityModule(ActivityModule(this))
            .appModule(AppModule(this.application))
            .mainScreenModule(MainScreenModule(LayoutInflater.from(this)))
            .detailsScreenModule(DetailsScreenModule(this))
            .build()

        val startController = appComponent.startController()
        val action = if (intent != null && intent.action != null) intent.action else ""
        startController.startedWithAction(action, content)
    }
}
