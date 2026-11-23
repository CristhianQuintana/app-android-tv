plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.cotasperu.tv"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.cotasperu.tv"
        minSdk = 27
        targetSdk = 37
        versionCode = 44
        versionName = "0.3.39"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Producción pública (informe 2026-11-04). Override opcional: -PCOTAS_API_BASE_URL=...
    val publicApi = "https://api.cotasperu.com/api/v1"
    val apiOverride = (findProperty("COTAS_API_BASE_URL") as String?)?.trim().orEmpty()
    val debugApi = when {
        apiOverride.isNotEmpty() -> apiOverride
        else -> publicApi
    }
    val releaseApi = when {
        apiOverride.startsWith("https://") -> apiOverride
        else -> publicApi
    }
    val debugAllowLan = debugApi.startsWith("http://")
    val debugLanOrigin = if (debugAllowLan) debugApi else ""

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$debugApi\"")
            buildConfigField("String", "LAN_HTTP_ORIGIN", "\"$debugLanOrigin\"")
            buildConfigField("boolean", "ALLOW_LAN_HTTP", "$debugAllowLan")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"$releaseApi\"")
            buildConfigField("String", "LAN_HTTP_ORIGIN", "\"\"")
            buildConfigField("boolean", "ALLOW_LAN_HTTP", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.tv.material)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.moshi)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.decoder)
    implementation(libs.androidx.media3.ui)
    // FFmpeg soft-decode (MP2/AC3/E-AC3): canales IPTV usan audio/mpeg-L2 no soportado por MediaCodec.
    implementation(files("libs/media3-decoder-ffmpeg.aar"))
    implementation(libs.coil.compose)
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
