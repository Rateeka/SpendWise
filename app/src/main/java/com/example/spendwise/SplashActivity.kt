package com.example.spendwise

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DURATION = 3000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.logoImage)
        val ripple1 = findViewById<android.view.View>(R.id.ripple1)
        val ripple2 = findViewById<android.view.View>(R.id.ripple2)
        val ripple3 = findViewById<android.view.View>(R.id.ripple3)

        // Pop-in animation on logo
        val popIn = AnimationUtils.loadAnimation(this, R.anim.logo_pop_in)
        logo.startAnimation(popIn)

        // Ripple animations staggered
        val rippleAnim1 = AnimationUtils.loadAnimation(this, R.anim.ripple_expand)
        val rippleAnim2 = AnimationUtils.loadAnimation(this, R.anim.ripple_expand)
        val rippleAnim3 = AnimationUtils.loadAnimation(this, R.anim.ripple_expand)

        rippleAnim2.startOffset = 200
        rippleAnim3.startOffset = 400

        Handler(Looper.getMainLooper()).postDelayed({
            ripple1.startAnimation(rippleAnim1)
            ripple2.startAnimation(rippleAnim2)
            ripple3.startAnimation(rippleAnim3)
        }, 600)

        // Float animation after pop-in
        val floatAnim = AnimationUtils.loadAnimation(this, R.anim.logo_float)
        Handler(Looper.getMainLooper()).postDelayed({
            logo.startAnimation(floatAnim)
        }, 900)

        // Navigate to MainActivity after splash
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToMain()
        }, SPLASH_DURATION)
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}