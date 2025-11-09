package com.aniak.azakron

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aniak.azakron.data.Note
import com.aniak.azakron.data.Tag
import com.aniak.azakron.databinding.ItemNoteBinding
import com.google.android.material.chip.Chip

class NotesAdapter(
    private val notes: MutableList<Note>,
    private val onNoteClick: (Note) -> Unit,
    private val onEditClick: (Note) -> Unit,      // NOVO
    private val onDeleteClick: (Note) -> Unit     // NOVO
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) {
            binding.noteTitle.text = note.title
            binding.noteDescription.text = note.description

            // Limitar descrição a 2 linhas
            binding.noteDescription.maxLines = 2

            // Adicionar chips de tags (máximo 3)
            binding.chipGroup.removeAllViews()
            note.tags.take(3).forEach { tag ->
                addChip(tag)
            }

            // Click listeners
            binding.root.setOnClickListener {
                onNoteClick(note)
            }

            // NOVO: Botões de edição e exclusão
            binding.btnEdit.setOnClickListener {
                onEditClick(note)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(note)
            }
        }

        private fun addChip(tag: Tag) {
            val chip = Chip(binding.root.context).apply {
                text = tag.name
                isClickable = false
                isCheckable = false
                chipBackgroundColor = ContextCompat.getColorStateList(
                    binding.root.context,
                    R.color.primary
                )
                setTextColor(
                    ContextCompat.getColor(
                        binding.root.context,
                        R.color.background_dark
                    )
                )
                chipStrokeWidth = 0f
                textSize = 12f
            }
            binding.chipGroup.addView(chip)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(notes[position])
    }

    override fun getItemCount(): Int = notes.size
}