package com.aniak.azakron

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aniak.azakron.data.Note
import com.aniak.azakron.api.NoteRepository
import com.aniak.azakron.api.TagRepository
import com.aniak.azakron.databinding.ActivityNotesBinding
import com.aniak.azakron.databinding.LayoutpopBinding
import com.aniak.azakron.databinding.ModalTagBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class NotesActivity : BaseActivity() {

    private lateinit var binding: ActivityNotesBinding
    private lateinit var notesAdapter: NotesAdapter

    private val displayedNotes = mutableListOf<Note>()
    private var currentPage = 0
    private val pageSize = 10
    private var isLoading = false
    private var isSearchActive = false
    private var currentSearchQuery = ""
    private var hasMoreNotes = true

    private val noteRepository by lazy { NoteRepository(this) }
    private val tagRepository by lazy { TagRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        noteRepository.clearCache()

        setupMenuButton(binding.btnMenu)
        setupRecyclerView()
        setupSearchBar()
        loadNextPage()

        binding.buttonAddNote.setOnClickListener { view ->
            showAddNotePopup(view)
        }
    }

    override fun onResume() {
        super.onResume()

        // Verifica se voltou de criar/editar nota
        val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
        val needsRefresh = prefs.getBoolean("needs_refresh", false)

        if (needsRefresh) {
            // Limpa cache e recarrega
            noteRepository.clearCache()
            displayedNotes.clear()
            currentPage = 0
            hasMoreNotes = true
            isLoading = false
            isSearchActive = false

            // Limpa busca se houver
            binding.searchInput.text.clear()

            notesAdapter.notifyDataSetChanged()
            loadNextPage()

            // Remove flag
            prefs.edit().putBoolean("needs_refresh", false).apply()
        }
    }

    private fun setupSearchBar() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                currentSearchQuery = query

                binding.clearSearchButton.visibility =
                    if (query.isNotEmpty()) View.VISIBLE else View.GONE

                if (query.isEmpty()) {
                    isSearchActive = false
                    displayedNotes.clear()
                    currentPage = 0
                    hasMoreNotes = true
                    loadNextPage()
                } else {
                    isSearchActive = true
                    searchNotes(query)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.clearSearchButton.setOnClickListener {
            binding.searchInput.text.clear()
            binding.searchInput.clearFocus()
        }
    }

    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(
            notes = displayedNotes,
            onNoteClick = { note -> openNoteDetail(note) },
            onEditClick = { note -> editNote(note) },
            onDeleteClick = { note -> deleteNoteWithConfirmation(note) }
        )

        val layoutManager = LinearLayoutManager(this)
        binding.recyclerViewNotes.apply {
            this.layoutManager = layoutManager
            adapter = notesAdapter
            setHasFixedSize(true)
        }

        binding.recyclerViewNotes.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (isSearchActive) return

                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3) {
                    loadNextPage()
                }
            }
        })
    }

    private fun showLoading(show: Boolean, message: String = "Carregando...") {
        val overlay = binding.includeLoading.loadingOverlay
        val text = binding.includeLoading.loadingText

        overlay.visibility = if (show) View.VISIBLE else View.GONE
        text.text = message
    }

    private fun loadNextPage() {
        if (isLoading || isSearchActive || !hasMoreNotes) return
        isLoading = true
        showLoading(true, "Carregando notas...")

        lifecycleScope.launch {
            try {
                val skip = currentPage * pageSize
                val notes = noteRepository.getPaginatedNotes(skip, pageSize)

                if (notes.isEmpty()) {
                    hasMoreNotes = false
                } else {
                    val insertPosition = displayedNotes.size
                    displayedNotes.addAll(notes)
                    notesAdapter.notifyItemRangeInserted(insertPosition, notes.size)
                    currentPage++
                }
                showLoading(false)

                binding.emptyStateLayout.visibility =
                    if (displayedNotes.isEmpty()) View.VISIBLE else View.GONE

            } catch (e: Exception) {
                e.printStackTrace()
                binding.emptyStateLayout.visibility = View.VISIBLE
            } finally {
                isLoading = false
                showLoading(false)
            }
        }
    }

    private fun searchNotes(query: String) {
        lifecycleScope.launch {
            val results = noteRepository.searchLocalNotes(query)
            displayedNotes.clear()
            displayedNotes.addAll(results)
            notesAdapter.notifyDataSetChanged()
            binding.emptyStateLayout.visibility =
                if (results.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun openNoteDetail(note: Note) {
        val tagNames = ArrayList(note.tags.map { it.name })

        val intent = Intent(this, NoteActivity::class.java).apply {
            putExtra("NOTE_ID", note.id)
            putExtra("NOTE_TITLE", note.title)
            putExtra("NOTE_DESCRIPTION", note.description)
            putStringArrayListExtra("NOTE_TAGS", tagNames)
        }
        startActivity(intent)
    }

    // NOVO: Função para editar nota
    private fun editNote(note: Note) {
        val intent = Intent(this, NewNoteActivity::class.java).apply {
            putExtra("MODE", "EDIT")
            putExtra("NOTE_ID", note.id)
            putExtra("NOTE_TITLE", note.title)
            putExtra("NOTE_DESCRIPTION", note.description)
            putStringArrayListExtra("NOTE_TAGS", ArrayList(note.tags.map { it.name }))
        }
        startActivity(intent)
    }

    private fun deleteNoteWithConfirmation(note: Note) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_note, null)
        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setView(dialogView)
            .create()

        // Fundo transparente para mostrar o CardView com cantos arredondados
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Customizar mensagem com nome da nota
        dialogView.findViewById<TextView>(R.id.dialogMessage).text =
            "A nota '${note.title}' será excluída permanentemente."

        // Botão Cancelar
        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        // Botão Confirmar Exclusão
        dialogView.findViewById<MaterialButton>(R.id.btnConfirmDelete).setOnClickListener {
            dialog.dismiss()
            deleteNote(note)
        }

        dialog.show()
    }

    // NOVO: Função para deletar nota
    private fun deleteNote(note: Note) {
        showLoading(true, "Excluindo nota...")

        lifecycleScope.launch {
            try {
                val deleted = noteRepository.deleteNote(note.id)

                if (deleted) {
                    // Remove da lista e atualiza UI
                    val position = displayedNotes.indexOf(note)
                    if (position >= 0) {
                        displayedNotes.removeAt(position)
                        notesAdapter.notifyItemRemoved(position)
                    }

                    // Atualiza empty state
                    binding.emptyStateLayout.visibility =
                        if (displayedNotes.isEmpty()) View.VISIBLE else View.GONE

                    Toast.makeText(
                        this@NotesActivity,
                        "✅ Nota excluída com sucesso",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@NotesActivity,
                        "❌ Erro ao excluir nota",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@NotesActivity,
                    "Erro: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showAddNotePopup(view: View) {
        val popupBinding = LayoutpopBinding.inflate(layoutInflater)
        val popupWindow = PopupWindow(
            popupBinding.root,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.elevation = 10f
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isOutsideTouchable = true
        popupWindow.showAsDropDown(view, -280, -460)

        popupBinding.optionCreateNote.setOnClickListener {
            startActivity(Intent(this, NewNoteActivity::class.java))
            popupWindow.dismiss()
        }

        popupBinding.optionCreateTag.setOnClickListener {
            popupWindow.dismiss()
            showCreateTagDialog()
        }
    }

    private fun showCreateTagDialog() {
        val dialogBinding = ModalTagBinding.inflate(LayoutInflater.from(this))
        val dialog = MaterialAlertDialogBuilder(this, R.style.CustomAlertDialog)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#1a1a1a")))

        dialogBinding.buttonSaveTag.setOnClickListener {
            val tagName = dialogBinding.tagInput.text.toString().trim()

            if (tagName.isBlank()) {
                Toast.makeText(this, "❌ O nome da tag não pode estar vazio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dialog.dismiss()
            createTag(tagName)
        }

        dialogBinding.buttonCancelTag.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun createTag(tagName: String) {
        showLoading(true, "Criando tag...")

        lifecycleScope.launch {
            try {
                val tagId = tagRepository.createTag(tagName)

                if (tagId != null) {
                    Toast.makeText(
                        this@NotesActivity,
                        "Tag '$tagName' criada com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@NotesActivity,
                        "Erro ao criar tag",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@NotesActivity,
                    "Erro: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }
}