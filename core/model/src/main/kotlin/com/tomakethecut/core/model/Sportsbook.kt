package com.tomakethecut.core.model

/**
 * Books we know how to display. The [apiKey] is the identifier the odds provider uses;
 * keeping it here (instead of relying on `enum.name`) means renaming a Kotlin constant
 * can never silently break parsing.
 */
enum class Sportsbook(val apiKey: String, val displayName: String) {
    DRAFTKINGS("draftkings", "DraftKings"),
    FANDUEL("fanduel", "FanDuel"),
    BETMGM("betmgm", "BetMGM"),
    CAESARS("caesars", "Caesars"),
    ;

    companion object {
        fun fromApiKey(key: String): Sportsbook? = entries.firstOrNull { it.apiKey.equals(key, ignoreCase = true) }
    }
}
