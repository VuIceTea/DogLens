package com.example.doglens.ui.favorite

import android.os.Bundle
import android.view.View
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doglens.R
import com.example.doglens.data.adapter.FavoriteDogAdapter
import com.example.doglens.data.model.FavoriteDogItem
import com.example.doglens.data.repository.FavoriteRepository
import java.util.Locale

class FavoriteFragment : Fragment(R.layout.fragment_favorite) {

    private val repository by lazy { FavoriteRepository() }

    private lateinit var favoriteAdapter: FavoriteDogAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInput: EditText
    private lateinit var countText: TextView
    private lateinit var emptyText: TextView
    private lateinit var sortSpinner: Spinner

    private var selectedSize = "all"
    private var selectedSort = 0

    private var allFavorites: List<FavoriteDogItem> = emptyList()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvFavoriteDogs)
        searchInput = view.findViewById(R.id.txtSeach)
        countText = view.findViewById(R.id.txtFavoriteCount)
        emptyText = view.findViewById(R.id.txtFavoriteEmpty)
        sortSpinner = view.findViewById(R.id.spinnerFavoriteSort)

        allFavorites = repository.getFavoriteItems()

        setupRecyclerView()
        setupSortSpinner()
        setupFilters(view)
        setupSearch()

        renderFavorites()
    }

    private fun setupRecyclerView() {
        favoriteAdapter = FavoriteDogAdapter(
            onDogClick = { item ->
                Toast.makeText(
                    requireContext(),
                    item.dog.name,
                    Toast.LENGTH_SHORT
                ).show()
            },
            onFavoriteClick = { item ->
                repository.removeFavorite(item.dog.id)
                allFavorites = repository.getFavoriteItems()
                renderFavorites()
            },
        )

        recyclerView.layoutManager = GridLayoutManager(
            requireContext(),
            2
        )

        recyclerView.addItemDecoration(
            GridSpacingItemDecoration(
                spanCount = 2,
                spacing = (12 * resources.displayMetrics.density).toInt()
            )
        )

        recyclerView.adapter = favoriteAdapter
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.setHasFixedSize(false)
    }

    private fun setupSortSpinner() {
        val options = listOf("Mới nhất", "Tên A-Z")

        val spinnerAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        sortSpinner.adapter = spinnerAdapter

        sortSpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedSort = position
                    renderFavorites()
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            }
    }

    private fun setupFilters(view: View) {
        val filterAll = view.findViewById<View>(R.id.filterAll)
        val filterSmall = view.findViewById<View>(R.id.filterSmall)
        val filterMedium = view.findViewById<View>(R.id.filterMedium)
        val filterLarge = view.findViewById<View>(R.id.filterLarge)

        filterAll.setOnClickListener {
            selectedSize = "all"
            updateFilterStyles(view)
            renderFavorites()
        }

        filterSmall.setOnClickListener {
            selectedSize = "small"
            updateFilterStyles(view)
            renderFavorites()
        }

        filterMedium.setOnClickListener {
            selectedSize = "medium"
            updateFilterStyles(view)
            renderFavorites()
        }

        filterLarge.setOnClickListener {
            selectedSize = "large"
            updateFilterStyles(view)
            renderFavorites()
        }

        updateFilterStyles(view)
    }

    private fun updateFilterStyles(view: View) {
        val filterIds = listOf(
            R.id.filterAll to "all",
            R.id.filterSmall to "small",
            R.id.filterMedium to "medium",
            R.id.filterLarge to "large"
        )

        filterIds.forEach { (id, size) ->
            val button = view.findViewById<com.google.android.material.button.MaterialButton>(id)

            val isSelected = selectedSize == size

            button.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor(
                        if (isSelected) "#1585FD" else "#E5EBF4"
                    )
                )
            )

            button.setTextColor(android.graphics.Color.parseColor(if (isSelected) "#FFFFFF" else "#526581"))

            button.strokeWidth = if (isSelected) 0 else (resources.displayMetrics.density).toInt()

            button.strokeColor =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#D7E0ED"))
        }
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(
            object : android.text.TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    renderFavorites()
                }

                override fun afterTextChanged(s: android.text.Editable?) = Unit
            }
        )

        searchInput.setOnEditorActionListener { _, actionId, event ->
            val isSearch = actionId == EditorInfo.IME_ACTION_SEARCH
            val isEnter =
                event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
            isSearch || isEnter
        }
    }

    private fun renderFavorites() {
        val keyword = searchInput.text.toString().trim().lowercase(Locale.getDefault())

        var result = allFavorites.filter { item ->
            val matchesKeyword = item.dog.name.lowercase(Locale.getDefault())
                .contains(keyword) || item.dog.description.lowercase(Locale.getDefault())
                .contains(keyword)

            val matchesSize = selectedSize == "all" || item.dog.sizeCategoryId == selectedSize

            matchesKeyword && matchesSize
        }

        result = when (selectedSort) {
            1 -> result.sortedBy {
                it.dog.name.lowercase(Locale.getDefault())
            }

            else -> result.sortedByDescending {
                it.favorite.addedAt
            }
        }

        favoriteAdapter.submitLists(result)

        countText.text = "${allFavorites.size} giống chó"
        emptyText.visibility = if (result.isEmpty()) View.VISIBLE else View.GONE

        recyclerView.visibility = if (result.isEmpty()) View.GONE else View.VISIBLE
    }
}