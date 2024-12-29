package com.CatEatDog.bookapp

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.CatEatDog.bookapp.adapters.AdapterBookAdmin
import com.CatEatDog.bookapp.databinding.FragmentHotBinding
import com.CatEatDog.bookapp.models.ModelBook
import com.CatEatDog.bookapp.models.ModelGenre
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar


class HotFragment : Fragment() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: FragmentHotBinding
    private lateinit var adapterBook: AdapterBookAdmin
    private var bookList = ArrayList<ModelBook>()
    private var genreList = ArrayList<ModelGenre>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHotBinding.inflate(inflater,container,false)

        adapterBook = AdapterBookAdmin(requireContext(), bookList, genreList)

        binding.bookRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        val dividerItemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        binding.bookRecyclerView.addItemDecoration(dividerItemDecoration)
        binding.bookRecyclerView.adapter = adapterBook

        binding.greetTv.setText("${getGreeting()}")

        loadBooks("most_view", 5)

        selectButton(binding.mostViewedBtn)

        binding.mostViewedBtn.setOnClickListener{
            selectButton(binding.mostViewedBtn)
            loadBooks("most_view", 5)
        }

        binding.recomendationBtn.setOnClickListener{
            selectButton(binding.recomendationBtn)
        }

        binding.newReleaseBtn.setOnClickListener{
            selectButton(binding.newReleaseBtn)
            loadBooks("new_release", 5)
        }

        binding.topRatedBtn.setOnClickListener{
            selectButton(binding.topRatedBtn)
        }


        return binding.root
    }
    private fun selectButton(button : Button){
        resetButton()
        button.setBackgroundResource(R.drawable.shape_button_hot_selected)
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text_button))
        button.compoundDrawableTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.primary_text_button)
        )
    }
    private fun resetButton(){
        val buttons = listOf(
            binding.mostViewedBtn,
            binding.recomendationBtn,
            binding.newReleaseBtn,
            binding.topRatedBtn
        )
        buttons.forEach { button ->
            button.setBackgroundResource(R.drawable.shape_button_hot_unselected)
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_tint))
            button.compoundDrawableTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.primary_tint)
            )
        }
    }
    private fun loadBooks(filterBy : String = "most_view", noOfBooks : Int = 10) {
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                bookList.clear()
                for (ds in snapshot.children) {
                    val model = ds.getValue(ModelBook::class.java)
                    if (model != null) {
                        bookList.add(model)
                    }
                }

                when(filterBy){
                    "most_view" -> bookList.sortByDescending { it.viewCount }
                    "new_release" -> bookList.sortByDescending { it.timestamp }
                }

                val filteredList = bookList.take(noOfBooks)

                bookList.clear()
                bookList.addAll(filteredList)


                adapterBook.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load books: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })


        val genreRef = FirebaseDatabase.getInstance().getReference("Genres")
        genreRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                genreList.clear()
                for (ds in snapshot.children) {
                    val genre = ds.getValue(ModelGenre::class.java)
                    if (genre != null) {
                        genreList.add(genre)
                    }
                }
                adapterBook.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load genres: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 5..11 -> "Good Morning"
            hour in 12..17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }


}