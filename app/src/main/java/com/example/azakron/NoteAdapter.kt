package com.example.azakron

import com.example.azakron.model.NoteModel
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.azakron.databinding.ItemNoteBinding
import com.google.android.material.chip.Chip

class NotesAdapter(
    private val notes: MutableList<NoteModel>,
    private val onNoteClick: (NoteModel) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: NoteModel) {
            binding.noteTitle.text = note.title
            binding.noteDescription.text = note.description

            // Limitar descrição a 2 linhas
            binding.noteDescription.maxLines = 2

            // Adicionar chips de tags (máximo 3)
            binding.chipGroup.removeAllViews()
            note.tags.take(3).forEach { tag ->
                addChip(tag)
            }

            // Click listener
            binding.root.setOnClickListener {
                onNoteClick(note)
            }
        }

        private fun addChip(tagName: String) {
            val chip = Chip(binding.root.context).apply {
                text = tagName
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
