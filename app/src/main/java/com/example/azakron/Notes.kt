package com.example.azakron

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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.azakron.databinding.ActivityNotesBinding
import com.example.azakron.databinding.LayoutpopBinding
import com.example.azakron.databinding.ModalTagBinding
import com.example.azakron.model.NoteModel

class Notes : AppCompatActivity() {

    private lateinit var binding: ActivityNotesBinding
    private lateinit var notesAdapter: NotesAdapter
    private val allNotes = mutableListOf<NoteModel>()
    private val displayedNotes = mutableListOf<NoteModel>()
    private val filteredNotes = mutableListOf<NoteModel>()

    private var currentPage = 0
    private val pageSize = 10
    private var isLoading = false
    private var isSearchActive = false
    private var currentSearchQuery = ""

    override fun onPause() {
        super.onPause()
        val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
        val lastScreen = prefs.getString("last_screen", null)

        if (lastScreen == "NewNote") {
            startActivity(Intent(this, NewNote::class.java))
        } else {
            prefs.edit().putString("last_screen", null).apply()
        }

        binding = ActivityNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearchBar()
        loadMockNotes()
        loadNextPage()

        binding.buttonAddNote.setOnClickListener { view ->
            showAddNotePopup(view)
        }
    }

    private fun setupSearchBar() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                currentSearchQuery = query

                // Mostrar/ocultar botão de limpar
                binding.clearSearchButton.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE

                if (query.isEmpty()) {
                    // Voltar para a lista completa
                    isSearchActive = false
                    displayedNotes.clear()
                    currentPage = 0
                    loadNextPage()
                    binding.emptyStateLayout.visibility = View.GONE
                } else {
                    // Realizar busca
                    isSearchActive = true
                    performSearch(query)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.clearSearchButton.setOnClickListener {
            binding.searchInput.text.clear()
            binding.searchInput.clearFocus()
        }
    }

    private fun performSearch(query: String) {
        val searchQuery = query.lowercase().trim()

        val newFilteredList = allNotes.filter { note ->
            // Buscar no título
            note.title.lowercase().contains(searchQuery) ||
                    // Buscar na descrição
                    note.description.lowercase().contains(searchQuery) ||
                    // Buscar nas tags
                    note.tags.any { tag -> tag.lowercase().contains(searchQuery) }
        }

        // Atualizar a lista de forma eficiente para evitar tremidas
        val oldSize = displayedNotes.size
        displayedNotes.clear()
        notesAdapter.notifyItemRangeRemoved(0, oldSize)

        displayedNotes.addAll(newFilteredList)
        notesAdapter.notifyItemRangeInserted(0, newFilteredList.size)

        // Mostrar/ocultar estado vazio
        binding.emptyStateLayout.visibility = if (newFilteredList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(displayedNotes) { note ->
            openNoteDetail(note)
        }

        val layoutManager = LinearLayoutManager(this)
        binding.recyclerViewNotes.apply {
            this.layoutManager = layoutManager
            adapter = notesAdapter
            setHasFixedSize(true)
        }

        // Scroll Listener para carregar mais itens (só funciona quando não está buscando)
        binding.recyclerViewNotes.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // Não carregar mais páginas se estiver buscando
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

    private fun loadMockNotes() {
        val sampleTitles = listOf(
            "Reunião de Planejamento",
            "Lista de Compras",
            "Ideias para o App",
            "Estudar Kotlin",
            "Aniversário da Maria",
            "Projeto Final",
            "Treino da Semana",
            "Receita de Bolo",
            "Metas do Mês",
            "Viagem de Férias"
        )

        val sampleDescriptions = listOf(
            "Discutir os próximos passos do projeto e definir as prioridades da sprint.",
            "Arroz, Feijão, Macarrão, Leite, Ovos, Café, Pão",
            "Implementar modo escuro, adicionar notificações push, melhorar a performance",
            "Revisar corrotinas, flows, e padrões de arquitetura MVVM",
            "Lembrar de comprar presente e organizar a festa surpresa",
            "Finalizar documentação e preparar apresentação",
            "Segunda: Peito e Tríceps, Quarta: Costas e Bíceps, Sexta: Pernas",
            "2 ovos, 1 xícara de açúcar, 1 xícara de leite, 2 xícaras de farinha",
            "Ler 2 livros, economizar R$ 500, fazer exercícios 3x por semana",
            "Destino: Praia, Período: Janeiro, Hotel: Resort Mar Azul"
        )

        val sampleTags = listOf(
            listOf("Trabalho", "Urgente"),
            listOf("Pessoal", "Casa"),
            listOf("Desenvolvimento", "Ideias"),
            listOf("Estudo", "Programação"),
            listOf("Pessoal", "Importante"),
            listOf("Trabalho", "Projeto"),
            listOf("Saúde", "Fitness"),
            listOf("Culinária", "Receitas"),
            listOf("Pessoal", "Objetivos"),
            listOf("Lazer", "Viagem")
        )

        allNotes.clear()
        for (i in 0 until 50) {
            val index = i % sampleTitles.size
            allNotes.add(
                NoteModel(
                    id = i + 1,
                    title = sampleTitles[index],
                    description = sampleDescriptions[index],
                    tags = sampleTags[index]
                )
            )
        }
        allNotes.get(1).title =  "Teste";

    }

    private fun loadNextPage() {
        if (isLoading || isSearchActive) return

        val startIndex = currentPage * pageSize
        val endIndex = minOf(startIndex + pageSize, allNotes.size)

        if (startIndex >= allNotes.size) return

        isLoading = true

        binding.recyclerViewNotes.postDelayed({
            val newNotes = allNotes.subList(startIndex, endIndex)
            val insertPosition = displayedNotes.size
            displayedNotes.addAll(newNotes)
            notesAdapter.notifyItemRangeInserted(insertPosition, newNotes.size)

            currentPage++
            isLoading = false
        }, 300)
    }

    private fun openNoteDetail(note: NoteModel) {
        val intent = Intent(this, Note::class.java).apply {
            putExtra("NOTE_ID", note.id)
            putExtra("NOTE_TITLE", note.title)
            putExtra("NOTE_DESCRIPTION", note.description)
            putStringArrayListExtra("NOTE_TAGS", ArrayList(note.tags))
        }
        startActivity(intent)
    }

    private fun showAddNotePopup(view: android.view.View) {
        val popupBinding = LayoutpopBinding.inflate(layoutInflater)

        val popupWindow = PopupWindow(
            popupBinding.root,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.elevation = 10f

        val xOffset = -280
        val yOffset = -460
        popupWindow.showAsDropDown(view, xOffset, yOffset)
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isOutsideTouchable = true

        popupBinding.optionCreateNote.setOnClickListener {
            startActivity(Intent(this, NewNote::class.java))
            finish()
            popupWindow.dismiss()
        }

        popupBinding.optionCreateTag.setOnClickListener {
            popupWindow.dismiss()
            showCreateTagDialog()
        }
    }

    private fun showCreateTagDialog() {
        val dialogBinding = ModalTagBinding.inflate(LayoutInflater.from(this))

        val dialog = AlertDialog.Builder(this, R.style.TransparentDialog)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.buttonSaveTag.setOnClickListener {
            val tagName = dialogBinding.tagInput.text.toString()
            if (tagName.isNotBlank()) {
                dialog.dismiss()
            }
        }

        dialogBinding.buttonCancelTag.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}