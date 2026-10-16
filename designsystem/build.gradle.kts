plugins {
    alias(libs.plugins.zerno.kmp.compose)
    alias(libs.plugins.zerno.sj.tokens)
}

compose.resources {
    // Fonts and mascots are reached through the design system API, not through Res directly.
    publicResClass = false
    packageOfResClass = "app.zerno.designsystem.resources"
}
