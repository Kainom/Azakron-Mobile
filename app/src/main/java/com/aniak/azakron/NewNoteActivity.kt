package com.aniak.azakron

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.aniak.azakron.api.NoteRepository
import com.aniak.azakron.api.TagRepository
import com.aniak.azakron.data.Note
import com.aniak.azakron.data.Tag
import com.aniak.azakron.databinding.ActivityNewNoteBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class NewNoteActivity : BaseActivity() {

    private lateinit var binding: ActivityNewNoteBinding
    private var isClosing = false

    private val tagRepository by lazy { TagRepository(this) }
    private val noteRepository by lazy { NoteRepository(this) }

    private var availableTags = listOf<Tag>()

    // NOVO: Variáveis para modo de edição
    private var editMode = false
    private var editingNoteId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityNewNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NOVO: Detectar modo de edição
        editMode = intent.getStringExtra("MODE") == "EDIT"
        editingNoteId = intent.getStringExtra("NOTE_ID")

        if (editMode) {
            binding.btnSave.text = "Atualizar Nota"
        }

        loadTagsFromDatabase()
        setupSaveButton()
        close()
    }

    private fun loadTagsFromDatabase() {
        showLoading(true, "Carregando tags...")

        lifecycleScope.launch {
            try {
                availableTags = tagRepository.getTags()

                setupTags()

                // NOVO: Carrega dados no modo de edição
                if (editMode) {
                    loadEditData()
                } else {
                    carregarDados() // Carrega dados salvos (rascunho)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@NewNoteActivity,
                    "❌ Erro ao carregar tags: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            } finally {
                showLoading(false)
            }
        }
    }

    // NOVO: Carregar dados no modo de edição
    private fun loadEditData() {
        val title = intent.getStringExtra("NOTE_TITLE") ?: ""
        val description = intent.getStringExtra("NOTE_DESCRIPTION") ?: ""
        val tagNames = intent.getStringArrayListExtra("NOTE_TAGS")?.toSet() ?: emptySet()

        binding.etTitle.setText(title)
        binding.etDescription.setText(description)
        setSelectedTags(tagNames)
    }

    private fun showLoading(show: Boolean, message: String = "Salvando...") {
        val overlay = binding.includeLoading.loadingOverlay
        val text = binding.includeLoading.loadingText

        overlay.visibility = if (show) View.VISIBLE else View.GONE
        text.text = message
    }

    override fun onPause() {
        super.onPause()
        // NOVO: Não salva rascunho no modo de edição
        if (!isClosing && !editMode) {
            val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
            val note = getSharedPreferences("note", MODE_PRIVATE)

            prefs.edit { putString("last_screen", "NewNote") }

            note.edit {
                putString("title", binding.etTitle.text.toString())
                putString("description", binding.etDescription.text.toString())
                putStringSet(
                    "selected_tags",
                    getSelectedTags().map { it.name }.toSet()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun setupTags() {
        binding.chipGroupTags.removeAllViews()

        if (availableTags.isEmpty()) {
            val emptyText = android.widget.TextView(this)
            emptyText.text = "Nenhuma tag disponível. Crie tags na tela anterior."
            emptyText.setTextColor(getColor(R.color.text_secondary))
            emptyText.textSize = 14f
            emptyText.setPadding(16, 16, 16, 16)
            binding.chipGroupTags.addView(emptyText)
            return
        }

        availableTags.forEach { tag ->
            val chip = Chip(this)
            chip.text = tag.name
            chip.isCheckable = true
            chip.setChipBackgroundColorResource(R.color.background_dark)
            chip.setTextColor(getColor(R.color.white))
            chip.chipStrokeWidth = 2f
            chip.setChipStrokeColorResource(R.color.primary)
            chip.checkedIconTint = ColorStateList.valueOf(getColor(R.color.primary))

            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    chip.setChipBackgroundColorResource(R.color.primary)
                    chip.setTextColor(getColor(android.R.color.background_dark))
                } else {
                    chip.setChipBackgroundColorResource(R.color.background_dark)
                    chip.setTextColor(getColor(R.color.white))
                }
            }

            binding.chipGroupTags.addView(chip)
        }
    }

    private fun getSelectedTags(): List<Tag> {
        val selectedTags = mutableListOf<Tag>()
        for (i in 0 until binding.chipGroupTags.childCount) {
            val view = binding.chipGroupTags.getChildAt(i)
            if (view is Chip && view.isChecked) {
                val tag = availableTags.firstOrNull { it.name == view.text.toString() }
                if (tag != null) {
                    selectedTags.add(tag)
                }
            }
        }
        return selectedTags
    }

    private fun setSelectedTags(tagNames: Set<String>) {
        for (i in 0 until binding.chipGroupTags.childCount) {
            val view = binding.chipGroupTags.getChildAt(i)
            if (view is Chip) {
                view.isChecked = tagNames.contains(view.text.toString())
            }
        }
    }

    private fun carregarDados() {
        val note = getSharedPreferences("note", MODE_PRIVATE)
        val title = note.getString("title", "")
        val description = note.getString("description", "")
        val selectedTags = note.getStringSet("selected_tags", emptySet()) ?: emptySet()

        binding.etTitle.setText(title)
        binding.etDescription.setText(description)
        setSelectedTags(selectedTags)
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val description = binding.etDescription.text.toString()
            val tags = getSelectedTags()

            if (title.isBlank()) {
                binding.etTitle.error = "Título é obrigatório"
                return@setOnClickListener
            }

            showLoading(true, if (editMode) "Atualizando..." else "Salvando nota...")

            lifecycleScope.launch {
                try {
                    val userId = getSharedPreferences("user_session", MODE_PRIVATE)
                        .getString("user_id", null)

                    if (userId == null) {
                        binding.etTitle.error = "Usuário não autenticado"
                        showLoading(false)
                        return@launch
                    }

                    val note = Note(
                        id = editingNoteId ?: "",
                        title = title,
                        description = description,
                        tags = tags,
                        userId = userId
                    )

                    // NOVO: Cria ou atualiza dependendo do modo
                    val success = if (editMode) {
                        noteRepository.updateNote(editingNoteId!!, note)
                    } else {
                        val noteId = noteRepository.createNote(note)
                        noteId.isNotEmpty()
                    }

                    if (success) {
                        Toast.makeText(
                            this@NewNoteActivity,
                            if (editMode) " Nota atualizada!" else " Nota criada!",
                            Toast.LENGTH_SHORT
                        ).show()

                        clearTemporaryData()

                        // NOVO: Sinaliza que NotesActivity precisa recarregar
                        getSharedPreferences("app_state", MODE_PRIVATE)
                            .edit()
                            .putBoolean("needs_refresh", true)
                            .apply()

                        val intent = Intent(this@NewNoteActivity, NotesActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish()
                        isClosing = true
                    } else {
                        Toast.makeText(
                            this@NewNoteActivity,
                            "Erro ao ${if (editMode) "atualizar" else "salvar"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@NewNoteActivity,
                        "Erro: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    showLoading(false)
                }
            }
        }
    }

    private fun clearTemporaryData() {
        val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
        val note = getSharedPreferences("note", MODE_PRIVATE)
        prefs.edit { putString("last_screen", null) }
        note.edit {
            putString("title", "")
            putString("description", "")
            putStringSet("selected_tags", emptySet())
        }
    }

    private fun close() {
        binding.btnClose.setOnClickListener {
            val hasContent = binding.etTitle.text.toString().isNotBlank() ||
                    binding.etDescription.text.toString().isNotBlank() ||
                    getSelectedTags().isNotEmpty()

            if (hasContent) {
                val dialogView = layoutInflater.inflate(R.layout.dialog_discard, null)
                val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
                    .setView(dialogView)
                    .create()

                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#1a1a1a")))

                dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
                    dialog.dismiss()
                }
                dialogView.findViewById<MaterialButton>(R.id.btnDiscard).setOnClickListener {
                    isClosing = true
                    clearTemporaryData()
                    startActivity(Intent(this, NotesActivity::class.java))
                    finish()
                    dialog.dismiss()
                }

                dialog.show()
            } else {
                isClosing = true
                clearTemporaryData()
                startActivity(Intent(this, NotesActivity::class.java))
                finish()
            }
        }
    }
}