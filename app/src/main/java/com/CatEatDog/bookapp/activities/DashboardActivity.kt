package com.CatEatDog.bookapp.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.CatEatDog.bookapp.fragments.BookListFragment
import com.CatEatDog.bookapp.R
import com.CatEatDog.bookapp.UserSettingFragment
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
            highlightSelectedTab(it)
            loadFragment(GenreListFragment())
        }
        binding.centerButton.setOnClickListener {
            highlightSelectedTab(it)
            loadFragment(BookListFragment())
        }

        binding.rightButton.setOnClickListener {
            highlightSelectedTab(it)
            loadFragment(UserSettingFragment())
        }
    }

    private fun highlightSelectedTab(selectedView: View) {
        binding.leftButton.isSelected = false
        binding.centerButton.isSelected = false
        binding.rightButton.isSelected = false

        selectedView.isSelected = true
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
