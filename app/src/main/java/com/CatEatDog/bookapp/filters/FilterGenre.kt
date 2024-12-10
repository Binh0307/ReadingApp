package com.CatEatDog.bookapp.filters

import android.widget.Filter
import com.CatEatDog.bookapp.adapters.AdapterGenre
import com.CatEatDog.bookapp.models.ModelGenre

class FilterGenre:Filter {
    private var filterList: ArrayList<ModelGenre>

    private var adapterGenre: AdapterGenre

    constructor(filterList: ArrayList<ModelGenre>, adapterGenre: AdapterGenre) {
        this.filterList = filterList
        this.adapterGenre = adapterGenre
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        val results = FilterResults()

        if (constraint != null && constraint.isNotEmpty()) {
            val filteredModel: ArrayList<ModelGenre> = ArrayList()
            val filterPattern = constraint.toString().uppercase()

            for (item in filterList) {
                if (item.genre.uppercase().contains(filterPattern)) {
                    filteredModel.add(item)
                }
            }

            results.count = filteredModel.size
            results.values = filteredModel
        } else {
            results.count = filterList.size
            results.values = filterList
        }

        return results
    }


    override fun publishResults(constraint: CharSequence?, results: FilterResults) {
        adapterGenre.genreArrayList = results.values as ArrayList<ModelGenre>

        adapterGenre.notifyDataSetChanged()
    }
}