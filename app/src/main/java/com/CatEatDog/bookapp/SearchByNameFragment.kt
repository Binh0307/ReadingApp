package com.CatEatDog.bookapp

import android.app.DatePickerDialog
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.CatEatDog.bookapp.activities.BookAddActivity
import com.CatEatDog.bookapp.adapters.AdapterBookAdmin
import com.CatEatDog.bookapp.databinding.FragmentBookListBinding
import com.CatEatDog.bookapp.models.ModelBook
import com.CatEatDog.bookapp.models.ModelGenre
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SearchByNameFragment : Fragment() {
    private lateinit var binding: FragmentBookListBinding
    private lateinit var adapterBook: AdapterBookAdmin
    private var bookList = ArrayList<ModelBook>()
    private var genreList = ArrayList<ModelGenre>()
    private var lastAuthorFilter: String? = null
    private var selectedStartDate: Long? = null
    private var selectedEndDate: Long? = null
    private var filteredBookList = ArrayList<ModelBook>()

    private companion object {
        const val TAG = "BOOK_LIST_FRAGMENT"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBookListBinding.inflate(inflater, container, false)

        adapterBook = AdapterBookAdmin(requireContext(), bookList, genreList)

        binding.bookRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.bookRecyclerView.adapter = adapterBook

        loadBooks()


//        binding.searchEt.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
//
//            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
//                try {
//                    adapterBook.filter!!.filter(s)
//                } catch (e: Exception) {
//                    Log.d(TAG, "onTextChanged: ${e.message}")
//                }
//            }
//
//            override fun afterTextChanged(p0: Editable?) {}
//        })

        binding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    if (s.isNullOrEmpty()) {
                        adapterBook.bookArrayList = ArrayList(filteredBookList)
                    } else {
                        val filteredBooks = filteredBookList.filter {
                            it.title.contains(s, ignoreCase = true) || it.author.contains(s, ignoreCase = true)
                        }
                        adapterBook.bookArrayList = ArrayList(filteredBooks)
                    }
                    adapterBook.notifyDataSetChanged()
                } catch (e: Exception) {
                    Log.d(TAG, "onTextChanged: ${e.message}")
                }
            }

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                showSuggestions(query)
            }
        })




        binding.filterButton.setOnClickListener {
            showFilterDialog()
        }

        val sharedPreferences = requireContext().getSharedPreferences("BookAppPrefs", android.content.Context.MODE_PRIVATE)
        val savedAuthor = sharedPreferences.getString("filterAuthor", "") ?: ""
        val savedStartDate = sharedPreferences.getLong("filterStartDate", 0L)
        val savedEndDate = sharedPreferences.getLong("filterEndDate", 0L)
        val savedSortOption = sharedPreferences.getString("filterSortOption", "Title") ?: "Title"
        val savedCondition = sharedPreferences.getString("filterCondition", "AND") ?: "AND"
        val savedGenres = sharedPreferences.getStringSet("filterGenres", emptySet()) ?: emptySet()

        filterBooks(savedAuthor, savedGenres, savedSortOption, if (savedStartDate == 0L) null else savedStartDate, if (savedEndDate == 0L) null else savedEndDate, savedCondition)

        return binding.root
    }

    private fun showSuggestions(query: String) {
        val suggestions = filteredBookList.filter { it.title.contains(query, ignoreCase = true) }
            .map { it.title }

        val suggestionAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, suggestions)
        binding.searchEt.setAdapter(suggestionAdapter)
        binding.searchEt.threshold = 3
        binding.searchEt.showDropDown()
    }



    private fun loadBooks() {
        // Load books from Firebase
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                bookList.clear()
                for (ds in snapshot.children) {
                    val model = ds.getValue(ModelBook::class.java)
                    if (model != null) {
                        bookList.add(model)
                    }
                }
                adapterBook.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load books: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Load genres from Firebase
        val genreRef = FirebaseDatabase.getInstance().getReference("Genres")
        genreRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                genreList.clear()
                for (ds in snapshot.children) {
                    val genre = ds.getValue(ModelGenre::class.java)
                    if (genre != null) {
                        genreList.add(genre)
                    }
                }
                adapterBook.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load genres: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showFilterDialog() {
        val dialog = AlertDialog.Builder(requireContext())

        // Inflate the layout
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_filter_author, null)

        // Find views in the dialog
        val authorEt = dialogView.findViewById<EditText>(R.id.authorEt)
        val sortSpinner = dialogView.findViewById<Spinner>(R.id.sortSpinner)
        val selectedStartDateTv = dialogView.findViewById<TextView>(R.id.selectedStartDateTv)
        val selectedEndDateTv = dialogView.findViewById<TextView>(R.id.selectedEndDateTv)
        val conditionRadioGroup = dialogView.findViewById<RadioGroup>(R.id.conditionRadioGroup)
        val genreTextView = dialogView.findViewById<TextView>(R.id.genreTextView)

        // Pre-fill fields with saved values
        val sharedPreferences = requireContext().getSharedPreferences("BookAppPrefs", android.content.Context.MODE_PRIVATE)
        val savedAuthor = sharedPreferences.getString("filterAuthor", "") ?: ""
        val savedStartDate = sharedPreferences.getLong("filterStartDate", 0L)
        val savedEndDate = sharedPreferences.getLong("filterEndDate", 0L)
        val savedSortOption = sharedPreferences.getString("filterSortOption", "Title") ?: "Title"
        val savedCondition = sharedPreferences.getString("filterCondition", "AND") ?: "AND"
        val savedGenres = sharedPreferences.getStringSet("filterGenres", emptySet()) ?: emptySet()

        // Set saved values in the dialog
        authorEt.setText(savedAuthor)
        if (savedStartDate != 0L) selectedStartDateTv.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(savedStartDate))
        if (savedEndDate != 0L) selectedEndDateTv.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(savedEndDate))
        val sortAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, arrayOf("Title", "Newest ","Oldest", "Author","Most Viewed","Least Viewed" ))
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortSpinner.adapter = sortAdapter
        sortSpinner.setSelection(sortAdapter.getPosition(savedSortOption))
        conditionRadioGroup.check(if (savedCondition == "AND") R.id.andRadioButton else R.id.orRadioButton)
        genreTextView.text = savedGenres.joinToString(", ")

        // Genre selection
        val genreNames = genreList.map { it.genre }.toTypedArray()
        val selectedGenres = savedGenres.toMutableSet()

        genreTextView.setOnClickListener {
            val selectedItems = BooleanArray(genreNames.size) { selectedGenres.contains(genreNames[it]) }
            AlertDialog.Builder(requireContext())
                .setTitle("Select Genres")
                .setMultiChoiceItems(genreNames, selectedItems) { _, which, isChecked ->
                    if (isChecked) selectedGenres.add(genreNames[which]) else selectedGenres.remove(genreNames[which])
                }
                .setPositiveButton("OK") { _, _ -> genreTextView.text = selectedGenres.joinToString(", ") }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Date pickers
        selectedStartDateTv.setOnClickListener { showDatePicker(selectedStartDateTv, true) }
        selectedEndDateTv.setOnClickListener { showDatePicker(selectedEndDateTv, false) }

        // Set dialog buttons
        dialog.setView(dialogView)
            .setTitle("Filter")
            .setPositiveButton("Apply") { _, _ ->
                val author = authorEt.text.toString().trim()
                val sortOption = sortSpinner.selectedItem.toString()
                val condition = when (conditionRadioGroup.checkedRadioButtonId) {
                    R.id.andRadioButton -> "AND"
                    else -> "OR"
                }
                val startDateFilter = selectedStartDate
                val endDateFilter = selectedEndDate

                // Save filter settings
                sharedPreferences.edit().apply {
                    putString("filterAuthor", author)
                    putLong("filterStartDate", startDateFilter ?: 0L)
                    putLong("filterEndDate", endDateFilter ?: 0L)
                    putString("filterSortOption", sortOption)
                    putString("filterCondition", condition)
                    putStringSet("filterGenres", selectedGenres)
                    apply()
                }

                // Apply filters
                filterBooks(author, selectedGenres, sortOption, startDateFilter, endDateFilter, condition)
            }
            .setNeutralButton("Reset") { _, _ -> resetFilters() }
            .setNegativeButton("Cancel", null)
            .show()
    }




    private fun resetFilters() {
        lastAuthorFilter = null
        selectedStartDate = null
        selectedEndDate = null

        filteredBookList = ArrayList(bookList)

        adapterBook.bookArrayList = ArrayList(bookList)
        adapterBook.notifyDataSetChanged()

        Toast.makeText(requireContext(), "Filters reset", Toast.LENGTH_SHORT).show()
    }





    private fun showDatePicker(selectedDateTv: TextView, isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            val selectedDateMillis = calendar.timeInMillis
            if (isStartDate) {
                selectedStartDate = selectedDateMillis
                selectedDateTv.text = "${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)}"
            } else {
                selectedEndDate = selectedDateMillis
                selectedDateTv.text = "${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)}"
            }
        }

        DatePickerDialog(
            requireContext(),
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun filterBooks(
        author: String,
        selectedGenres: Set<String>,
        sortOption: String,
        startDateFilter: Long?,
        endDateFilter: Long?,
        condition: String
    ) {
        // Filter by author
        var filteredBooks = bookList.filter { it.author.contains(author, ignoreCase = true) }

        // Filter by selected genres
        if (selectedGenres.isNotEmpty()) {
            val selectedGenreIds = genreList.filter { selectedGenres.contains(it.genre) }.map { it.id }

            filteredBooks = if (condition == "AND") {
                filteredBooks.filter { selectedGenreIds.all { genreId -> it.genreIds.contains(genreId) } }
            } else {
                filteredBooks.filter { selectedGenreIds.any { genreId -> it.genreIds.contains(genreId) } }
            }
        }

        // Filter by date range if selected
        if (startDateFilter != null && endDateFilter != null) {
            filteredBooks = filteredBooks.filter { it.timestamp in startDateFilter..endDateFilter }
        }

        // Sort the filtered books
        filteredBooks = when (sortOption) {
            "Title" -> filteredBooks.sortedBy { it.title }
            "Newest" -> filteredBooks.sortedByDescending { it.timestamp }
            "Oldest" -> filteredBooks.sortedBy { it.timestamp }
            "Author" -> filteredBooks.sortedBy { it.author }
            "Most Viewed" -> filteredBooks.sortedByDescending { it.viewCount }
            "Least Viewed" -> filteredBooks.sortedBy { it.viewCount }
            else -> filteredBooks
        }


        // Update the adapter with the filtered and sorted books
        adapterBook.bookArrayList = ArrayList(filteredBooks)
        adapterBook.notifyDataSetChanged()

        // Save the last applied author filter to SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("BookAppPrefs", android.content.Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("lastAuthorFilter", author).apply()

        filteredBookList = ArrayList(filteredBooks)
    }


}

