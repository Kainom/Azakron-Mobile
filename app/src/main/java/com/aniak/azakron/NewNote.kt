package com.aniak.azakron

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.aniak.azakron.databinding.ActivityNewNoteBinding
import com.example.azakron.Notes
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class NewNote : AppCompatActivity() {

    private lateinit var binding: ActivityNewNoteBinding
    private var isClosing = false

    private val availableTags = listOf(
        "Trabalho",
        "Pessoal",
        "Urgente",
        "Importante",
        "Estudo",
        "Projeto",
        "Ideias",
        "Casa",
        "Saúde",
        "Lazer",
        "Desenvolvimento",
        "Programação",
        "Fitness",
        "Culinária",
        "Viagem"
    )

    override fun onPause() {
        super.onPause()

        if (!isClosing) {
            val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
            val note = getSharedPreferences("note", MODE_PRIVATE)

            prefs.edit { putString("last_screen", "NewNote") }
            note.edit {
                putString("title", binding.etTitle.text.toString())
                putString("description", binding.etDescription.text.toString())
                putStringSet("selected_tags", getSelectedTags().toSet())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        carregarDados()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityNewNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTags()
        close()
        setupSaveButton()
    }

    private fun setupTags() {
        binding.chipGroupTags.removeAllViews()

        availableTags.forEach { tagName ->
            val chip = Chip(this)
            chip.text = tagName
            chip.isCheckable = true
            chip.setChipBackgroundColorResource(R.color.background_dark)  // 👈 Mude aqui
            chip.setTextColor(getColor(R.color.white))
            chip.chipStrokeWidth = 2f
            chip.setChipStrokeColorResource(R.color.primary)
            chip.checkedIconTint = ColorStateList.valueOf(getColor(R.color.primary))

            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    chip.setChipBackgroundColorResource(R.color.primary)
                    chip.setTextColor(getColor(android.R.color.background_dark))
                } else {
                    chip.setChipBackgroundColorResource(R.color.background_dark)  // 👈 E aqui
                    chip.setTextColor(getColor(R.color.white))
                }
            }

            binding.chipGroupTags.addView(chip)
        }
    }

    private fun getSelectedTags(): List<String> {
        val selectedTags = mutableListOf<String>()

        for (i in 0 until binding.chipGroupTags.childCount) {
            val chip = binding.chipGroupTags.getChildAt(i) as Chip
            if (chip.isChecked) {
                selectedTags.add(chip.text.toString())
            }
        }

        return selectedTags
    }

    private fun setSelectedTags(tags: Set<String>) {
        for (i in 0 until binding.chipGroupTags.childCount) {
            val chip = binding.chipGroupTags.getChildAt(i) as Chip
            chip.isChecked = tags.contains(chip.text.toString())
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

            if (title.isNotBlank()) {
                // Aqui você pode salvar a nota no banco de dados
                // saveNoteToDatabase(title, description, tags)

                // Limpar dados temporários
                clearTemporaryData()

                // Voltar para a tela de notas
                val intent = Intent(this, Notes::class.java)
                startActivity(intent)
                finish()
                isClosing = true

            } else {
                binding.etTitle.error = "Título é obrigatório"
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
            // Verificar se há algum conteúdo digitado
            val hasContent = binding.etTitle.text.toString().isNotBlank() ||
                    binding.etDescription.text.toString().isNotBlank() ||
                    getSelectedTags().isNotEmpty()

            if (hasContent) {
                // Inflar o layout customizado
                val dialogView = layoutInflater.inflate(R.layout.dialog_discard, null)

                val dialog = AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create()

                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                // Botão Cancelar
                dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
                    dialog.dismiss()
                }

                // Botão Descartar
                dialogView.findViewById<MaterialButton>(R.id.btnDiscard).setOnClickListener {
                    isClosing = true
                    clearTemporaryData()
                    val intent = Intent(this, Notes::class.java)
                    startActivity(intent)
                    finish()
                    dialog.dismiss()
                }

                dialog.show()
            } else {
                // Não há conteúdo, pode sair direto
                isClosing = true
                clearTemporaryData()
                val intent = Intent(this, Notes::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}