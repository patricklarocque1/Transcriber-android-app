package com.example.wristlingo

import android.app.Application
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.WorkManager

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
      override fun onStop(owner: LifecycleOwner) {
        stopService(Intent(this@App, TranslatorService::class.java))
        WorkManager.getInstance(this@App)
          .cancelAllWorkByTag(TranslatorService::class.java.name)
      }
    })
  }
}
