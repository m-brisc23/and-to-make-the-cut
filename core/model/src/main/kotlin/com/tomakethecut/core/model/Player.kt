package com.tomakethecut.core.model

data class Player(
    val id: String,
    val name: String,
    /** ISO-3166 alpha-3 style code as supplied by the provider, e.g. "USA", "NIR". */
    val country: String,
    val tour: Tour,
)
