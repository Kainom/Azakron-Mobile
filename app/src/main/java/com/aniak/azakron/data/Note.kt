package com.aniak.azakron.data

data class Note(
    val id: Int,
    var title: String,
    val description: String,
    val tags: List<String>
)