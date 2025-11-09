package com.aniak.azakron.api

import android.content.Context
import android.util.Log
import com.aniak.azakron.data.Note

class NoteRepository(
    private val context: Context,
    private val api: AzakronService = RetrofitClient.api
) {
    private val cachedNotes = mutableListOf<Note>()
    private var isCacheSynced = false

    private fun getUserId(): String {
        val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        return prefs.getString("user_id", "") ?: ""
    }

    suspend fun syncCache() {
        if (!isCacheSynced) {
            Log.d("NoteRepository", "Sincronizando cache...")
            getNotes()
            isCacheSynced = true
        }
    }

    suspend fun getPaginatedNotes(skip: Int, limit: Int): List<Note> {
        return try {
            val notes = api.getNotesPaginated(skip, limit, getUserId())

            notes.forEach { note ->
                Log.d("DEBUG_TAGS", "Note ${note.title} → TAGS = ${note.tags}")
                if (cachedNotes.none { it.id == note.id }) {
                    cachedNotes.add(note)
                }
            }

            Log.d("DEBUG_CACHE", "Cache agora tem ${cachedNotes.size} notas")
            notes
        } catch (e: Exception) {
            Log.e("NoteRepository", "Erro ao buscar notas paginadas", e)
            emptyList()
        }
    }

    suspend fun getNotes(): List<Note> {
        return try {
            val notes = api.getNotes(getUserId())
            cachedNotes.clear()
            cachedNotes.addAll(notes)
            Log.d("NoteRepository", "Cache atualizado com ${notes.size} notas")
            notes
        } catch (e: Exception) {
            Log.e("NoteRepository", "Erro ao buscar todas as notas", e)
            emptyList()
        }
    }

    fun getNoteFromCache(noteId: String): Note? {
        return cachedNotes.firstOrNull { it.id == noteId }
    }

    suspend fun getNote(noteId: String): Note? {
        val cached = getNoteFromCache(noteId)
        if (cached != null) {
            Log.d("NoteRepository", "Nota encontrada no cache")
            return cached
        }

        Log.d("NoteRepository", "Nota não encontrada no cache, buscando da API...")
        syncCache()
        return getNoteFromCache(noteId)
    }

    suspend fun searchLocalNotes(query: String): List<Note> {
        Log.d("NoteRepository", "Cache size = ${cachedNotes.size}")

        if (cachedNotes.isEmpty()) {
            Log.d("NoteRepository", "Cache vazio, buscando da API...")
            getNotes()
        }

        if (query.isBlank()) {
            Log.d("NoteRepository", "Query vazio, retornando ${cachedNotes.size} notas")
            return cachedNotes.toList()
        }

        val filtered = cachedNotes.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.tags.any { tag -> tag.name.contains(query, ignoreCase = true) }
        }

        Log.d("NoteRepository", "Busca por '$query' retornou ${filtered.size} resultados")
        return filtered
    }

    suspend fun createNote(note: Note): String {
        return try {
            val userId = getUserId()
            val noteWithUser = note.copy(userId = userId)
            val id = api.createNote(noteWithUser)

            cachedNotes.add(noteWithUser.copy(id = id))
            Log.d("NoteRepository", "Nota criada com ID: $id")
            id
        } catch (e: Exception) {
            Log.e("NoteRepository", "Erro ao criar nota", e)
            throw e
        }
    }

    suspend fun updateNote(noteId: String, note: Note): Boolean {
        return try {
            val userId = getUserId()
            val updated = api.updateNote(noteId, userId, note)

            if (updated) {
                val index = cachedNotes.indexOfFirst { it.id == noteId }
                if (index >= 0) {
                    cachedNotes[index] = note.copy(id = noteId, userId = userId)
                    Log.d("NoteRepository", "Nota atualizada no cache: $noteId")
                }
            }
            updated
        } catch (e: Exception) {
            Log.e("NoteRepository", "Erro ao atualizar nota", e)
            false
        }
    }

    suspend fun deleteNote(noteId: String): Boolean {
        return try {
            val deleted = api.deleteNote(noteId, getUserId())

            if (deleted) {
                cachedNotes.removeAll { it.id == noteId }
                Log.d("NoteRepository", "Nota $noteId removida do cache")
            }
            deleted
        } catch (e: Exception) {
            Log.e("NoteRepository", "Erro ao deletar nota", e)
            false
        }
    }

    fun clearCache() {
        cachedNotes.clear()
        isCacheSynced = false
        Log.d("NoteRepository", "Cache limpo")
    }

    fun getCacheSize(): Int = cachedNotes.size
}