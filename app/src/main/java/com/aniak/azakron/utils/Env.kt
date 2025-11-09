package com.aniak.azakron.utils

import io.github.cdimascio.dotenv.dotenv

object Env {
    private val dotenv = dotenv {
        ignoreIfMissing = true
    }

    val BASE_URL: String = dotenv["BASE_URL"] ?: ""
    val VERCEL_TOKEN: String = dotenv["VERCEL_TOKEN"] ?: ""
}
