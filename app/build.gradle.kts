@file:Suppress("UnstableApiUsage")

import java.awt.AlphaComposite
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt
import java.util.Locale

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
val defaultManagerName = "Aloha OneUI"
val managerPackageName = project.findProperty("ONEUIALOHA_PACKAGE_NAME")?.toString() ?: defaultManagerPackageName
val managerName = project.findProperty("ONEUIALOHA_NAME")?.toString() ?: defaultManagerName

// Keep the user's source image unchanged in the repository. Generate only the
// launcher-specific foreground: fit the complete artwork inside Android's 66dp safe
// zone on a transparent 108dp canvas so OEM masks (including Samsung One UI) don't clip it.
val launcherIconSource = rootProject.file("Aloha_OneUI_icon.png")
val generatedLauncherResDir = layout.buildDirectory.dir("generated/res/alohaLauncher")
val adaptiveIconCanvasPx = 432 // 108dp at 4x density
val adaptiveIconSafeZonePx = 264 // 66dp at 4x density

fun sampleAlohaIconBackground(image: BufferedImage): String {
    val buckets = mutableMapOf<Int, MutableList<Int>>()

    fun recordPixel(x: Int, y: Int) {
        val argb = image.getRGB(x, y)
        val alpha = (argb ushr 24) and 0xFF
        if (alpha < 240) return
        val red = (argb ushr 16) and 0xFF
        val green = (argb ushr 8) and 0xFF
        val blue = argb and 0xFF
        // Group near-identical edge colors, then average the dominant group.
        val bucket = ((red ushr 4) shl 8) or ((green ushr 4) shl 4) or (blue ushr 4)
        buckets.getOrPut(bucket) { mutableListOf() }.add(argb)
    }

    val horizontalBand = maxOf(1, image.width / 64)
    val verticalBand = maxOf(1, image.height / 64)
    for (x in 0 until image.width) {
        for (offset in 0 until minOf(verticalBand, image.height)) {
            recordPixel(x, offset)
            recordPixel(x, image.height - 1 - offset)
        }
    }
    for (y in 0 until image.height) {
        for (offset in 0 until minOf(horizontalBand, image.width)) {
            recordPixel(offset, y)
            recordPixel(image.width - 1 - offset, y)
        }
    }

    val dominant = buckets.values.maxByOrNull { it.size } ?: return "#00000000"
    val red = dominant.sumOf { (it ushr 16) and 0xFF } / dominant.size
    val green = dominant.sumOf { (it ushr 8) and 0xFF } / dominant.size
    val blue = dominant.sumOf { it and 0xFF } / dominant.size
    return String.format(Locale.ROOT, "#%02X%02X%02X", red, green, blue)
}

val prepareAlohaLauncherIcon = tasks.register("prepareAlohaLauncherIcon") {
    inputs.file(launcherIconSource)
    outputs.dir(generatedLauncherResDir)
    doLast {
        check(launcherIconSource.isFile) {
            "Required launcher icon source is missing: ${launcherIconSource.path}"
        }
        val sourceImage = ImageIO.read(launcherIconSource)
            ?: error("Unable to decode Aloha_OneUI_icon.png")
        check(sourceImage.width > 0 && sourceImage.height > 0) {
            "Aloha launcher source image has invalid dimensions."
        }

        val outputRoot = generatedLauncherResDir.get().asFile
        outputRoot.deleteRecursively()

        // The uploaded original is read-only; only this derived resource is transformed.
        // Scale the whole source proportionally, never trim/crop it, and leave transparent
        // pixels around it. 264/432 == 66/108, the official adaptive-icon safe zone.
        val scale = minOf(
            1.0,
            minOf(
                adaptiveIconSafeZonePx.toDouble() / sourceImage.width,
                adaptiveIconSafeZonePx.toDouble() / sourceImage.height
            )
        )
        val drawWidth = (sourceImage.width * scale).roundToInt().coerceAtLeast(1)
        val drawHeight = (sourceImage.height * scale).roundToInt().coerceAtLeast(1)
        val canvas = BufferedImage(
            adaptiveIconCanvasPx,
            adaptiveIconCanvasPx,
            BufferedImage.TYPE_INT_ARGB
        )
        val graphics = canvas.createGraphics()
        try {
            graphics.composite = AlphaComposite.Src
            graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
            )
            graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
            )
            graphics.setRenderingHint(
                RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
            )
            graphics.drawImage(
                sourceImage,
                (adaptiveIconCanvasPx - drawWidth) / 2,
                (adaptiveIconCanvasPx - drawHeight) / 2,
                drawWidth,
                drawHeight,
                null
            )
        } finally {
            graphics.dispose()
        }

        val foregroundIcon = File(
            outputRoot,
            "mipmap-xxxhdpi/ic_launcher_aloha_foreground.png"
        )
        foregroundIcon.parentFile.mkdirs()
        check(ImageIO.write(canvas, "png", foregroundIcon)) {
            "Unable to write generated adaptive launcher foreground."
        }

        val background = sampleAlohaIconBackground(sourceImage)
        val colorsXml = File(outputRoot, "values/aloha_launcher_colors.xml")
        colorsXml.parentFile.mkdirs()
        colorsXml.writeText(
            """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="aloha_launcher_background">$background</color>
</resources>
""".trimIndent() + "\n",
            Charsets.UTF_8
        )

        println(
            "Generated Aloha adaptive icon: full source fitted without cropping into " +
                "${adaptiveIconSafeZonePx}x${adaptiveIconSafeZonePx} safe zone on " +
                "${adaptiveIconCanvasPx}x${adaptiveIconCanvasPx} transparent canvas; " +
                "source file was not modified; background=$background"
        )
        sourceImage.flush()
        canvas.flush()
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

// Register the generated resources and make every Android resource-processing task depend on their generation.
android.sourceSets.getByName("main").res.srcDir(generatedLauncherResDir.get().asFile)
tasks.configureEach {
    if (name.contains("Resources") || name.endsWith("SourceSetPaths") || name.contains("SupportedLocales") || name.contains("DeepLinks")) {
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
