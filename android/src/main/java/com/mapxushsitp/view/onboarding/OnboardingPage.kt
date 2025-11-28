package com.mapxushsitp.view.onboarding

data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val description: String,
    val imageRes: Int,
    val isGif: Boolean = false
)

