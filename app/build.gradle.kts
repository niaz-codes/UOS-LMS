import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // Makes the Kotlin compiler aware of Lombok-generated accessors (@Getter/@Builder/etc.)
    // on the Java domain models - without this, Kotlin sees only the private fields Lombok
    // is annotating and every Kotlin call site reading a Java model's fields fails to compile.
    alias(libs.plugins.kotlin.lombok)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.navigation.safeargs)
}

// The google-services plugin hard-fails project configuration (not just its own task) if
// app/google-services.json is missing, which would break every build for anyone who hasn't
// set up a Firebase project yet (push notifications are optional - see feature/notifications/fcm).
// Applying it conditionally means the app keeps building/running without the file; FCM push
// simply won't register (guarded at the one call site that fetches a token) until it's added.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Cloudinary credentials live in local.properties (gitignored), never in source
// control. See local.properties for where to get these values.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
// Soft-fails (empty string + a build-log warning) rather than crashing configuration
// when a key is missing, so a fresh clone with no local.properties still builds - the
// Cloudinary upload feature just won't work until real keys are added, same tradeoff
// already made below for google-services.json/FCM.
fun localProperty(key: String): String {
    val value = localProperties.getProperty(key) ?: System.getenv(key) ?: ""
    if (value.isBlank()) {
        println("WARNING: '$key' is not set in local.properties - Cloudinary uploads will fail until it is.")
    }
    return value
}
fun localPropertyOrDefault(key: String, default: String): String =
    localProperties.getProperty(key) ?: System.getenv(key) ?: default

// AGP 9's built-in Kotlin support otherwise defaults to a JDK 21 toolchain
// and tries to auto-download it, which this environment's network can't
// complete. Pin it to the JDK that's actually installed locally instead.
kotlin {
    jvmToolchain(24)
}

android {
    namespace = "com.example.uos_lms"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.uos_lms"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${localProperty("CLOUDINARY_CLOUD_NAME")}\"")
        buildConfigField("String", "CLOUDINARY_API_KEY", "\"${localProperty("CLOUDINARY_API_KEY")}\"")
        buildConfigField("String", "CLOUDINARY_API_SECRET", "\"${localProperty("CLOUDINARY_API_SECRET")}\"")

        // Backend API base URL - defaults to the deployed production backend (Railway), so a
        // fresh clone works against real data with zero setup. Override in local.properties
        // (API_BASE_URL) for local dev: http://10.0.2.2:4000/api/ for the emulator's loopback
        // to the host machine, or your machine's LAN IP for real-device testing.
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"${localPropertyOrDefault("API_BASE_URL", "https://uos-lms-production.up.railway.app/api/")}\""
        )
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.hilt.android)
    // ksp alone, deliberately NOT also annotationProcessor(hilt.android.compiler): with both
    // configured, ksp's and javac's Hilt processors each independently (re)generate the same
    // "hilt_aggregated_deps" proxy for every Hilt-annotated class they can see on the shared
    // classpath, crashing with a duplicate-file error. ksp's pass alone already covers the
    // Java classes too (confirmed via a full assembleDebug: hiltAggregateDepsDebug/
    // hiltJavaCompileDebug/transformDebugClassesWithAsm all succeed for Java @AndroidEntryPoint
    // classes like MainActivity/LoginFragment). kapt would be the conventional fix for mixed
    // Kotlin/Java Hilt modules instead, but it's incompatible with AGP 9's built-in Kotlin.
    ksp(libs.hilt.android.compiler)

    // --- Java + XML conversion additions ---
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.google.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.image.cropper)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Direct REST calls to Cloudinary's upload/destroy API (signed uploads,
    // multipart with progress tracking) — no dedicated Cloudinary SDK needed.
    implementation(libs.okhttp)

    // Backend REST API (Node/Express/MongoDB migration)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    // JWT storage (auth token) - EncryptedSharedPreferences
    implementation(libs.androidx.security.crypto)
    // Task/TaskCompletionSource, used app-wide as the async return type. Used to come in
    // transitively via firebase-auth/firebase-firestore; now the only source of it since
    // Firebase was removed entirely (final cutover).
    implementation(libs.play.services.tasks)

    // Push transport only (see feature/notifications/fcm) - no Firebase Auth/Firestore.
    // Compiles and runs fine without google-services.json; FCM registration itself just stays
    // inert until that file is present (see the conditional plugin application above).
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
