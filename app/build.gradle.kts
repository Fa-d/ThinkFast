import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    // google-services plugin applied conditionally below
}

android {
    namespace = "dev.sadakat.thinkfaster"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.sadakat.thinkfaster"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Load keystore properties for release signing
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")

                // Enable modern signing schemes
                // V1: Disabled (legacy JAR signing)
                // V2: Enabled (modern, secure, required for Android 7.0+)
                // V3: Enabled (adds key rotation support for Android 9.0+)
                // V4: Disabled (only for streaming installations, not needed)
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    flavorDimensions += "persona"

    productFlavors {
        create("production") {
            dimension = "persona"
            isDefault = true
            buildConfigField("String", "USER_PERSONA", "\"PRODUCTION\"")
        }
        create("freshInstall") {
            dimension = "persona"
            versionNameSuffix = "-freshInstall"
            buildConfigField("String", "USER_PERSONA", "\"FRESH_INSTALL\"")
        }
        create("earlyAdopter") {
            dimension = "persona"
            versionNameSuffix = "-earlyAdopter"
            buildConfigField("String", "USER_PERSONA", "\"EARLY_ADOPTER\"")
        }
        create("transitioningUser") {
            dimension = "persona"
            versionNameSuffix = "-transitioningUser"
            buildConfigField("String", "USER_PERSONA", "\"TRANSITIONING_USER\"")
        }
        create("establishedUser") {
            dimension = "persona"
            versionNameSuffix = "-establishedUser"
            buildConfigField("String", "USER_PERSONA", "\"ESTABLISHED_USER\"")
        }
        create("lockedModeUser") {
            dimension = "persona"
            versionNameSuffix = "-lockedModeUser"
            buildConfigField("String", "USER_PERSONA", "\"LOCKED_MODE_USER\"")
        }
        create("lateNightScroller") {
            dimension = "persona"
            versionNameSuffix = "-lateNightScroller"
            buildConfigField("String", "USER_PERSONA", "\"LATE_NIGHT_SCROLLER\"")
        }
        create("weekendWarrior") {
            dimension = "persona"
            versionNameSuffix = "-weekendWarrior"
            buildConfigField("String", "USER_PERSONA", "\"WEEKEND_WARRIOR\"")
        }
        create("compulsiveReopener") {
            dimension = "persona"
            versionNameSuffix = "-compulsiveReopener"
            buildConfigField("String", "USER_PERSONA", "\"COMPULSIVE_REOPENER\"")
        }
        create("goalSkipper") {
            dimension = "persona"
            versionNameSuffix = "-goalSkipper"
            buildConfigField("String", "USER_PERSONA", "\"GOAL_SKIPPER\"")
        }
        create("overLimitStruggler") {
            dimension = "persona"
            versionNameSuffix = "-overLimitStruggler"
            buildConfigField("String", "USER_PERSONA", "\"OVER_LIMIT_STRUGGLER\"")
        }
        create("streakAchiever") {
            dimension = "persona"
            versionNameSuffix = "-streakAchiever"
            buildConfigField("String", "USER_PERSONA", "\"STREAK_ACHIEVER\"")
        }
        create("realisticMixed") {
            dimension = "persona"
            versionNameSuffix = "-realisticMixed"
            buildConfigField("String", "USER_PERSONA", "\"REALISTIC_MIXED\"")
        }
        create("newUser") {
            dimension = "persona"
            versionNameSuffix = "-newUser"
            buildConfigField("String", "USER_PERSONA", "\"NEW_USER\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }

    lint {
        // Suppress false positive from profileinstaller library
        // ProfileInstallerInitializer is correctly implemented by AndroidX
        disable += "Instantiatable"
    }
}

dependencies {
    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Koin (Dependency Injection)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // ProfileInstaller
    implementation(libs.androidx.profileinstaller)

    // Charts (Vico)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.vico.core)

    // Charts (MPAndroidChart)
    implementation(libs.mpandroidchart)

    // Firebase Analytics
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.crashlytics.ktx)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Test configuration
tasks.withType<Test> {
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Apply google-services plugin only if google-services.json exists
val googleServicesFile = file("google-services.json")
if (googleServicesFile.exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}