@file:Suppress("UnstableApiUsage")

import java.io.File

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
}

val androidCompileSdkVersion = rootProject.extra["androidCompileSdkVersion"] as Int
val androidCompileSdkVersionMinor = rootProject.extra["androidCompileSdkVersionMinor"] as Int
val androidBuildToolsVersion = rootProject.extra["androidBuildToolsVersion"] as String
val androidMinSdkVersion = rootProject.extra["androidMinSdkVersion"] as Int
val androidTargetSdkVersion = rootProject.extra["androidTargetSdkVersion"] as Int
val androidSourceCompatibility = rootProject.extra["androidSourceCompatibility"] as JavaVersion
val androidTargetCompatibility = rootProject.extra["androidTargetCompatibility"] as JavaVersion
val managerVersionCode = rootProject.extra["managerVersionCode"] as Int
val managerVersionName = rootProject.extra["managerVersionName"] as String

val isPrBuild = project.findProperty("IS_PR_BUILD")?.toString()?.toBoolean() ?: false
val defaultManagerPackageName = "com.oneuixp.app"
val defaultManagerName = "OneUI XP"
val managerPackageName = project.findProperty("ONEUIXP_PACKAGE_NAME")?.toString() ?: defaultManagerPackageName
val managerName = project.findProperty("ONEUIXP_NAME")?.toString() ?: defaultManagerName


android {
    namespace = "com.oneuixp.app"

    // The same owner keystore must sign EVERY distributable APK.
    // CI supplies credentials via GitHub Actions secrets; never commit keys.
    val ownerKeystore = System.getenv("ONEUIXP_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
    val ownerStorePassword = System.getenv("ONEUIXP_KEYSTORE_PASSWORD")
    val ownerKeyAlias = System.getenv("ONEUIXP_KEY_ALIAS")
    val ownerKeyPassword = System.getenv("ONEUIXP_KEY_PASSWORD")
    val ownerSigningReady = !ownerKeystore.isNullOrBlank() &&
        !ownerStorePassword.isNullOrBlank() &&
        !ownerKeyAlias.isNullOrBlank() &&
        !ownerKeyPassword.isNullOrBlank()

    signingConfigs {
        if (ownerSigningReady) {
            create("owner") {
                storeFile = File(ownerKeystore!!)
                storePassword = ownerStorePassword!!
                keyAlias = ownerKeyAlias!!
                keyPassword = ownerKeyPassword!!
            }
        }
    }

    buildTypes {
        debug {
            if (ownerSigningReady) signingConfig = signingConfigs.getByName("owner")
        }
        release {
            if (ownerSigningReady) signingConfig = signingConfigs.getByName("owner")
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        buildConfig = true
        resValues = true
        compose = true
    }

    packaging {
        dex {
            useLegacyPackaging = true
        }
        jniLibs {
            useLegacyPackaging = true
            excludes += "lib/*/libandroidx.graphics.path.so"
        }
    }



    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    androidResources {
        generateLocaleConfig = true

        // Hook resources are loaded into SystemUI / Samsung host processes.
        // Keep OneUIX's non-default resource package ID to avoid 0x7f collisions.
        additionalParameters += listOf(
            "--allow-reserved-package-id",
            "--package-id",
            "0x55",
        )
    }
    compileSdk {
        version =
            release(androidCompileSdkVersion) {
                minorApiLevel = androidCompileSdkVersionMinor
            }
    }
    buildToolsVersion = androidBuildToolsVersion

    defaultConfig {
        minSdk = androidMinSdkVersion
        targetSdk = androidTargetSdkVersion
        versionCode = managerVersionCode
        versionName = managerVersionName
        applicationId = managerPackageName

        buildConfigField("boolean", "IS_PR_BUILD", isPrBuild.toString())
        resValue("string", "app_name", managerName)



        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = androidSourceCompatibility
        targetCompatibility = androidTargetCompatibility
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) {
        // Modern modules require META-INF/xposed/{java_init.list,module.prop,scope.list} even in release.
        it.packaging.resources.excludes.addAll(listOf("META-INF/*.kotlin_module", "kotlin/**", "**.bin"))
    }
}

base {
    archivesName.set(
        "${managerName.replace(" ", "_")}_${managerVersionName}_${managerVersionCode}"
    )
}

dependencies {
    implementation(project(":common"))
    implementation(project(":hook"))

    compileOnly("de.robv.android.xposed:api:82")
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.navigationevent.compose)



    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.commonmark)
    implementation(libs.commonmark.ext.gfm.tables)
    implementation(libs.commonmark.ext.gfm.strikethrough)
    implementation(libs.commonmark.ext.autolink)
    implementation(libs.commonmark.ext.task.list.items)

    implementation(libs.androidx.webkit)

    implementation(libs.lsposed.cxx)

    implementation(libs.hiddenapibypass)

    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.nav)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.blur)

    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)

    implementation(libs.material.kolor)

    implementation(libs.appiconloader)

}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }
}
