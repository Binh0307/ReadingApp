package com.CatEatDog.bookapp.adapters
import android.content.Intent
import com.CatEatDog.bookapp.R

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.CatEatDog.bookapp.activities.BookDetailActivity
import com.CatEatDog.bookapp.models.ModelBook
import com.bumptech.glide.Glide

class BookAdapter : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    private var bookList: List<ModelBook> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = bookList[position]
        val bookId = book.id
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, BookDetailActivity::class.java)
            intent.putExtra("bookId", bookId)
            context.startActivity(intent)
        }
        holder.bind(book)
    }

    override fun getItemCount(): Int = bookList.size

    fun submitList(list: List<ModelBook>) {
        bookList = list
        notifyDataSetChanged()
    }

    inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val bookCover: ImageView = itemView.findViewById(R.id.bookCover)
        private val bookTitle: TextView = itemView.findViewById(R.id.bookTitle)

        fun bind(book: ModelBook) {
            bookTitle.text = book.title
            Glide.with(itemView.context)
                .load(book.coverUrl)
                .into(bookCover)
        }
    }
}
