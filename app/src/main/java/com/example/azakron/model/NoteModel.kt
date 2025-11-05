package com.example.azakron.model


data class NoteModel(
    val id: Int,
    var title: String,
    val description: String,
    val tags: List<String>
)