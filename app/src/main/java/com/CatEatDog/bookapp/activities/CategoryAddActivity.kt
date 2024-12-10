package com.CatEatDog.bookapp.activities

import android.app.ProgressDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.CatEatDog.bookapp.databinding.ActivityCategoryAddBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CategoryAddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryAddBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryAddBinding.inflate(layoutInflater)
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

    private var category = ""

    private fun validatedata() {
        category = binding.categoryEt.text.toString().trim()

        if (category.isEmpty()) {
            Toast.makeText(this, "Enter category..", Toast.LENGTH_SHORT).show()
        } else {
            addCategoryFirebase()
        }
    }

    private fun addCategoryFirebase() {
        progressDialog.show()

        val timestamp = System.currentTimeMillis()

        val hashmap = HashMap<String, Any>()
        hashmap["id"] = "$timestamp"
        hashmap["category"] = category
        hashmap["timestamp"] = timestamp
        hashmap["uid"] = "${firebaseAuth.uid}"

        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.child("$timestamp")
            .setValue(hashmap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Added Successfully", Toast.LENGTH_SHORT).show()


            }
            .addOnFailureListener { e->
                progressDialog.dismiss()
                Toast.makeText(this, "Fail to add category.. due to ${e.message}", Toast.LENGTH_SHORT).show()
            }

    }
}