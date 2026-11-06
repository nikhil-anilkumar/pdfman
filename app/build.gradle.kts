import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
}

// Release signing is read from keystore.properties (not checked in). Without it, release builds are unsigned.
val keystoreProps = Properties().apply {
  rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
  namespace = "com.pdfman.app"
  compileSdk = 37
  defaultConfig {
    applicationId = "com.pdfman.app"
    minSdk = 28
    targetSdk = 37
    versionCode = 1
    versionName = "1.0.0"

    // Shown on the About screen so the credits always match what ships.
    buildConfigField("String", "COMPOSE_BOM_VERSION", "\"${libs.versions.androidxComposeBom.get()}\"")
    buildConfigField("String", "CORE_VERSION", "\"${libs.versions.androidxCore.get()}\"")
    buildConfigField("String", "ACTIVITY_VERSION", "\"${libs.versions.androidxActivity.get()}\"")
    buildConfigField("String", "LIFECYCLE_VERSION", "\"${libs.versions.androidxLifecycle.get()}\"")
    buildConfigField("String", "KOTLIN_VERSION", "\"${libs.versions.kotlin.get()}\"")
    buildConfigField("String", "COROUTINES_VERSION", "\"${libs.versions.coroutines.get()}\"")
  }

  signingConfigs {
    if (keystoreProps.isNotEmpty()) {
      create("release") {
        storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
        storePassword = keystoreProps.getProperty("storePassword")
        keyAlias = keystoreProps.getProperty("keyAlias")
        keyPassword = keystoreProps.getProperty("keyPassword")
      }
    }
  }

  buildTypes {
    debug {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-debug"
    }
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.findByName("release")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    aidl = false
    buildConfig = true
    shaders = false
  }

  // Leave out the dependency metadata block, which is encrypted with a Google key.
  // F-Droid flags it, and it isn't needed outside Google Play.
  dependenciesInfo {
    includeInApk = false
    includeInBundle = false
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.kotlinx.coroutines.android)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
