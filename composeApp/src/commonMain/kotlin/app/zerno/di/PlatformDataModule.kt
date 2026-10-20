package app.zerno.di

import org.koin.core.module.Module

/** `UserDb` and `TranslationStore`, one each per app, built on the platform's own directories. */
expect val platformDataModule: Module
