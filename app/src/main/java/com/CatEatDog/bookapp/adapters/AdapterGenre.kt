package com.CatEatDog.bookapp.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.CatEatDog.bookapp.filters.FilterGenre
import com.CatEatDog.bookapp.models.ModelGenre
import com.CatEatDog.bookapp.activities.PdfListAdminActivity
import com.CatEatDog.bookapp.databinding.RowGenreBinding
import com.google.firebase.database.FirebaseDatabase

class AdapterGenre :RecyclerView.Adapter<AdapterGenre.HolderGenre> , Filterable{
    private val context:Context
    public var genreArrayList: ArrayList<ModelGenre>
    private var filterList:ArrayList<ModelGenre>

    private var filter: FilterGenre?= null

    private lateinit var binding: RowGenreBinding

    constructor(context: Context ,genreArrayList: ArrayList<ModelGenre> ) {
        this.genreArrayList = genreArrayList
        this.context = context
        this.filterList = genreArrayList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderGenre {
        binding = RowGenreBinding.inflate(LayoutInflater.from(context),parent,false)
        return HolderGenre(binding.root)
    }

    override fun getItemCount(): Int {
        return  genreArrayList.size
    }

    override fun onBindViewHolder(holder: HolderGenre, position: Int) {
        // Get data, set data
        // get data
        val model = genreArrayList[position]
        val id = model.id
        val genre = model.genre
        val uid = model.uid
        val timestamp = model.timestamp

        //setdata
        holder.genreTv.text = genre


        holder.deleteBtn.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Delete")
                .setMessage("Are you sure want to delete this genre?")
                .setPositiveButton("Confirm"){a, d->
                    Toast.makeText(context, "deleting...", Toast.LENGTH_SHORT).show()
                    deleteGenre(model,holder)
                }
                .setNegativeButton("Cancel") {a, d->
                    a.dismiss()
                }
                .show()
        }

        // handle click start pdf list admin
//        holder.itemView.setOnClickListener {
//            val intent = Intent(context, PdfListAdminActivity::class.java)
//            intent.putExtra("genreId", id)
//            intent.putExtra("genre", genre)
//            context.startActivity(intent)
//        }

    }

    private fun deleteGenre(model: ModelGenre, holder: HolderGenre) {
        val id = model.id

        val ref = FirebaseDatabase.getInstance().getReference("Genres")
        ref.child(id)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {e->
                Toast.makeText(context, "unable to dlete due to ${e.message}", Toast.LENGTH_SHORT).show()
            }

    }


    inner  class  HolderGenre(itemView:android.view.View): RecyclerView.ViewHolder(itemView) {
        var genreTv:TextView = binding.genreTv
        var deleteBtn:ImageButton = binding.deleteBtn
    }

    override fun getFilter():Filter {
        if (filter == null) {
            filter = FilterGenre(filterList, this)
        }
        return  filter as FilterGenre

    }


}