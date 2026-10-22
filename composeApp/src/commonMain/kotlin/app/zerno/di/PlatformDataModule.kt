package app.zerno.di

import org.koin.core.module.Module

/** `UserDb`, `TranslationStore` and `AppPreferences`, one each per app, built on the platform's own storage. */
expect val platformDataModule: Module
