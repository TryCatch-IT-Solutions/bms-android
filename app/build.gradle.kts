plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.greenrobot.greendao")
}

android {
    namespace = "com.example.bms"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dai.bms"
        minSdk = 30
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    greendao {
        schemaVersion = 1
        daoPackage = "com.hf.facex.greendao.gen"
        targetGenDir = file("src/main/java")
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/spring.tooling"
            excludes += "/META-INF/spring.handlers"
            excludes += "/META-INF/spring.schemas"
            excludes += "/META-INF/license.txt"
            excludes += "/META-INF/notice.txt"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.material.v190)
    implementation(libs.gson)
    implementation("com.github.f0ris.sweetalert:library:1.5.6")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    implementation("org.springframework.security:spring-security-core:5.8.0")
    implementation("org.springframework.security:spring-security-crypto:5.8.0")
    implementation("org.greenrobot:greendao:3.3.0")

    //permission
    implementation("com.github.getActivity:XXPermissions:20.0")
//db
    implementation("org.greenrobot:greendao:3.3.0")
    implementation("io.github.yuweiguocn:GreenDaoUpgradeHelper:v2.2.1")

    implementation("com.github.li-xiaojun:XPopup:2.10.0")

    implementation("androidx.recyclerview:recyclerview:1.1.0")

    implementation("androidx.activity:activity-ktx:1.6.1")
    implementation("androidx.fragment:fragment-ktx:1.5.5")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.airbnb.android:lottie:6.6.2")

    api(files("libs/fingerprintv3.aar"))
    api(files("libs/facepass.aar"))

}