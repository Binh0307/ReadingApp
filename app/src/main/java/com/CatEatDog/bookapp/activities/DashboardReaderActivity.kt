package com.CatEatDog.bookapp.activities

import android.app.Activity
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.CatEatDog.bookapp.FavoritePageUserFragment
import com.CatEatDog.bookapp.HotFragment
import com.CatEatDog.bookapp.R
import com.CatEatDog.bookapp.SearchPageUserFragment
import com.CatEatDog.bookapp.StatisticsFragment
import com.CatEatDog.bookapp.fragments.GenreListFragment
import com.CatEatDog.bookapp.databinding.ActivityDashboardReaderBinding
import com.CatEatDog.bookapp.UserSettingFragment
import java.util.Locale

class DashboardReaderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardReaderBinding
    private lateinit var sharedPreferences : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadFragment(HotFragment())
        highlightSelectedTab(binding.hot)

        binding.hot.setOnClickListener {
            highlightSelectedTab(it)
            loadFragment(HotFragment())
        }
        binding.search.setOnClickListener {
            highlightSelectedTab(it)
            loadFragment(SearchPageUserFragment())
        }
        binding.fav.setOnClickListener {
            highlightSelectedTab(it)
            loadFragment(FavoritePageUserFragment())
        }

        binding.statistics.setOnClickListener {
            highlightSelectedTab(it)
            loadFragment(StatisticsFragment())
        }

        binding.profile.setOnClickListener {
            highlightSelectedTab(it)
            loadFragment(UserSettingFragment())
        }

    }

    private fun highlightSelectedTab(selectedView: View) {
        binding.hot.isSelected = false
        binding.search.isSelected = false
        binding.fav.isSelected = false
        binding.profile.isSelected = false

        selectedView.isSelected = true
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

}

