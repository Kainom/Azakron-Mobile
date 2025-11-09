package com.aniak.azakron.data

import com.google.gson.annotations.SerializedName

data class Tag(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("name")
    val name: String,

    @SerializedName("user_id")
    val userId: String? = null
)