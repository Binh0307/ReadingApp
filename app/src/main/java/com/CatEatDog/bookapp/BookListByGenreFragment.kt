package com.CatEatDog.bookapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.CatEatDog.bookapp.adapters.BookAdapter
import com.CatEatDog.bookapp.models.ModelBook
import com.google.firebase.database.FirebaseDatabase


class BookListByGenreFragment : Fragment(R.layout.fragment_book_list_by_genre) {

    private lateinit var genreTitle: TextView
    private lateinit var booksRecyclerView: RecyclerView
    private lateinit var bookAdapter: BookAdapter
    //private lateinit var toolbar: androidx.appcompat.widget.Toolbar

    companion object {
        private const val ARG_GENRE_ID = "genre_id"
        private const val ARG_GENRE_NAME = "genre_name"

        fun newInstance(genreId: String, genreName: String): BookListByGenreFragment {
            val fragment = BookListByGenreFragment()
            val args = Bundle().apply {
                putString(ARG_GENRE_ID, genreId)
                putString(ARG_GENRE_NAME, genreName)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        genreTitle = view.findViewById(R.id.genreTitle)
        booksRecyclerView = view.findViewById(R.id.booksRecyclerView)

        // Set up the toolbar
       // (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        //(activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // RecyclerView setup
        booksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        bookAdapter = BookAdapter()
        booksRecyclerView.adapter = bookAdapter

        val genreId = requireArguments().getString(ARG_GENRE_ID) ?: return
        val genreName = requireArguments().getString(ARG_GENRE_NAME) ?: return

        genreTitle.text = genreName

        loadBooksForGenre(genreId)
    }

    private fun loadBooksForGenre(genreId: String) {
        val booksRef = FirebaseDatabase.getInstance().getReference("Books")
        booksRef.get().addOnSuccessListener { booksSnapshot ->
            val booksForGenre = mutableListOf<ModelBook>()
            for (bookSnapshot in booksSnapshot.children) {
                val book = bookSnapshot.getValue(ModelBook::class.java)
                book?.let {
                    if (it.genreIds.contains(genreId)) {
                        booksForGenre.add(it)
                    }
                }
            }
            bookAdapter.submitList(booksForGenre)
        }
    }
}
