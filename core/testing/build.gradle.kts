import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Shared test doubles. Hand-written fakes (not mocking-library mocks) are Google's
// recommendation: they behave like the real thing, survive refactors, and make
// tests read as behaviour rather than as a list of `verify { }` calls.
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
    api(projects.core.domain)
    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
}
