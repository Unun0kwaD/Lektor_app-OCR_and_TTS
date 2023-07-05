@file:Suppress("DEPRECATION")

package com.example.cameraxapp

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage
import androidx.core.animation.doOnEnd


@ExperimentalGetImage class Animation : AppCompatActivity() {
    private lateinit var mSceneView: View
    private lateinit var mPizzaView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_animation)
        mSceneView = findViewById(R.id.scene)
        mPizzaView= mSceneView.findViewById(R.id.pizza)

         startAnimation()

    }

    private fun startAnimation() {
        val pizzaYStart = mSceneView.paddingTop.toFloat()
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val pizzaYEnd =  height.toFloat()/2
        val pizzaRotationStart = mPizzaView.rotation
        val pizzaRotationEnd = mPizzaView.rotation + 360f

        println("pizzaYStart: $pizzaYStart")
        println("pizzaYEnd: $pizzaYEnd")

        val heightAnimator = ObjectAnimator
            .ofFloat(mPizzaView, "y", pizzaYStart, pizzaYEnd)
            .setDuration(800)

        val rotationAnimator = ObjectAnimator
            .ofFloat(mPizzaView, "rotation", pizzaRotationStart, pizzaRotationEnd)
            .setDuration(1200)

        heightAnimator.interpolator = AccelerateInterpolator()

        heightAnimator.start()
        val animatorSet = AnimatorSet()
        animatorSet
            .play(heightAnimator)
            .with(rotationAnimator)
        animatorSet.start()
        animatorSet.doOnEnd {
            val intent = Intent(this, MainMenuActivity::class.java)
            startActivity(intent) }



    }
}