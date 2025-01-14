package com.CatEatDog.bookapp.activities

import com.CatEatDog.bookapp.R
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.CatEatDog.bookapp.adapters.FlashCardAdapter
import com.CatEatDog.bookapp.models.ModelFlashCard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FlashCardActivity : AppCompatActivity() {

    private lateinit var flashcardRecyclerView: RecyclerView
    private lateinit var flashcardAdapter: FlashCardAdapter
    private val flashcardList = mutableListOf<ModelFlashCard>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flash_card)

        flashcardRecyclerView = findViewById(R.id.flashcardRecyclerView)
        flashcardRecyclerView.layoutManager = GridLayoutManager(this, 2)

        flashcardAdapter = FlashCardAdapter(flashcardList, ::deleteFlashcard)
        flashcardRecyclerView.adapter = flashcardAdapter

        loadFlashcards()
    }

    private fun loadFlashcards() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val uid = user.uid
            val database = FirebaseDatabase.getInstance()
            val flashcardsRef = database.reference.child("Flashcards").child(uid)

            flashcardsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    flashcardList.clear()
                    for (flashcardSnapshot in snapshot.children) {
                        val flashcard = flashcardSnapshot.getValue(ModelFlashCard::class.java)
                        if (flashcard != null) {
                            flashcardList.add(flashcard)
                        }
                    }
                    flashcardAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@FlashCardActivity, "Failed to load flashcards: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteFlashcard(flashcardId: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val uid = user.uid
            val flashcardsRef = FirebaseDatabase.getInstance().reference.child("Flashcards").child(uid).child(flashcardId)

            flashcardsRef.removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Flashcard deleted!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error deleting flashcard: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
