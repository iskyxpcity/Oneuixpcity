@file:Suppress("UnstableApiUsage")

import java.io.File
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.roundToInt

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
val defaultManagerPackageName = "com.oneuialoha.app"
val defaultManagerName = "OneUI Aloha"
val managerPackageName = project.findProperty("ONEUIALOHA_PACKAGE_NAME")?.toString() ?: defaultManagerPackageName
val managerName = project.findProperty("ONEUIALOHA_NAME")?.toString() ?: defaultManagerName

val launcherIconSource = rootProject.file("Aloha_OneUI_icon.png")
val generatedLauncherResDir = layout.buildDirectory.dir("generated/res/alohaLauncher")

// Build the launcher icon directly from the owner-supplied root PNG.
// The generated artwork is exactly 256x256 pixels; transparency is preserved
// and non-square inputs are contained without stretching or cropping.
val prepareAlohaLauncherIcon = tasks.register("prepareAlohaLauncherIcon") {
    inputs.file(launcherIconSource)
    outputs.dir(generatedLauncherResDir)
    doLast {
        check(launcherIconSource.isFile) {
            "Required launcher icon source is missing: ${launcherIconSource.path}"
        }
        val sourceImage = ImageIO.read(launcherIconSource)
            ?: throw GradleException("Cannot decode Aloha_OneUI_icon.png as an image.")
        val size = 256
        val outputImage = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val graphics = outputImage.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val scale = minOf(size.toDouble() / sourceImage.width, size.toDouble() / sourceImage.height)
            val drawWidth = (sourceImage.width * scale).roundToInt().coerceIn(1, size)
            val drawHeight = (sourceImage.height * scale).roundToInt().coerceIn(1, size)
            graphics.drawImage(
                sourceImage,
                (size - drawWidth) / 2,
                (size - drawHeight) / 2,
                drawWidth,
                drawHeight,
                null,
            )
        } finally {
            graphics.dispose()
        }

        val outputRoot = generatedLauncherResDir.get().asFile
        outputRoot.deleteRecursively()
        val legacyIcon = File(outputRoot, "mipmap-nodpi/ic_launcher_aloha.png")
        val adaptiveForeground = File(outputRoot, "drawable-nodpi/ic_launcher_aloha_foreground.png")
        legacyIcon.parentFile.mkdirs()
        adaptiveForeground.parentFile.mkdirs()
        check(ImageIO.write(outputImage, "png", legacyIcon)) {
            "No PNG writer is available for the legacy launcher icon."
        }
        check(ImageIO.write(outputImage, "png", adaptiveForeground)) {
            "No PNG writer is available for the adaptive launcher foreground."
        }
        File(outputRoot, "mipmap-anydpi-v26/ic_launcher_aloha.xml").apply {
            parentFile.mkdirs()
            writeText(
                """
                <?xml version="1.0" encoding="utf-8"?>
                <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
                    <background android:drawable="@android:color/transparent" />
                    <foreground android:drawable="@drawable/ic_launcher_aloha_foreground" />
                </adaptive-icon>
                """.trimIndent() + "\n"
            )
        }
        println("Prepared Aloha launcher icon: ${size}x${size} px from ${launcherIconSource.name}")
    }
}


android {
    namespace = "com.oneuialoha.app"

    // The same owner keystore must sign EVERY distributable APK.
    // CI supplies credentials via GitHub Actions secrets; never commit keys.
    val ownerKeystore = System.getenv("ONEUIALOHA_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
    val ownerStorePassword = System.getenv("ONEUIALOHA_KEYSTORE_PASSWORD")
    val ownerKeyAlias = System.getenv("ONEUIALOHA_KEY_ALIAS")
    val ownerKeyPassword = System.getenv("ONEUIALOHA_KEY_PASSWORD")
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
        // Keep the app's non-default resource package ID to avoid 0x7f collisions.
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

// Keep the generated icon in the Android resource merge for both debug and release builds.
android.sourceSets.getByName("main").res.srcDir(generatedLauncherResDir)
tasks.configureEach {
    if (name.startsWith("merge") && name.endsWith("Resources")) {
        dependsOn(prepareAlohaLauncherIcon)
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
