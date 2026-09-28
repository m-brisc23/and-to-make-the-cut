package com.tomakethecut.core.data.network.mock

/** Where the mock server reads its canned responses from. An interface so tests can inject strings. */
fun interface FixtureSource {
    /** @return the fixture body, or null when no fixture exists at [path]. */
    fun read(path: String): String?
}

/**
 * Reads fixtures from Java resources under `mock/`. Java resources in a JVM library are
 * merged into the APK, so this works identically in unit tests and on a device.
 */
class ClasspathFixtureSource(
    private val classLoader: ClassLoader = ClasspathFixtureSource::class.java.classLoader!!,
) : FixtureSource {
    override fun read(path: String): String? =
        classLoader.getResourceAsStream("mock/$path")?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
}
