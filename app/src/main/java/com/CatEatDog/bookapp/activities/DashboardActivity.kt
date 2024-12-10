package com.CatEatDog.bookapp.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.CatEatDog.bookapp.fragments.BookListFragment
import com.CatEatDog.bookapp.R
import com.CatEatDog.bookapp.fragments.GenreListFragment
import com.CatEatDog.bookapp.databinding.ActivityDashboardBinding

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)


        loadFragment(GenreListFragment())


        binding.leftButton.setOnClickListener {
            // Handle left button click
        }
        binding.centerButton.setOnClickListener {
            loadFragment(GenreListFragment())
        }

        binding.rightButton.setOnClickListener {
            loadFragment(BookListFragment())
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
