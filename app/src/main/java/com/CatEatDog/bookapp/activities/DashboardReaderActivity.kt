package com.CatEatDog.bookapp.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.CatEatDog.bookapp.FavoritePageUserFragment
import com.CatEatDog.bookapp.fragments.BookListFragment
import com.CatEatDog.bookapp.R
import com.CatEatDog.bookapp.SearchPageUserFragment
import com.CatEatDog.bookapp.fragments.GenreListFragment
import com.CatEatDog.bookapp.databinding.ActivityDashboardReaderBinding

class DashboardReaderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardReaderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)


        loadFragment(GenreListFragment())


        binding.hot.setOnClickListener {
            // Handle left button click
        }
        binding.search.setOnClickListener {
            loadFragment(SearchPageUserFragment())
        }

        binding.fav.setOnClickListener {
            loadFragment(FavoritePageUserFragment())
        }

        binding.profile.setOnClickListener {
            //loadFragment(BookListFragment())
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
