package com.CatEatDog.bookapp.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.CatEatDog.bookapp.MyApplication
import com.CatEatDog.bookapp.R
import com.CatEatDog.bookapp.adapters.ReviewUserAdapter
import com.CatEatDog.bookapp.databinding.ActivityReviewsBinding
import com.CatEatDog.bookapp.models.ModelReview
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ReviewsActivity : AppCompatActivity() {
    private var bookId: String? = null
    private lateinit var binding: ActivityReviewsBinding
    private lateinit var adapterReview : ReviewUserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookId = intent.getStringExtra("bookId")!!


        var listReviewsWithUsers: MutableList<Pair<ModelReview, String>> = mutableListOf()

        adapterReview = ReviewUserAdapter(listReviewsWithUsers)

        binding.reviewRecyclerView.layoutManager = LinearLayoutManager(this)
        val dividerItemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        binding.reviewRecyclerView.addItemDecoration(dividerItemDecoration)
        binding.reviewRecyclerView.adapter = adapterReview


        MyApplication.loadReviewsAndUsers(bookId.toString()) { reviewWithUsers ->
            val sortedReviews = reviewWithUsers.sortedByDescending { it.first.timestamp }
            listReviewsWithUsers.addAll(sortedReviews)
            adapterReview.notifyDataSetChanged()

        }


        loadBookInfo(bookId.toString())

        binding.backBtn.setOnClickListener{
            onBackPressed()
        }




    }

    private fun loadBookInfo(bookId: String){
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    val title = "${snapshot.child("title").value}"
                    val coverUrl = "${snapshot.child("coverUrl").value}"

                    binding.progressBar.visibility = View.VISIBLE
                    MyApplication.loadCover(coverUrl,binding.coverView,binding.progressBar)

                    binding.bookNameTv.setText(title)
                }
                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
}