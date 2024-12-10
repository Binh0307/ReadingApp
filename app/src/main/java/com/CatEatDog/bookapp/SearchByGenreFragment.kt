package com.CatEatDog.bookapp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.CatEatDog.bookapp.adapters.GenreAdapter
import com.CatEatDog.bookapp.adapters.GenreWithBooks
import com.CatEatDog.bookapp.models.ModelBook
import com.CatEatDog.bookapp.models.ModelGenre
import com.google.firebase.database.FirebaseDatabase

// SearchByGenreFragment.kt
// SearchByGenreFragment.kt
class SearchByGenreFragment : Fragment(R.layout.fragment_search_by_genre) {

    private lateinit var genreRecyclerView: RecyclerView
    private lateinit var genreAdapter: GenreAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        genreRecyclerView = view.findViewById(R.id.genreRecyclerView)
        genreRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // SearchByGenreFragment.kt
        genreAdapter = GenreAdapter { selectedGenre ->
            val parentFragment = parentFragment as? SearchPageUserFragment
            parentFragment?.navigateToBookListByGenreFragment(selectedGenre) ?: run {
                Log.e("SearchByGenreFragment", "Parent fragment is not initialized yet.")
            }
        }

        genreRecyclerView.adapter = genreAdapter

        loadGenresAndBooks()
    }

    private fun loadGenresAndBooks() {
        val database = FirebaseDatabase.getInstance()
        val genresRef = database.getReference("Genres")
        val booksRef = database.getReference("Books")

        genresRef.get().addOnSuccessListener { genresSnapshot ->
            val genres = mutableListOf<ModelGenre>()
            for (genreSnapshot in genresSnapshot.children) {
                val genre = genreSnapshot.getValue(ModelGenre::class.java)
                genre?.let { genres.add(it) }
            }

            booksRef.get().addOnSuccessListener { booksSnapshot ->
                val books = mutableListOf<ModelBook>()
                for (bookSnapshot in booksSnapshot.children) {
                    val book = bookSnapshot.getValue(ModelBook::class.java)
                    book?.let { books.add(it) }
                }

                val genreWithBooksList = genres.map { genre ->
                    val booksForGenre = books.filter { book -> book.genreIds.contains(genre.id) }
                    GenreWithBooks(genre, booksForGenre.toMutableList())
                }

                genreAdapter.submitList(genreWithBooksList)
            }
        }
    }
}


