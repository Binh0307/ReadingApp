package com.CatEatDog.bookapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.CatEatDog.bookapp.adapters.BookAdapter
import com.CatEatDog.bookapp.models.ModelBook
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class FavoritePageUserFragment : Fragment(R.layout.fragment_favorite_page_user) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var bookAdapter: BookAdapter
    private val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.favoriteRecyclerView)

        // Set up GridLayoutManager (e.g., 4 columns)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)  // 4 columns
        bookAdapter = BookAdapter()
        recyclerView.adapter = bookAdapter

        // Load favorites for current user
        loadFavoriteBooks()
    }

    private fun loadFavoriteBooks() {
        val database = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("Favorites")
        database.get().addOnSuccessListener { snapshot ->
            val favoriteBookIds = snapshot.children.mapNotNull { it.key } // Get all bookIds

            if (favoriteBookIds.isNotEmpty()) {
                // Get book details for each favorite bookId
                loadBooksDetails(favoriteBookIds)
            }
        }
    }

    private fun loadBooksDetails(favoriteBookIds: List<String>) {
        val booksRef = FirebaseDatabase.getInstance().getReference("Books")
        booksRef.get().addOnSuccessListener { booksSnapshot ->
            val favoriteBooks = mutableListOf<ModelBook>()
            for (bookSnapshot in booksSnapshot.children) {
                val book = bookSnapshot.getValue(ModelBook::class.java)
                book?.let {
                    if (favoriteBookIds.contains(book.id)) {
                        favoriteBooks.add(it)
                    }
                }
            }
            bookAdapter.submitList(favoriteBooks)
        }
    }
}
