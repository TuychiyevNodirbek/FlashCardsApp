plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    jvm()

    // iosX64 (Intel-симулятор) не таргетируем: Compose Multiplatform 1.11.x
    // больше не публикует под него артефакты (задепрекейтили вместе с macosX64).
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.jetbrains.navigation.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.datetime)
            implementation(libs.haze)
            implementation(libs.haze.materials)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.androidx.room.runtime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.compose.ui.text.google.fonts)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

compose.resources {
    packageOfResClass = "uz.nodirbek.flashcardsapp.composeapp.generated.resources"
}

configurations.all {
    resolutionStrategy {
        // Compose Multiplatform Material3 (внутренний DatePicker) и/или haze тянут
        // kotlinx-datetime:0.7.1 транзитивно, у которого Clock/Instant — typealias
        // на kotlin.time.Clock/Instant из Kotlin 2.3+ stdlib (см. changelog
        // kotlinx-datetime 0.7.0/0.7.1). На пином Kotlin 2.2.21 эти типы не
        // резолвятся ("Unresolved reference 'System'") — фиксируем 0.6.1, на
        // которой написан весь текущий код (Clock.System из самой kotlinx-datetime).
        force("org.jetbrains.kotlinx:kotlinx-datetime:${libs.versions.kotlinxDatetime.get()}")
    }
}

android {
    namespace = "uz.nodirbek.flashcardsapp.composeapp"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
