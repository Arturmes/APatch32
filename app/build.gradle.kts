import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import org.jetbrains.kotlin.de.undercouch.gradle.tasks.download.Download

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.ksp)
    alias(libs.plugins.lsplugin.apksign)
    id("kotlin-parcelize")
}

val managerVersionCode: Int by rootProject.extra
val managerVersionName: String by rootProject.extra
val kernelPatchVersion: String by rootProject.extra

apksign {
    storeFileProperty = "KEYSTORE_FILE"
    storePasswordProperty = "KEYSTORE_PASSWORD"
    keyAliasProperty = "KEY_ALIAS"
    keyPasswordProperty = "KEY_PASSWORD"
}

android {
    namespace = "me.bmax.apatch"

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            multiDexEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }


    buildFeatures {
        aidl = true
        buildConfig = true
        compose = true
    }

    defaultConfig {
        buildConfigField("String", "buildKPV", "\"$kernelPatchVersion\"")
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.androidx.compose.compiler.get().version
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/**.version"
            excludes += "okhttp3/**"
            excludes += "kotlin/**"
            excludes += "/org/bouncycastle/**"
            excludes += "org/**"
            excludes += "**.properties"
            excludes += "**.bin"
            excludes += "kotlin-tooling-metadata.json"
        }
    }

    androidResources {
        generateLocaleConfig = true
    }

    sourceSets["main"].jniLibs.srcDir("libs")

    applicationVariants.all {
        outputs.forEach {
            val output = it as BaseVariantOutputImpl
            output.outputFileName = "APatch_${managerVersionName}_${managerVersionCode}-$name.apk"
        }

        kotlin.sourceSets {
            getByName(name) {
                kotlin.srcDir("build/generated/ksp/$name/kotlin")
            }
        }
    }
}

// https://github.com/bbqsrc/cargo-ndk
// cross clippy --manifest-path apd/Cargo.toml --target armv7-linux-androideabi --release
tasks.register<Exec>("clippyBuild") {
    executable("cross")
    args("clippy", "--manifest-path", "Cargo.toml", "--target", "armv7-linux-androideabi", "--release")
    workingDir("${project.rootDir}/apd")
}

tasks.register<Copy>("buildApd") {
    dependsOn("clippyBuild")
    from("${project.rootDir}/apd/target/armv7-linux-androideabi/release/apd")
    into("${project.projectDir}/libs/armeabi-v7a")
    rename("apd", "libapd.so")
}

tasks.configureEach {
    if (name == "mergeDebugJniLibFolders" || name == "mergeReleaseJniLibFolders") {
        dependsOn("buildApd")
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.webkit)
    implementation(libs.timber)
    implementation(libs.devappx)
    implementation(libs.ini4j)
    implementation(libs.bcpkix)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.com.google.accompanist.drawablepainter)
    implementation(libs.com.google.accompanist.navigation.animation)
    implementation(libs.com.google.accompanist.systemuicontroller)

    implementation(libs.compose.destinations.animations.core)
    ksp(libs.compose.destinations.ksp)

    implementation(libs.com.github.topjohnwu.libsu.core)
    implementation(libs.com.github.topjohnwu.libsu.service)
    implementation(libs.com.github.topjohnwu.libsu.nio)
    implementation(libs.com.github.topjohnwu.libsu.io)

    implementation(libs.dev.rikka.rikkax.parcelablelist)

    implementation(libs.io.coil.kt.coil.compose)

    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.me.zhanghai.android.appiconloader.coil)

    implementation(libs.sheet.compose.dialogs.core)
    implementation(libs.sheet.compose.dialogs.list)
    implementation(libs.sheet.compose.dialogs.input)

    implementation(libs.markdown)
    implementation(libs.com.google.accompanist.webview)
}
