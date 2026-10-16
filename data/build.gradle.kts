plugins {
    alias(libs.plugins.zerno.sqldelight)
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.entities)
    }
}

sqldelight {
    databases {
        // The user's own data: one file, schema changes go in numbered .sqm migrations.
        create("UserDb") {
            packageName.set("app.zerno.data.db.user")
            srcDirs.setFrom("src/commonMain/sqldelight/user")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/user/databases"))
            verifyMigrations.set(true)
        }
        // One Bible file per translation, schema v1 (backend docs/bible-schema-v1.md). Read-only, never migrated.
        create("BibleDb") {
            packageName.set("app.zerno.data.db.bible")
            srcDirs.setFrom("src/commonMain/sqldelight/bible")
        }
    }
}
