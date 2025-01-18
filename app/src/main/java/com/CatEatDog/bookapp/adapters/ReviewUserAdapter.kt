package com.CatEatDog.bookapp.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.RecyclerView
import com.CatEatDog.bookapp.MyApplication
import com.CatEatDog.bookapp.R
import com.CatEatDog.bookapp.activities.AnotherUserProfileActivity
import com.CatEatDog.bookapp.activities.ReviewsActivity
import com.CatEatDog.bookapp.models.ModelReview
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase

class ReviewUserAdapter(
    private val reviewUserList : List<Pair<ModelReview, String>>
): RecyclerView.Adapter<ReviewUserAdapter.ReviewUserHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewUserHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_review, parent, false
        )
        return ReviewUserHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewUserHolder, position: Int) {
        val review = reviewUserList[position].first
        val userName = reviewUserList[position].second
        holder.bind(review,userName)

    }
    override fun getItemCount(): Int = reviewUserList.size

    inner class ReviewUserHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val nameUser : TextView = itemView.findViewById(R.id.userNameTv)
        private val ratingBar : RatingBar = itemView.findViewById(R.id.ratingBar)
        private val dateTv : TextView = itemView.findViewById(R.id.dateTv)
        private val reviewTv : TextView = itemView.findViewById(R.id.reviewTv)
        private val avtView : ImageView = itemView.findViewById(R.id.avatar_image_view)
        private val nameAvtLayout : LinearLayout = itemView.findViewById(R.id.nameAndAvtLayout)

        fun bind(review : ModelReview, userName : String){
            nameUser.setText(userName)
            ratingBar.rating = review.star.toFloat()
            dateTv.setText(MyApplication.formatTimeStamp(review.timestamp))
            reviewTv.setText(review.review)
            val uid = review.uid

            nameAvtLayout.setOnClickListener{
                val intent = Intent(itemView.context, AnotherUserProfileActivity::class.java)
                intent.putExtra("userId", uid)
                itemView.context.startActivity(intent)
            }




            FirebaseDatabase.getInstance().getReference("Users")
                .child(uid)
                .child("profileImage")
                .get()
                .addOnSuccessListener { snapshot ->
                    val imageUrl = snapshot.getValue(String::class.java)
                    if (imageUrl != null) {

                        Glide.with(itemView.context)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_person_white)
                            .circleCrop()
                            .into(avtView)

                    }
                }
                .addOnFailureListener {
                    Toast.makeText(itemView.context, "Failed to load profile picture", Toast.LENGTH_SHORT).show()
                }

        }
    }
}