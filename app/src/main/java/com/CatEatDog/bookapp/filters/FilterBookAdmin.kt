package com.CatEatDog.bookapp.filters

import android.widget.Filter
import com.CatEatDog.bookapp.adapters.AdapterBookAdmin
import com.CatEatDog.bookapp.models.ModelBook


class FilterBookAdmin  : Filter {

    var filterList: ArrayList<ModelBook>
    var adapterPdfAdmin: AdapterBookAdmin

    constructor(filterList: ArrayList<ModelBook>, adapterPdfAdmin: AdapterBookAdmin) {
        this.filterList = filterList
        this.adapterPdfAdmin = adapterPdfAdmin
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        var constraint: CharSequence? = constraint
        val results = FilterResults()

        if (constraint != null && constraint.isNotEmpty()) {
            constraint = constraint.toString().lowercase()
            val filteredModels = ArrayList<ModelBook>()
            for (i in filterList.indices) {
                if (filterList[i].title.lowercase().contains(constraint)) {
                    filteredModels.add(filterList[i])
                }
            }
            results.count = filteredModels.size
            results.values = filteredModels
        } else {
            results.count = filterList.size
            results.values = filterList
        }

        return  results

    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults) {
        adapterPdfAdmin.bookArrayList = results!!.values as ArrayList<ModelBook>

        adapterPdfAdmin.notifyDataSetChanged()
    }
}
