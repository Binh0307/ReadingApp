package com.CatEatDog.bookapp.fragments

import android.R
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.CatEatDog.bookapp.activities.BookAddActivity
import com.CatEatDog.bookapp.adapters.AdapterBookAdmin
import com.CatEatDog.bookapp.databinding.FragmentBookListBinding
import com.CatEatDog.bookapp.models.ModelBook
import com.CatEatDog.bookapp.models.ModelGenre
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class BookListFragment : Fragment() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: FragmentBookListBinding
    private lateinit var adapterBook: AdapterBookAdmin
    private var bookList = ArrayList<ModelBook>()
    private var genreList = ArrayList<ModelGenre>()  // List of genres

    private companion object {
        const val TAG = "BOOK_LIST_FRAGMENT"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        checkUserType()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,

    ): View {
        binding = FragmentBookListBinding.inflate(inflater, container, false)

        adapterBook = AdapterBookAdmin(requireContext(), bookList, genreList)

        binding.bookRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.bookRecyclerView.adapter = adapterBook


        //checkUserType()
        binding.addBookButton.setOnClickListener {
            val intent = Intent(requireContext(), BookAddActivity::class.java)
            startActivity(intent)
        }


        binding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                try{
                    adapterBook.filter!!.filter(s)
                }
                catch (e: Exception) {
                    Log.d(TAG, "onTextChanged: ${e.message}")
                }
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })

        loadBooks()
        return binding.root
    }

    private fun loadBooks() {
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

    private fun checkUserType() {
        binding.addBookButton.visibility = View.GONE
        var firebaseUser = firebaseAuth.currentUser!!


        var ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child((firebaseUser.uid))
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    var userType = snapshot.child("userType").value
                    Log.d(TAG, "Retrieved userType: $userType")
                    if (userType == "user") {

                        binding.addBookButton.hide()
                    } else if (userType == "admin")  {
                        binding.addBookButton.show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }
}
