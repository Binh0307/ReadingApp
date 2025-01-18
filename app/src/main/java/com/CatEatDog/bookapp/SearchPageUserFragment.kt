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


class SearchPageUserFragment : Fragment(R.layout.fragment_search_page_user) {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var nestedFragmentContainer: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)
        nestedFragmentContainer = view.findViewById(R.id.nestedFragmentContainer)


        val adapter = SearchFragmentAdapter(childFragmentManager, lifecycle)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "By Genre"
                1 -> "By Name"
                else -> ""
            }
        }.attach()

        viewPager.setPageTransformer { page, position ->
            val scaleFactor = 1 - Math.abs(position) * 0.3f
            page.scaleX = scaleFactor
            page.scaleY = scaleFactor
        }


        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)

            }
        })
    }

    fun navigateToBookListByGenreFragment(genre: ModelGenre) {
        tabLayout.visibility = View.GONE
        viewPager.visibility = View.GONE

        // Show nested fragment container
        nestedFragmentContainer.visibility = View.VISIBLE

        val fragment = BookListByGenreFragment.newInstance(genre.id, genre.genre)
        childFragmentManager.beginTransaction()
            .replace(R.id.nestedFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    fun onBackPressed(): Boolean {
        val nestedFragment = childFragmentManager.findFragmentById(R.id.nestedFragmentContainer)
        return if (nestedFragment != null) {
            childFragmentManager.popBackStack()

            tabLayout.visibility = View.VISIBLE
            viewPager.visibility = View.VISIBLE
            nestedFragmentContainer.visibility = View.GONE

            true
        } else {
            false
        }
    }
}




