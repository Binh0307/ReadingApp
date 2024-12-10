package com.CatEatDog.bookapp.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.CatEatDog.bookapp.filters.FilterBookAdmin
import com.CatEatDog.bookapp.MyApplication
import com.CatEatDog.bookapp.activities.BookDetailActivity
import com.CatEatDog.bookapp.activities.PdfDetailActivity
import com.CatEatDog.bookapp.activities.PdfEditActivity
import com.CatEatDog.bookapp.databinding.RowBookAdminBinding
import com.CatEatDog.bookapp.models.ModelBook
import com.CatEatDog.bookapp.models.ModelGenre
import com.CatEatDog.bookapp.models.ModelPdf

class AdapterBookAdmin : RecyclerView.Adapter<AdapterBookAdmin.HolderBookAdmin>, Filterable {

    private var context: Context
    public var bookArrayList: ArrayList<ModelBook>
    private val filteredList: ArrayList<ModelBook>

    private lateinit var binding: RowBookAdminBinding

    // List of all genres
    var genreList: List<ModelGenre> = listOf()

    // Filter object
    private var filter: FilterBookAdmin? = null

    constructor(context: Context, bookArrayList: ArrayList<ModelBook>, genreList: List<ModelGenre>) : super() {
        this.context = context
        this.bookArrayList = bookArrayList
        this.filteredList = bookArrayList
        this.genreList = genreList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderBookAdmin {
        binding = RowBookAdminBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderBookAdmin(binding.root)
    }

    override fun onBindViewHolder(holder: HolderBookAdmin, position: Int) {
        val model = bookArrayList[position]
        val bookId = model.id
        val title = model.title
        val description = model.description
        val author = model.author
        val coverUrl = model.coverUrl
        val timestamp = model.timestamp
        val genreIds = model.genreIds

        holder.titleTv.text = title
        //holder.descriptionTv.text = description
        holder.authorTv.text = author
        holder.dateTv.text = MyApplication.formatTimeStamp(timestamp)

        // Load cover image
        MyApplication.loadImageFromUrl(coverUrl, holder.coverIv)

        // Get genres from genreIds
        val genres = genreIds.mapNotNull { genreId ->
            genreList.find { it.id == genreId }?.genre
        }.joinToString(", ")

        holder.genreTv.text = genres

         //Handle click: Show book details
        holder.itemView.setOnClickListener {
            val intent = Intent(context, BookDetailActivity::class.java)
            intent.putExtra("bookId", bookId)  // Passing bookId
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return bookArrayList.size
    }

    override fun getFilter(): Filter {
        if (filter == null) {
            filter = FilterBookAdmin(filteredList, this)
        }
        return filter as FilterBookAdmin
    }

    inner class HolderBookAdmin(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTv = binding.titleTv
        //val descriptionTv = binding.descriptionTv
        val authorTv = binding.authorTv
        val coverIv = binding.coverIv
        val dateTv = binding.dateTv
        val genreTv = binding.genreTv
    }
}


