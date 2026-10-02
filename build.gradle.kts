// AGP 9.0+ включает встроенную поддержку Kotlin, поэтому плагин
// org.jetbrains.kotlin.android больше не применяется. По умолчанию AGP тянет
// KGP 2.2.10; поднимаем версию явно, чтобы компилятор читал метаданные
// актуальных AndroidX/Compose-библиотек (Compose BOM 2026.09.00).
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
