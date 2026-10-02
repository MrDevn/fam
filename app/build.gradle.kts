plugins {
    alias(libs.plugins.android.application)
    // AGP 9 компилирует Kotlin сам (built-in Kotlin), отдельный плагин
    // org.jetbrains.kotlin.android не нужен. Compose-компилятор подключается отдельно.
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.fam.aware"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fam.aware"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    // Подпись релиза подключается только если заданы переменные окружения.
    // Секреты в репозиторий не попадают: без них release собирается неподписанным.
    val signingStorePath = providers.environmentVariable("FAM_KEYSTORE_PATH").orNull
    val signingStorePassword = providers.environmentVariable("FAM_KEYSTORE_PASSWORD").orNull
    val signingKeyAlias = providers.environmentVariable("FAM_KEY_ALIAS").orNull
    val signingKeyPassword = providers.environmentVariable("FAM_KEY_PASSWORD").orNull
    val canSignRelease = signingStorePath != null &&
        signingStorePassword != null &&
        signingKeyAlias != null &&
        signingKeyPassword != null

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = file(signingStorePath!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (canSignRelease) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        abortOnError = false
        warningsAsErrors = false
        checkReleaseBuilds = false
    }
}

// Kotlin jvmTarget при встроенной поддержке Kotlin в AGP 9 берётся из
// compileOptions.targetCompatibility выше — отдельный блок kotlin { } не нужен.

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
