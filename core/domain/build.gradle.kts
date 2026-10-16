import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The domain layer is pure Kotlin. It depends on nothing Android and on no
// networking library: repository *interfaces* live here, implementations live in
// :core:data (Dependency Inversion — the "D" in SOLID).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)
    // JSR-330 annotations only (@Inject) — keeps use cases DI-framework agnostic.
    implementation(libs.javax.inject)

    testImplementation(projects.core.testing)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
