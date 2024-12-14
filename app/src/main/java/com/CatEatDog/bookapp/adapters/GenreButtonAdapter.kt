package com.CatEatDog.bookapp.adapters

import com.CatEatDog.bookapp.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.CatEatDog.bookapp.models.ModelGenre

class GenreButtonAdapter(private val onGenreSelected: (ModelGenre) -> Unit) :
    RecyclerView.Adapter<GenreButtonAdapter.GenreViewHolder>() {

    private val genres = mutableListOf<ModelGenre>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_genre_button, parent, false)
        return GenreViewHolder(view)
    }

    override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
        val genre = genres[position]
        holder.bind(genre)
        holder.itemView.setOnClickListener { onGenreSelected(genre) }
    }

    override fun getItemCount(): Int = genres.size

    fun submitList(newGenres: List<ModelGenre>) {
        genres.clear()
        genres.addAll(newGenres)
        notifyDataSetChanged()
    }

    class GenreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val genreButton: Button = itemView.findViewById(R.id.genreButton)

        fun bind(genre: ModelGenre) {
            genreButton.text = genre.genre
        }
    }
}
