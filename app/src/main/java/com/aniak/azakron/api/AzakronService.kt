package com.aniak.azakron.api

import com.aniak.azakron.data.Note
import com.aniak.azakron.data.Tag
import com.aniak.azakron.utils.Env
import retrofit2.http.*

interface AzakronService {

    companion object {
        private val TOKEN = Env.VERCEL_TOKEN
    }

    // ============ NOTES ============
    @GET("notes/{user_id}")
    suspend fun getNotes(
        @Path("user_id") userId: String,
        @Header("x-vercel-protection-bypass") token: String = TOKEN
    ): List<Note>

    @GET("notes/paginated")
    suspend fun getNotesPaginated(
        @Query("skip") skip: Int,
        @Query("limit") limit: Int,
        @Query("user_id") userId: String,
        @Header("x-vercel-protection-bypass") token: String = TOKEN
    ): List<Note>

    @GET("notes/search/{user_id}")
    suspend fun searchNotes(
        @Path("user_id") userId: String,
        @Query("query") query: String,
        @Header("x-vercel-protection-bypass") token: String = TOKEN
    ): List<Note>

    @POST("notes")
    suspend fun createNote(
        @Body note: Note,
        @Header("x-vercel-protection-bypass") token: String = TOKEN
    ): String

    @PUT("notes/{note_id}/{user_id}")
    suspend fun updateNote(
        @Path("note_id") noteId: String,
        @Path("user_id") userId: String,
        @Body note: Note,
        @Header("x-vercel-protection-bypass") token: String = TOKEN
    ): Boolean

    @DELETE("notes/{note_id}/{user_id}")
    suspend fun deleteNote(
        @Path("note_id") noteId: String,
        @Path("user_id") userId: String,
        @Header("x-vercel-protection-bypass") token: String = TOKEN
    ): Boolean

    @GET("note/{note_id}/{user_id}")
    suspend fun getNote(
        @Path("note_id") noteId: String,
        @Path("user_id") userId: String,
        @Header("x-vercel-protection-bypass") token: String = TOKEN
    ): Note

    // ============ TAGS ============
    @POST("tags")
    suspend fun createTag(
        @Body tag: Tag,
        @Header("x-vercel-protection-bypass") token: String = TOKEN
    ): String

    @GET("tag/{tag_id}")
    suspend fun getTag(
        @Path("tag_id") tagId: String,
        @Query("user_id") userId: String,
        @Header("x-vercel-protection-bypass") token: String = TOKEN
    ): Tag

    @GET("tags/{user_id}")
    suspend fun getTags(
        @Path("user_id") userId: String,
        @Header("x-vercel-protection-bypass") token: String = TOKEN
    ): List<Tag>

    @PUT("tags/{user_id}/{tag_id}")
    suspend fun updateTag(
        @Path("user_id") userId: String,
        @Path("tag_id") tagId: String,
        @Body tag: Tag,
        @Header("x-vercel-protection-bypass") token: String = TOKEN
    ): Boolean

    @DELETE("tags/{user_id}/{tag_id}")
    suspend fun deleteTag(
        @Path("user_id") userId: String,
        @Path("tag_id") tagId: String,
        @Header("x-vercel-protection-bypass") token: String = TOKEN
    ): Boolean
}