package com.CatEatDog.bookapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.CatEatDog.bookapp.activities.BookAddActivity
import com.CatEatDog.bookapp.adapters.AdapterBookAdmin
import com.CatEatDog.bookapp.adapters.SearchFragmentAdapter
import com.CatEatDog.bookapp.databinding.FragmentBookListBinding
import com.CatEatDog.bookapp.models.ModelBook
import com.CatEatDog.bookapp.models.ModelGenre
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


// SearchPageUserFragment.kt
class SearchPageUserFragment : Fragment(R.layout.fragment_search_page_user) {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var nestedFragmentContainer: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)
        nestedFragmentContainer = view.findViewById(R.id.nestedFragmentContainer)

        // Create an adapter to manage the two fragments
        val adapter = SearchFragmentAdapter(childFragmentManager, lifecycle)
        viewPager.adapter = adapter

        // Set up TabLayout to work with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "By Genre"
                1 -> "By Name"
                else -> ""
            }
        }.attach()
    }

    fun navigateToBookListByGenreFragment(genre: ModelGenre) {
        // Hide TabLayout and ViewPager2
        tabLayout.visibility = View.GONE
        viewPager.visibility = View.GONE

        // Show nested fragment container
        nestedFragmentContainer.visibility = View.VISIBLE

        // Replace with BookListByGenreFragment
        val fragment = BookListByGenreFragment.newInstance(genre.id, genre.genre)
        childFragmentManager.beginTransaction()
            .replace(R.id.nestedFragmentContainer, fragment)
            .addToBackStack(null)  // Add to back stack so we can pop it later
            .commit()
    }

    fun onBackPressed(): Boolean {
        // If we have a nested fragment
        val nestedFragment = childFragmentManager.findFragmentById(R.id.nestedFragmentContainer)
        return if (nestedFragment != null) {
            childFragmentManager.popBackStack()

            // Show TabLayout and ViewPager2 again
            tabLayout.visibility = View.VISIBLE
            viewPager.visibility = View.VISIBLE
            nestedFragmentContainer.visibility = View.GONE

            true
        } else {
            false
        }
    }
}




