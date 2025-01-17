package com.CatEatDog.bookapp.adapters

import android.content.Intent
import android.view.Display.Mode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.CatEatDog.bookapp.MyApplication
import com.CatEatDog.bookapp.R
import com.CatEatDog.bookapp.activities.BookDetailActivity
import com.CatEatDog.bookapp.adapters.ReviewUserAdapter.ReviewUserHolder
import com.CatEatDog.bookapp.models.ModelReview
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ReviewBookAdapter(
    private val reviewBookList : List<ModelReview>
) : RecyclerView.Adapter<ReviewBookAdapter.ReviewBookHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewBookHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_book_review, parent, false
        )
        return ReviewBookHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewBookHolder, position: Int) {
        val review = reviewBookList[position]
        holder.bind(review)

    }

    override fun getItemCount(): Int = reviewBookList.size

    inner class ReviewBookHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val ratingBar : RatingBar = itemView.findViewById(R.id.ratingBar)
        private val dateTv : TextView = itemView.findViewById(R.id.dateTv)
        private val reviewTv : TextView = itemView.findViewById(R.id.reviewTv)
        private val coverView : ImageView = itemView.findViewById(R.id.coverView)
        private val progressBar : ProgressBar  = itemView.findViewById(R.id.progressBar)
        private val bookTv : TextView = itemView.findViewById(R.id.bookTv)
        private val nameBookLayout : LinearLayout = itemView.findViewById(R.id.nameAndBookLayout)

        fun bind(review : ModelReview){

            ratingBar.rating = review.star.toFloat()
            dateTv.setText(MyApplication.formatTimeStamp(review.timestamp))
            reviewTv.setText(review.review)
            val bookId = review.book

            nameBookLayout.setOnClickListener{
                val intent = Intent(itemView.context,BookDetailActivity::class.java)
                intent.putExtra("bookId", bookId)
                itemView.context.startActivity(intent)
            }


            val ref = FirebaseDatabase.getInstance().getReference("Books")
            ref.child(bookId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {

                        val title = "${snapshot.child("title").value}"
                        val coverUrl = "${snapshot.child("coverUrl").value}"

                        progressBar.visibility = View.VISIBLE
                        MyApplication.loadCover(coverUrl,coverView,progressBar)

                        bookTv.setText(title)
                    }
                    override fun onCancelled(error: DatabaseError) {

                    }
                })
        }
    }
}