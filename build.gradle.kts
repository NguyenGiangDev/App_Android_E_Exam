plugins {
    alias(libs.plugins.android.application) apply false
}

buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.1") // Phiên bản Gradle Plugin
        classpath("com.google.gms:google-services:4.3.15") // Plugin Google Services
    }
}
