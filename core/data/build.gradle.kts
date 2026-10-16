import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// A pure JVM module: Retrofit, OkHttp, kotlinx.serialization and Dagger are all plain
// JVM libraries, so the whole data layer — including the mock server — is testable
// with fast local unit tests, no emulator or Robolectric required.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    api(projects.core.domain)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)

    // hilt-core gives us @InstallIn/SingletonComponent without the Android runtime.
    implementation(libs.hilt.core)
    ksp(libs.hilt.compiler)

    testImplementation(projects.core.testing)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
