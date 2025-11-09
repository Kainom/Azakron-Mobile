package com.aniak.azakron.api

import android.content.Context
import android.util.Log
import com.aniak.azakron.data.Tag

class TagRepository(
    private val context: Context,
    private val api: AzakronService = RetrofitClient.api
) {
    private val cachedTags = mutableListOf<Tag>()
    private var isCacheSynced = false

    private fun getUserId(): String {
        val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        return prefs.getString("user_id", "") ?: ""
    }

    suspend fun getTags(): List<Tag> {
        return try {
            val tags = api.getTags(getUserId())
            cachedTags.clear()
            cachedTags.addAll(tags)
            isCacheSynced = true
            Log.d("TagRepository", "Cache atualizado com ${tags.size} tags")
            tags
        } catch (e: Exception) {
            Log.e("TagRepository", "Erro ao buscar tags", e)
            emptyList()
        }
    }

    suspend fun getTag(tagId: String): Tag? {
        return try {
            api.getTag(tagId, getUserId())
        } catch (e: Exception) {
            Log.e("TagRepository", "Erro ao buscar tag $tagId", e)
            null
        }
    }

    suspend fun createTag(tagName: String): String? {
        return try {
            val userId = getUserId()
            val tag = Tag(name = tagName, userId = userId)
            val tagId = api.createTag(tag)

            // Adiciona no cache
            cachedTags.add(tag.copy(id = tagId))
            Log.d("TagRepository", "Tag criada com ID: $tagId")
            tagId
        } catch (e: Exception) {
            Log.e("TagRepository", "Erro ao criar tag", e)
            null
        }
    }

    suspend fun updateTag(tagId: String, tagName: String): Boolean {
        return try {
            val userId = getUserId()
            val tag = Tag(id = tagId, name = tagName, userId = userId)
            val updated = api.updateTag(userId, tagId, tag)

            if (updated) {
                val index = cachedTags.indexOfFirst { it.id == tagId }
                if (index >= 0) {
                    cachedTags[index] = tag
                    Log.d("TagRepository", "Tag atualizada no cache: $tagId")
                }
            }
            updated
        } catch (e: Exception) {
            Log.e("TagRepository", "Erro ao atualizar tag", e)
            false
        }
    }

    suspend fun deleteTag(tagId: String): Boolean {
        return try {
            val deleted = api.deleteTag(getUserId(), tagId)

            if (deleted) {
                cachedTags.removeAll { it.id == tagId }
                Log.d("TagRepository", "Tag $tagId removida do cache")
            }
            deleted
        } catch (e: Exception) {
            Log.e("TagRepository", "Erro ao deletar tag", e)
            false
        }
    }

    fun getCachedTags(): List<Tag> = cachedTags.toList()

    fun clearCache() {
        cachedTags.clear()
        isCacheSynced = false
        Log.d("TagRepository", "Cache de tags limpo")
    }
}