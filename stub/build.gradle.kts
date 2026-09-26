plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.soclear.oneuix.stub"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 33
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
