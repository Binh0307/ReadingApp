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

    private companion object {
        const val TAG = "BOOK_LIST_FRAGMENT"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBookListBinding.inflate(inflater, container, false)

        // Initialize adapter with bookList and genreList
        adapterBook = AdapterBookAdmin(requireContext(), bookList, genreList)

        // Set up RecyclerView
        binding.bookRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.bookRecyclerView.adapter = adapterBook

        // Load books and genres
        loadBooks()

        // Retrieve the last applied author filter from SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("BookAppPrefs", android.content.Context.MODE_PRIVATE)
        lastAuthorFilter = sharedPreferences.getString("lastAuthorFilter", "")

        // Pre-fill the search bar with the last author filter
        binding.searchEt.setText(lastAuthorFilter)

        // Search by title listener
        binding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                try {
                    adapterBook.filter!!.filter(s)
                } catch (e: Exception) {
                    Log.d(TAG, "onTextChanged: ${e.message}")
                }
            }

            override fun afterTextChanged(p0: Editable?) {}
        })

        // Filter by author button listener
        binding.filterButton.setOnClickListener {
            showFilterDialog()
        }

        return binding.root
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

        // Pre-fill the EditText with the last applied author filter
        authorEt.setText(lastAuthorFilter)

        // Setup the Sort Spinner
        val sortOptions = arrayOf("Title", "Date", "Author")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortSpinner.adapter = adapter

        // Setup the Date Picker for start date
        selectedStartDateTv.setOnClickListener {
            showDatePicker(selectedStartDateTv, true) // Pass 'true' for start date
        }

        // Setup the Date Picker for end date
        selectedEndDateTv.setOnClickListener {
            showDatePicker(selectedEndDateTv, false) // Pass 'false' for end date
        }

        // Show selected dates if already set
        if (selectedStartDate != null) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            selectedStartDateTv.text = "${dateFormat.format(Date(selectedStartDate!!))}"
        }
        if (selectedEndDate != null) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            selectedEndDateTv.text = "${dateFormat.format(Date(selectedEndDate!!))}"
        }

        // Set up the dialog buttons
        dialog.setView(dialogView)
            .setTitle("Filter")
            .setPositiveButton("Apply") { _, _ ->
                val author = authorEt.text.toString().trim()
                val sortOption = sortSpinner.selectedItem.toString()
                val startDateFilter = selectedStartDate
                val endDateFilter = selectedEndDate

                // Apply filter
                filterBooks(author, sortOption, startDateFilter, endDateFilter)
            }
            .setNegativeButton("Cancel", null)
            .show()
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

    private fun filterBooks(author: String, sortOption: String, startDateFilter: Long?, endDateFilter: Long?) {
        // Filter by author
        var filteredBooks = bookList.filter { it.author.contains(author, ignoreCase = true) }

        // Filter by date range if selected
        if (startDateFilter != null && endDateFilter != null) {
            filteredBooks = filteredBooks.filter {
                it.timestamp in startDateFilter..endDateFilter
            }
        }

        // Sort the filtered books
        filteredBooks = when (sortOption) {
            "Title" -> filteredBooks.sortedBy { it.title }
            "Date" -> filteredBooks.sortedBy { it.timestamp }
            "Author" -> filteredBooks.sortedBy { it.author }
            else -> filteredBooks
        }

        // Update the adapter with the filtered and sorted books
        adapterBook.bookArrayList = ArrayList(filteredBooks)
        adapterBook.notifyDataSetChanged()

        // Save the last applied author filter to SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("BookAppPrefs", android.content.Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("lastAuthorFilter", author).apply()
    }
}

