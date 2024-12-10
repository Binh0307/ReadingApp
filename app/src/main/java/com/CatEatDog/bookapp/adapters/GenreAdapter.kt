package com.CatEatDog.bookapp.adapters
import com.CatEatDog.bookapp.R
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.CatEatDog.bookapp.models.ModelBook
import com.CatEatDog.bookapp.models.ModelGenre
import com.google.firebase.database.core.view.View

class GenreAdapter(
    private val onSeeMoreClicked: (ModelGenre) -> Unit
) : RecyclerView.Adapter<GenreAdapter.GenreViewHolder>() {

    private var genreWithBooksList: List<GenreWithBooks> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_genre, parent, false)
        return GenreViewHolder(view)
    }

    override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
        val genreWithBooks = genreWithBooksList[position]
        holder.bind(genreWithBooks)
    }

    override fun getItemCount(): Int = genreWithBooksList.size

    fun submitList(list: List<GenreWithBooks>) {
        genreWithBooksList = list
        notifyDataSetChanged()
    }

    inner class GenreViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val genreTitle: TextView = itemView.findViewById(R.id.genreTitle)
        private val booksRecyclerView: RecyclerView = itemView.findViewById(R.id.booksRecyclerView)
        private val seeMoreButton: TextView = itemView.findViewById(R.id.seeMoreButton) // Add a TextView for "See More"

        fun bind(genreWithBooks: GenreWithBooks) {
            genreTitle.text = genreWithBooks.genre.genre

            // Set up horizontal RecyclerView for books
            booksRecyclerView.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            val bookAdapter = BookAdapter()
            booksRecyclerView.adapter = bookAdapter
            bookAdapter.submitList(genreWithBooks.books)

            // Handle "See More" button click
            seeMoreButton.setOnClickListener {
                onSeeMoreClicked(genreWithBooks.genre)
            }
        }
    }
}


