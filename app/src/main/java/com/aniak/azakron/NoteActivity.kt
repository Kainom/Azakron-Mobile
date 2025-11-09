package com.aniak.azakron

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aniak.azakron.api.NoteRepository
import com.aniak.azakron.databinding.ActivityNoteBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class NoteActivity : BaseActivity() {

    private lateinit var binding: ActivityNoteBinding
    private val noteRepository by lazy { NoteRepository(this) }

    private var currentNoteId: String? = null
    private var currentNoteTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackButton()
        setupMenuButton(binding.btnMenu)
        setupEditButton()      // NOVO
        setupDeleteButton()    // NOVO
        loadNoteData()
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    // NOVO: Função para editar nota
    private fun setupEditButton() {
        binding.btnEdit.setOnClickListener {
            val noteId = intent.getStringExtra("NOTE_ID")
            val noteTitle = intent.getStringExtra("NOTE_TITLE")
            val noteDescription = intent.getStringExtra("NOTE_DESCRIPTION")
            val noteTags = intent.getStringArrayListExtra("NOTE_TAGS")

            val editIntent = Intent(this, NewNoteActivity::class.java).apply {
                putExtra("MODE", "EDIT")
                putExtra("NOTE_ID", noteId)
                putExtra("NOTE_TITLE", noteTitle)
                putExtra("NOTE_DESCRIPTION", noteDescription)
                putStringArrayListExtra("NOTE_TAGS", noteTags)
            }
            startActivity(editIntent)
            finish() // Fecha a tela de visualização
        }
    }

    // NOVO: Função para deletar nota
    private fun setupDeleteButton() {
        binding.btnDelete.setOnClickListener {
            deleteNoteWithConfirmation()
        }
    }

    // NOVO: Confirmação de exclusão
    private fun deleteNoteWithConfirmation() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_note, null)
        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Customizar mensagem com nome da nota
        dialogView.findViewById<TextView>(R.id.dialogMessage).text =
            "A nota '${currentNoteTitle ?: "esta nota"}' será excluída permanentemente."

        // Botão Cancelar
        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        // Botão Confirmar Exclusão
        dialogView.findViewById<MaterialButton>(R.id.btnConfirmDelete).setOnClickListener {
            dialog.dismiss()
            deleteNote()
        }

        dialog.show()
    }

    // NOVO: Função para deletar nota
    private fun deleteNote() {
        val noteId = currentNoteId ?: return

        showLoading(true, "Excluindo nota...")

        lifecycleScope.launch {
            try {
                val deleted = noteRepository.deleteNote(noteId)

                if (deleted) {
                    Toast.makeText(
                        this@NoteActivity,
                        "Nota excluída com sucesso",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Sinaliza que NotesActivity precisa recarregar
                    getSharedPreferences("app_state", MODE_PRIVATE)
                        .edit()
                        .putBoolean("needs_refresh", true)
                        .apply()

                    // Volta para a lista
                    val intent = Intent(this@NoteActivity, NotesActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this@NoteActivity,
                        "Erro ao excluir nota",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@NoteActivity,
                    "Erro: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean, message: String = "Carregando...") {
        val overlay = binding.includeLoading.loadingOverlay
        val text = binding.includeLoading.loadingText

        overlay.visibility = if (show) View.VISIBLE else View.GONE
        text.text = message
    }

    private fun loadNoteData() {
        val noteId = intent.getStringExtra("NOTE_ID")

        if (noteId.isNullOrBlank()) {
            Log.e("NoteActivity", "NOTE_ID não foi passado")
            finish()
            return
        }

        currentNoteId = noteId
        binding.materialCard.visibility = View.GONE
        showLoading(true, "Carregando nota...")

        lifecycleScope.launch {
            try {
                val note = noteRepository.getNote(noteId)

                if (note != null) {
                    currentNoteTitle = note.title
                    binding.noteTitle.text = note.title
                    binding.noteDescription.text = note.description

                    binding.chipGroup.removeAllViews()
                    note.tags.forEach { tag ->
                        addTagChip(tag.name)
                    }

                    Log.d("NoteActivity", "Nota carregada: ${note.title}")
                } else {
                    Log.e("NoteActivity", "Nota não encontrada com ID: $noteId")
                    binding.noteTitle.text = "Nota não encontrada"
                    binding.noteDescription.text = "Não foi possível carregar os dados desta nota."
                }

            } catch (e: Exception) {
                Log.e("NoteActivity", "Erro ao carregar nota", e)
                binding.noteTitle.text = "Erro"
                binding.noteDescription.text = "Ocorreu um erro ao carregar a nota: ${e.message}"
            } finally {
                binding.materialCard.visibility = View.VISIBLE
                showLoading(false)
            }
        }
    }

    private fun addTagChip(tagName: String) {
        val chip = Chip(this).apply {
            text = tagName
            isClickable = false
            isCheckable = false

            chipBackgroundColor = ContextCompat.getColorStateList(
                this@NoteActivity,
                R.color.primary
            )
            setTextColor(ContextCompat.getColor(this@NoteActivity, R.color.background_dark))
            chipStrokeWidth = 0f
        }
        binding.chipGroup.addView(chip)
    }
}