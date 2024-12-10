package com.CatEatDog.bookapp.filters

import android.widget.Filter
import com.CatEatDog.bookapp.adapters.AdapterCategory
import com.CatEatDog.bookapp.models.ModelCategory

class FilterCategory:Filter {
    private var filterList: ArrayList<ModelCategory>

    private var adapterCategory: AdapterCategory

    constructor(filterList: ArrayList<ModelCategory>, adapterCategory: AdapterCategory) {
        this.filterList = filterList
        this.adapterCategory = adapterCategory
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        val results = FilterResults()

        if (constraint != null && constraint.isNotEmpty()) {
            val filteredModel: ArrayList<ModelCategory> = ArrayList()
            val filterPattern = constraint.toString().uppercase()

            for (item in filterList) {
                if (item.category.uppercase().contains(filterPattern)) {
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
        adapterCategory.categoryArrayList = results.values as ArrayList<ModelCategory>

        adapterCategory.notifyDataSetChanged()
    }
}