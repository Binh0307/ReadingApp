package com.CatEatDog.bookapp.activities

import android.app.ProgressDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.CatEatDog.bookapp.databinding.ActivityGenreAddBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class GenreAddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGenreAddBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGenreAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Plase wait")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.submitBtn.setOnClickListener {
            validatedata()
        }

    }

    private var genre = ""

    private fun validatedata() {
        genre = binding.genreEt.text.toString().trim()

        if (genre.isEmpty()) {
            Toast.makeText(this, "Enter genre..", Toast.LENGTH_SHORT).show()
        } else {
            addGenreFirebase()
        }
    }

    private fun addGenreFirebase() {
        progressDialog.show()

        val timestamp = System.currentTimeMillis()

        val hashmap = HashMap<String, Any>()
        hashmap["id"] = "$timestamp"
        hashmap["genre"] = genre
        hashmap["timestamp"] = timestamp
        hashmap["uid"] = "${firebaseAuth.uid}"

        val ref = FirebaseDatabase.getInstance().getReference("Genres")
        ref.child("$timestamp")
            .setValue(hashmap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Added Successfully", Toast.LENGTH_SHORT).show()


            }
            .addOnFailureListener { e->
                progressDialog.dismiss()
                Toast.makeText(this, "Fail to add genre.. due to ${e.message}", Toast.LENGTH_SHORT).show()
            }

    }
}