// SplashActivity.kt
package com.example.stfapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set content view if you have any UI for the splash screen
        setContentView(R.layout.activity_splash)

        // Delay to show splash screen
        Handler().postDelayed({
            // Start MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            // Finish SplashActivity
            finish()
        }, 2000) // Duration in milliseconds
    }
}
