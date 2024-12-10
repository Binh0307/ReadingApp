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
import com.CatEatDog.bookapp.filters.FilterCategory
import com.CatEatDog.bookapp.models.ModelCategory
import com.CatEatDog.bookapp.activities.PdfListAdminActivity
import com.CatEatDog.bookapp.databinding.RowCategoryBinding
import com.google.firebase.database.FirebaseDatabase

class AdapterCategory :RecyclerView.Adapter<AdapterCategory.HolderCategory> , Filterable{
    private val context:Context
    public var categoryArrayList: ArrayList<ModelCategory>
    private var filterList:ArrayList<ModelCategory>

    private var filter: FilterCategory?= null

    private lateinit var binding: RowCategoryBinding

    constructor(context: Context ,categoryArrayList: ArrayList<ModelCategory> ) {
        this.categoryArrayList = categoryArrayList
        this.context = context
        this.filterList = categoryArrayList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderCategory {
        //inflare bind row category
        binding = RowCategoryBinding.inflate(LayoutInflater.from(context),parent,false)
        return HolderCategory(binding.root)
    }

    override fun getItemCount(): Int {
        return  categoryArrayList.size
    }

    override fun onBindViewHolder(holder: HolderCategory, position: Int) {
        // Get data, set data
        // get data
        val model = categoryArrayList[position]
        val id = model.id
        val category = model.category
        val uid = model.uid
        val timestamp = model.timestamp

        //setdata
        holder.categoryTv.text = category

        // delte category
        holder.deleteBtn.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Delete")
                .setMessage("Are you sure want to delete this category?")
                .setPositiveButton("Confirm"){a, d->
                    Toast.makeText(context, "deleting...", Toast.LENGTH_SHORT).show()
                    deleteCategory(model,holder)
                }
                .setNegativeButton("Cancel") {a, d->
                    a.dismiss()
                }
                .show()
        }

        // handle click start pdf list admin
        holder.itemView.setOnClickListener {
            val intent = Intent(context, PdfListAdminActivity::class.java)
            intent.putExtra("categoryId", id)
            intent.putExtra("category", category)
            context.startActivity(intent)
        }

    }

    private fun deleteCategory(model: ModelCategory, holder: HolderCategory) {
        val id = model.id

        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.child(id)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {e->
                Toast.makeText(context, "unable to dlete due to ${e.message}", Toast.LENGTH_SHORT).show()
            }

    }


    inner  class  HolderCategory(itemView:android.view.View): RecyclerView.ViewHolder(itemView) {
        var categoryTv:TextView = binding.categoryTv
        var deleteBtn:ImageButton = binding.deleteBtn
    }

    override fun getFilter():Filter {
        if (filter == null) {
            filter = FilterCategory(filterList, this)
        }
        return  filter as FilterCategory

    }


}