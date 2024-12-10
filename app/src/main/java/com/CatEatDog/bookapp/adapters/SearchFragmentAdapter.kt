package com.CatEatDog.bookapp.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.CatEatDog.bookapp.SearchByGenreFragment
import com.CatEatDog.bookapp.SearchByNameFragment

class SearchFragmentAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int = 2 // Two fragments to switch between

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SearchByGenreFragment() // First fragment: Search by Genre
            1 -> SearchByNameFragment()  // Second fragment: Search by Name
            else -> throw IllegalStateException("Invalid position")
        }
    }
}
