plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.alohaoneui.hook"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 31
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.dexkit)
    implementation(libs.kotlinx.serialization.json)

    compileOnly(libs.libxposed.api)
    compileOnly(project(":stub"))

    testImplementation(libs.junit)
}
