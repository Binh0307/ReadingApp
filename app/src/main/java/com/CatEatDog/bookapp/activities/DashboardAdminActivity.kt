package com.CatEatDog.bookapp.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import com.CatEatDog.bookapp.models.ModelCategory
import com.CatEatDog.bookapp.adapters.AdapterCategory
import com.CatEatDog.bookapp.databinding.ActivityDashboardAdminBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DashboardAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardAdminBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var categoryArrayList:ArrayList<ModelCategory>
    private lateinit var adapterCategory: AdapterCategory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        checkUser()

        loadCategories()

        //Search
        binding.searchEt.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                try {
                    adapterCategory.filter.filter(s)
                }
                catch (e: Exception) {

                }
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })

        binding.logoutBtn.setOnClickListener {
            firebaseAuth.signOut()
            checkUser()
        }

        // Profile
        binding.profileBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // add category page
        binding.addCategoryBtn.setOnClickListener {
            startActivity(Intent(this, CategoryAddActivity::class.java))
        }

        binding.addPdfFab.setOnClickListener {
            startActivity(Intent(this, PdfAddActivity::class.java))
        }






    }

    private fun loadCategories() {
        categoryArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryArrayList.clear()
                for (ds in snapshot.children) {
                    val model =ds.getValue(ModelCategory::class.java)

                    categoryArrayList.add(model!!)
                }
                //set up adapter
                adapterCategory = AdapterCategory(this@DashboardAdminActivity,categoryArrayList)
                binding.categorierRv.adapter = adapterCategory

            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun checkUser() {
        var firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else{
            val email = firebaseUser.email

            binding.subTitleTv.text = email
        }

    }
}