package com.CatEatDog.bookapp.adapters
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.CatEatDog.bookapp.R
import com.CatEatDog.bookapp.activities.BookDetailActivity
import com.CatEatDog.bookapp.models.ModelBook
import com.bumptech.glide.Glide

class RecommendedBooksAdapter(
    private val context: Context,
    private val bookList: List<ModelBook>
) : RecyclerView.Adapter<RecommendedBooksAdapter.BookViewHolder>() {

    inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bookCoverIv: ImageView = itemView.findViewById(R.id.bookCover)
        val bookTitleTv: TextView = itemView.findViewById(R.id.bookTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = bookList[position]
        holder.bookTitleTv.text = book.title
        Glide.with(context).load(book.coverUrl).into(holder.bookCoverIv)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, BookDetailActivity::class.java)
            intent.putExtra("bookId", book.id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = bookList.size
}
