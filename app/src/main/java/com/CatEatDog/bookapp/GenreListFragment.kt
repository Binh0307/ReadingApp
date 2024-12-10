package com.CatEatDog.bookapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.CatEatDog.bookapp.activities.GenreAddActivity
import com.CatEatDog.bookapp.adapters.AdapterGenre
import com.CatEatDog.bookapp.databinding.FragmentGenreListBinding
import com.CatEatDog.bookapp.models.ModelGenre
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GenreListFragment : Fragment() {
    private lateinit var binding: FragmentGenreListBinding
    private lateinit var adapterGenre: AdapterGenre
    private var genreList = ArrayList<ModelGenre>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGenreListBinding.inflate(inflater, container, false)

        // Setup RecyclerView
        binding.genreRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapterGenre = AdapterGenre(requireContext(), genreList)
        binding.genreRecyclerView.adapter = adapterGenre

        // Load Genres (replace with actual Firebase code)
        loadGenres()

        // Add Genre Button
        binding.addGenreButton.setOnClickListener {
            val intent = Intent(requireContext(), GenreAddActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }

    private fun loadGenres() {
        val ref = FirebaseDatabase.getInstance().getReference("Genres")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                genreList.clear() // Clear the list to avoid duplicates
                for (ds in snapshot.children) {
                    // Parse genre data into ModelGenre
                    val model = ds.getValue(ModelGenre::class.java)
                    if (model != null) {
                        genreList.add(model)
                    }
                }
                // Notify adapter about data changes
                adapterGenre.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors
                Toast.makeText(context, "Failed to load genres: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
