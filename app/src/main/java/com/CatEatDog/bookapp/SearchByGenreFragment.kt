package com.CatEatDog.bookapp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.CatEatDog.bookapp.adapters.GenreAdapter
import com.CatEatDog.bookapp.adapters.GenreButtonAdapter
import com.CatEatDog.bookapp.adapters.GenreWithBooks
import com.CatEatDog.bookapp.models.ModelBook
import com.CatEatDog.bookapp.models.ModelGenre
import com.google.firebase.database.FirebaseDatabase


class SearchByGenreFragment : Fragment(R.layout.fragment_search_by_genre) {

    private lateinit var genreRecyclerView: RecyclerView
    private lateinit var genreButtonAdapter: GenreButtonAdapter
    private lateinit var genreButtonFlow: Flow
    private lateinit var genreAdapter: GenreAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find Flow layout for genre buttons
        genreButtonFlow = view.findViewById(R.id.genreFlow)

        // Set up RecyclerView for genres and books
        genreRecyclerView = view.findViewById(R.id.genreRecyclerView)
        genreRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Set up genre adapter for the RecyclerView
        genreAdapter = GenreAdapter { selectedGenre ->
            val parentFragment = parentFragment as? SearchPageUserFragment
            parentFragment?.navigateToBookListByGenreFragment(selectedGenre) ?: run {
                Log.e("SearchByGenreFragment", "Parent fragment is not initialized yet.")
            }
        }
        genreRecyclerView.adapter = genreAdapter

        // Load genres and books
        loadGenresAndBooks()
        //loadGenres()
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

    private fun loadGenres() {
        val database = FirebaseDatabase.getInstance()
        val genresRef = database.getReference("Genres")

        genresRef.get().addOnSuccessListener { genresSnapshot ->
            val genres = mutableListOf<ModelGenre>()
            for (genreSnapshot in genresSnapshot.children) {
                val genre = genreSnapshot.getValue(ModelGenre::class.java)
                genre?.let { genres.add(it) }
            }

            // Prepare to dynamically add buttons
            val parentLayout = genreButtonFlow.parent as ConstraintLayout
            val buttonIds = mutableListOf<Int>()

            // Create buttons dynamically for each genre
            for (genre in genres) {
                val button = AppCompatButton(requireContext()).apply {
                    text = genre.genre
                    id = View.generateViewId() // Generate a unique ID
                    setBackgroundResource(R.drawable.genre_button_background)
                    setTextColor(resources.getColor(R.color.white, null))
                    setPadding(8, 8, 8, 8)

                    // Handle button click to navigate to the fragment
                    setOnClickListener {
                        navigateToBookListByGenreFragment(genre)
                    }
                }

                // Add button to the parent layout and update Flow
                parentLayout.addView(button)
                buttonIds.add(button.id)
            }

            // Set referenced IDs for the Flow
            genreButtonFlow.referencedIds = buttonIds.toIntArray()
        }
    }



    private fun navigateToBookListByGenreFragment(selectedGenre: ModelGenre) {
        // Check if the fragment container exists
        val fragmentContainer = view?.findViewById<FrameLayout>(R.id.nestedFragmentContainer)

        if (fragmentContainer != null) {
            Log.d("FragmentTransaction", "Fragment container is ready")

            // Create the fragment and perform the transaction
            val fragment = BookListByGenreFragment.newInstance(selectedGenre.id, selectedGenre.genre)
            parentFragmentManager.beginTransaction()
                .replace(R.id.nestedFragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        } else {
            Log.e("FragmentTransaction", "No fragment container found.")
        }
    }


}



