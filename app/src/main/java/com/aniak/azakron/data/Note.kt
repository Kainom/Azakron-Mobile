package com.aniak.azakron.data
import com.google.gson.annotations.SerializedName

data class Note(
    val id: String,
    val title: String,
    val description: String,
    val tags: List<Tag>,

    @SerializedName("user_id")
    val userId: String? = null
)
