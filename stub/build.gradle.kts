plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.alohaoneui.stub"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 31
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
