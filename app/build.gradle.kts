import java.util.Properties

plugins {
    id("com.android.application")
}

val credentialsFile = rootProject.file("credentials.properties")
val credentialsProps = Properties()
if (credentialsFile.exists()) {
    credentialsFile.inputStream().use { credentialsProps.load(it) }
}
// Credentials are read from the git-ignored credentials.properties (see credentials.properties.example).
// No key is stored in source control; without one the app runs on cached/sample data and the key can be entered in-app.
val clientId = credentialsProps.getProperty("DATEX_CLIENT_ID", "")
val clientKey = credentialsProps.getProperty("DATEX_CLIENT_KEY", "")

android {
    namespace = "org.me.gcu.trafficscotland_jordan"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.me.gcu.trafficscotland_jordan"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "DATEX_CLIENT_ID", "\"$clientId\"")
        buildConfigField("String", "DATEX_CLIENT_KEY", "\"$clientKey\"")
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
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")

    implementation("org.osmdroid:osmdroid-android:6.1.18")

    testImplementation("junit:junit:4.13.2")
    // android.jar only contains stubs of XmlPullParser, so unit tests on the JVM need a real implementation
    testImplementation("net.sf.kxml:kxml2:2.3.0")
    testImplementation("xmlpull:xmlpull:1.1.3.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
