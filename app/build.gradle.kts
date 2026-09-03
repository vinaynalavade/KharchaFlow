import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.vinaynalavade.expensetracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vinaynalavade.expensetracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.0.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("signing.properties")
            val userHomeLeafProperties = File(System.getProperty("user.home"), ".android/leaf-signing.properties")
            val userHomeLegacyProperties = File(System.getProperty("user.home"), ".android/kharchaflow-signing.properties")

            val props = Properties()
            if (keystorePropertiesFile.exists()) {
                keystorePropertiesFile.inputStream().use { props.load(it) }
            } else if (userHomeLeafProperties.exists()) {
                userHomeLeafProperties.inputStream().use { props.load(it) }
            } else if (userHomeLegacyProperties.exists()) {
                userHomeLegacyProperties.inputStream().use { props.load(it) }
            }

            val defaultLeafKeystore = File(System.getProperty("user.home"), ".android/leaf-upload-key.jks")
            val defaultLegacyKeystore = File(System.getProperty("user.home"), ".android/kharchaflow-upload-key.jks")

            val storeFilePath = System.getenv("LEAF_KEYSTORE_PATH")
                ?: System.getenv("KHARCHAFLOW_KEYSTORE_PATH")
                ?: props.getProperty("STORE_FILE")
                ?: if (defaultLeafKeystore.exists()) defaultLeafKeystore.absolutePath else if (defaultLegacyKeystore.exists()) defaultLegacyKeystore.absolutePath else null

            val keyAliasVal = System.getenv("LEAF_KEY_ALIAS")
                ?: System.getenv("KHARCHAFLOW_KEY_ALIAS")
                ?: props.getProperty("KEY_ALIAS")
                ?: if (defaultLeafKeystore.exists()) "leaf-upload" else "kharchaflow-upload"

            val storePasswordVal = System.getenv("LEAF_KEYSTORE_PASSWORD")
                ?: System.getenv("KHARCHAFLOW_KEYSTORE_PASSWORD")
                ?: props.getProperty("STORE_PASSWORD")

            val keyPasswordVal = System.getenv("LEAF_KEY_PASSWORD")
                ?: System.getenv("KHARCHAFLOW_KEY_PASSWORD")
                ?: props.getProperty("KEY_PASSWORD")
                ?: storePasswordVal

            val isConfigComplete = !storeFilePath.isNullOrBlank() &&
                !storePasswordVal.isNullOrBlank() &&
                !keyAliasVal.isNullOrBlank() &&
                !keyPasswordVal.isNullOrBlank()

            if (isConfigComplete) {
                val resolvedStoreFile = file(storeFilePath)
                if (!resolvedStoreFile.exists()) {
                    throw org.gradle.api.GradleException(
                        "Leaf release keystore file does not exist at specified path: ${resolvedStoreFile.absolutePath}"
                    )
                }
                storeFile = resolvedStoreFile
                storePassword = storePasswordVal
                keyAlias = keyAliasVal
                keyPassword = keyPasswordVal
            } else {
                val isReleaseBuildRequested = gradle.startParameter.taskNames.any {
                    it.contains("Release", ignoreCase = true)
                }
                if (isReleaseBuildRequested) {
                    val missingKeys = buildList {
                        if (storeFilePath.isNullOrBlank()) add("Keystore path (STORE_FILE / ~/.android/leaf-upload-key.jks)")
                        if (storePasswordVal.isNullOrBlank()) add("STORE_PASSWORD")
                        if (keyAliasVal.isNullOrBlank()) add("KEY_ALIAS")
                        if (keyPasswordVal.isNullOrBlank()) add("KEY_PASSWORD")
                    }
                    throw org.gradle.api.GradleException(
                        "Release signing configuration is incomplete for Leaf release build. Missing: ${missingKeys.joinToString(", ")}. " +
                        "Please configure signing.properties in the project root or ~/.android/leaf-signing.properties."
                    )
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.biometric)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Google Play Services Auth
    implementation(libs.play.services.auth)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore Preferences
    implementation(libs.androidx.datastore.preferences)

    // WorkManager Background Scheduling
    implementation(libs.androidx.work.runtime.ktx)

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit)

    // Debug tooling
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
