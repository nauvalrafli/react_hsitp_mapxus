package com.mapxushsitp.view.onboarding

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.viewpager2.widget.ViewPager2
import com.mapxushsitp.R
import com.mapxushsitp.service.Preference

class OnboardingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val viewPager: ViewPager2
    private val indicatorContainer: LinearLayout
    private val btnGetStarted: Button

    init {
        LayoutInflater.from(context).inflate(R.layout.onboarding_view, this, true)

        viewPager = findViewById(R.id.viewPager)
        indicatorContainer = findViewById(R.id.indicatorContainer)
        btnGetStarted = findViewById(R.id.btnGetStarted)
    }

    fun setup(items: List<OnboardingPage>) {
        val adapter = OnboardingAdapter(items)
        viewPager.adapter = adapter

        setupIndicators(items.size)
        setIndicator(0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if(position == items.size - 1) {
                    btnGetStarted.isEnabled = true
                }
                setIndicator(position)
            }
        })
    }

    private var finished: (() -> Unit)? = null

    fun setOnFinished(onFinished: () -> Unit = {}) {
        finished = onFinished
    }

    private fun setupIndicators(count: Int) {
        indicatorContainer.removeAllViews()

        repeat(count) {
            val dot = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(16, 16).apply {
                    marginStart = 6
                    marginEnd = 6
                }
                background = context.getDrawable(R.drawable.status_dot)
                backgroundTintList = context.getColorStateList(com.mapxus.map.mapxusmap.R.color.mapxus_gray)
            }
            indicatorContainer.addView(dot)
        }

        btnGetStarted.setOnClickListener {
            findViewById<OnboardingView>(R.id.containerOnboarding).visibility = GONE
            Preference.setOnboardingDone()
            finished?.invoke()
        }
    }

    private fun setIndicator(index: Int) {
        for (i in 0 until indicatorContainer.childCount) {
            val dot = indicatorContainer.getChildAt(i)
            dot.apply {
                if(i == index) {
                    backgroundTintList = context.getColorStateList(R.color.blue)
                } else {
                    backgroundTintList = context.getColorStateList(com.mapxus.map.mapxusmap.R.color.mapxus_gray)
                }
            }
        }
    }
}
