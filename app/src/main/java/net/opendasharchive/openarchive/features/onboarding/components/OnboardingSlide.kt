package net.opendasharchive.openarchive.features.onboarding.components

// Data class for onboarding slides
data class OnboardingSlide(
    val titleRes: Int,
    val textRes: Int,
    val linkRes: Int? = null,
    val imageRes: Int
)