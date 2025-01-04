package com.CatEatDog.bookapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.CatEatDog.bookapp.MyApplication
import com.CatEatDog.bookapp.R
import com.CatEatDog.bookapp.models.ModelReview

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

        fun bind(review : ModelReview, userName : String){
            nameUser.setText(userName)
            ratingBar.rating = review.star.toFloat()
            dateTv.setText(MyApplication.formatTimeStamp(review.timestamp))
            reviewTv.setText(review.review)
        }
    }
}