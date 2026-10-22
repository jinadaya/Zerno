package app.zerno.feature.onboarding.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** First-run flow: welcome, language, translation and reminder. */
@Serializable
data object OnboardingRoute : NavKey
