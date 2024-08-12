// YourApp.kt
package com.example.stfapp

import android.app.Application
import com.google.firebase.FirebaseApp

class YourApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
