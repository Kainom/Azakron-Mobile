package com.aniak.azakron

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.aniak.azakron.databinding.ActivityNoteBinding
import com.google.android.material.chip.Chip
class Note : AppCompatActivity() {

    private lateinit var binding: ActivityNoteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackButton()
        loadNoteData()
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadNoteData() {
        // Dados de teste - futuramente virão do banco de dados
        val title = "Reunião de Planejamento"
        val description = """
            Discutir os próximos passos do projeto e definir as prioridades da sprint.
            
            Pontos importantes:
            - Revisar backlog
            - Definir estimativas
            - Alocar recursos
            - Estabelecer deadline
        """.trimIndent()
        val tags = arrayListOf("Trabalho", "Urgente", "Reunião", "Planejamento")

        // Configurar views
        binding.noteTitle.text = title
        binding.noteDescription.text = description

        // Adicionar chips de tags
        binding.chipGroup.removeAllViews()
        tags.forEach { tag ->
            addTagChip(tag)
        }
    }

    private fun addTagChip(tagName: String) {
        val chip = Chip(this).apply {
            text = tagName
            isClickable = false
            isCheckable = false

            // Estilização do chip para tema escuro
            chipBackgroundColor = ContextCompat.getColorStateList(
                this@Note,
                R.color.primary
            )
            setTextColor(ContextCompat.getColor(this@Note, R.color.background_dark))
            chipStrokeWidth = 0f
        }
        binding.chipGroup.addView(chip)
    }
}