package com.CatEatDog.bookapp.activities

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.CatEatDog.bookapp.MyApplication
import com.CatEatDog.bookapp.R
import com.CatEatDog.bookapp.adapters.ReviewBookAdapter
import com.CatEatDog.bookapp.adapters.ReviewUserAdapter
import com.CatEatDog.bookapp.databinding.ActivityAnotherUserProfileBinding
import com.CatEatDog.bookapp.databinding.ActivityReviewsBinding
import com.CatEatDog.bookapp.models.ModelReview
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AnotherUserProfileActivity : AppCompatActivity() {
    private var userId: String? = null
    private lateinit var binding: ActivityAnotherUserProfileBinding
    private lateinit var adapterReviewBook : ReviewBookAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnotherUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getStringExtra("userId")!!

        var listReviewBooks: MutableList<ModelReview> = mutableListOf()

        adapterReviewBook = ReviewBookAdapter(listReviewBooks)
        binding.reviewRecyclerView.layoutManager = LinearLayoutManager(this)
        val dividerItemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        binding.reviewRecyclerView.addItemDecoration(dividerItemDecoration)

        binding.reviewRecyclerView.adapter = adapterReviewBook

        MyApplication.loadReviewsOfUser(userId!!){ reviewsOfUser ->
            val sortedReviews = reviewsOfUser.sortedByDescending { it.timestamp }
            listReviewBooks.addAll(sortedReviews)
            adapterReviewBook.notifyDataSetChanged()

        }
        loadUserInfo(userId!!)

        binding.backBtn.setOnClickListener{
            onBackPressed()
        }



    }
    private fun loadUserInfo(userId : String){
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userName = snapshot.child("name").getValue(String::class.java)!!
                    binding.userNameTv.setText(userName)
                    val profileImageUrl = snapshot.child("profileImage").getValue(String::class.java)!!
                    if(profileImageUrl != ""){
                        Glide.with(this@AnotherUserProfileActivity)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_person_white)
                            .circleCrop()
                            .into(binding.avatarImageView)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
}