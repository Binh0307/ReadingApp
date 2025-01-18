package com.CatEatDog.bookapp.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.CatEatDog.bookapp.R
import com.CatEatDog.bookapp.adapters.FlashCardAdapter
import com.CatEatDog.bookapp.databinding.ActivityFlashCardBinding
import com.CatEatDog.bookapp.models.ModelFlashCard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.AnimatorSet


class FlashCardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFlashCardBinding
    private lateinit var flashcardList: MutableList<ModelFlashCard>
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var flashCardAdapter: FlashCardAdapter
    private lateinit var flashcardRecyclerView: RecyclerView
    private var slideshowIndex = 0
    private var isCardFlipped = false
    private val studyInterval = 86400000L // 1 day in milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFlashCardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("UserSettings", MODE_PRIVATE)
        flashcardList = mutableListOf()

        val backButton: ImageButton = findViewById(R.id.backBtn)
        backButton.setOnClickListener { onBackPressed() }
        binding.startSlideshow.setOnClickListener { startSlideshow() }
        binding.slideshowBackBtn.setOnClickListener { endSlideshow() }

        setupRecyclerView()
        loadFlashcards()
        setupSlideshowListeners()
    }

    private fun setupRecyclerView() {
        flashcardRecyclerView = binding.flashcardRecyclerView
        flashcardRecyclerView.layoutManager = GridLayoutManager(this, 2)

        val studyInterval = sharedPreferences.getLong("studyInterval", 86400000L) // Default: 1 day

        flashCardAdapter = FlashCardAdapter(flashcardList, { id ->
            deleteFlashcard(id) // Handle delete logic here
        }, { id, position ->
            // Update lastStudiedDate logic
            val flashcard = flashcardList[position]
            flashcard.lastStudiedDate = System.currentTimeMillis()

            // Update the database
            FirebaseDatabase.getInstance()
                .reference
                .child("Flashcards")
                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                .child(id)
                .setValue(flashcard)
                .addOnSuccessListener {
                    Toast.makeText(this, "Flashcard updated!", Toast.LENGTH_SHORT).show()
                    sortFlashcardsByDueDate() // Update UI
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error updating flashcard: ${exception.message}", Toast.LENGTH_SHORT).show()
                }

            // Notify the adapter
            flashCardAdapter.notifyItemChanged(position)
        }, studyInterval)

        flashcardRecyclerView.adapter = flashCardAdapter
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
                            if (flashcard.lastStudiedDate == null) {
                                flashcard.lastStudiedDate = System.currentTimeMillis()
                                flashcardSnapshot.ref.child("lastStudiedDate")
                                    .setValue(flashcard.lastStudiedDate)
                            }
                            flashcardList.add(flashcard)
                        }
                    }
                    sortFlashcardsByDueDate()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@FlashCardActivity, "Failed to load flashcards: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sortFlashcardsByDueDate() {
        val interval = sharedPreferences.getLong("studyInterval", 86400000L)
        flashcardList.sortBy { flashcard ->
            (flashcard.lastStudiedDate ?: 0) + interval
        }
        flashCardAdapter.notifyDataSetChanged()
    }

    private fun startSlideshow() {
        if (flashcardList.isEmpty()) {
            Toast.makeText(this, "No flashcards available.", Toast.LENGTH_SHORT).show()
            return
        }

        // Sort by due status (due cards first)
        val interval = sharedPreferences.getLong("studyInterval", 86400000L)
        val currentTime = System.currentTimeMillis()
        flashcardList.sortBy { flashcard ->
            val dueTime = (flashcard.lastStudiedDate ?: 0) + interval
            dueTime > currentTime // True if the card is not yet due
        }

        // Set up slideshow
        binding.startSlideshow.visibility = View.GONE
        binding.slideshowContainer.visibility = View.VISIBLE
        binding.flashcardRecyclerView.visibility = View.GONE
        slideshowIndex = 0
        isCardFlipped = false
        displayFlashcard(flashcardList[slideshowIndex])
    }


    private fun endSlideshow() {
        binding.startSlideshow.visibility = View.VISIBLE
        binding.slideshowContainer.visibility = View.GONE
        binding.flashcardRecyclerView.visibility = View.VISIBLE
    }

    private fun displayFlashcard(flashcard: ModelFlashCard) {
        val interval = sharedPreferences.getLong("studyInterval", 86400000L)
        val currentTime = System.currentTimeMillis()

        val duetime = (flashcard.lastStudiedDate ?: 0) + interval
        val isDue = duetime <= currentTime
        binding.flashcardFront.text = flashcard.word
        binding.flashcardBack.text = flashcard.word_meaning
        println("Flashcard: ${flashcard.word}, IsDue: $isDue")

        // Handle card flip visibility
        binding.flashcardFront.visibility = if (isCardFlipped) View.GONE else View.VISIBLE
        binding.flashcardBack.visibility = if (isCardFlipped) View.VISIBLE else View.GONE

        // Handle due date visibility
        binding.dueDateMark.visibility = if (isDue) View.VISIBLE else View.GONE
    }



    private fun setupSlideshowListeners() {
        binding.flashcardFront.setOnClickListener { toggleCardFlip() }
        binding.flashcardBack.setOnClickListener { toggleCardFlip() }

        binding.btnLearned.setOnClickListener { markAsLearned() }
        binding.btnNotLearned.setOnClickListener { moveToNextCard() }
    }

    private fun toggleCardFlip() {
        isCardFlipped = !isCardFlipped
        flipCard(isFlippingToBack = isCardFlipped)
    }


    private fun markAsLearned() {
        val flashcard = flashcardList[slideshowIndex]
        flashcard.lastStudiedDate = System.currentTimeMillis()
        FirebaseDatabase.getInstance()
            .reference
            .child("Flashcards")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .child(flashcard.id)
            .setValue(flashcard)
            .addOnSuccessListener {
                Toast.makeText(this, "Marked as learned.", Toast.LENGTH_SHORT).show()
                moveToNextCard()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun moveToNextCard() {
        if (slideshowIndex >= flashcardList.size) {
            Toast.makeText(this, "Slideshow completed!", Toast.LENGTH_SHORT).show()
            endSlideshow()
            return
        }

        // Animate the current card out of the screen
        val currentCardView = binding.flashcardCard
        val currentFrontView = binding.flashcardFront
        val currentBackView = binding.flashcardBack

        // Set the next flashcard for display
        val nextFlashcard = flashcardList[slideshowIndex]
        displayFlashcard(nextFlashcard)

        // Slide out the current card
        val slideOutAnimator = ObjectAnimator.ofFloat(currentCardView, "translationX", 0f, -currentCardView.width.toFloat()).apply {
            duration = 300
        }

        // Slide in the next card
        val nextCardView = binding.flashcardCard
        val slideInAnimator = ObjectAnimator.ofFloat(nextCardView, "translationX", currentCardView.width.toFloat(), 0f).apply {
            duration = 300
        }

        // Play the animations together
        AnimatorSet().apply {
            playTogether(slideOutAnimator, slideInAnimator)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    slideshowIndex++
                    isCardFlipped = false
                }
            })
            start()
        }
    }


    private fun deleteFlashcard(flashcardId: String) {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        FirebaseDatabase.getInstance().reference
            .child("Flashcards")
            .child(uid)
            .child(flashcardId)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Flashcard deleted.", Toast.LENGTH_SHORT).show()
                loadFlashcards()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun flipCard(isFlippingToBack: Boolean) {
        val cardView = binding.flashcardCard
        val frontView = binding.flashcardFront
        val backView = binding.flashcardBack

        // Rotate the CardView to simulate a flip effect
        val flipOut = ObjectAnimator.ofFloat(cardView, "rotationY", 0f, 90f).apply {
            duration = 50
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)

                    // Change the visibility of front and back views after the flip animation
                    if (isFlippingToBack) {
                        frontView.visibility = View.GONE
                        backView.visibility = View.VISIBLE
                    } else {
                        backView.visibility = View.GONE
                        frontView.visibility = View.VISIBLE
                    }

                    // Rotate back to complete the flip
                    val flipBack = ObjectAnimator.ofFloat(cardView, "rotationY", -90f, 0f).apply {
                        duration = 50
                        start()
                    }
                    flipBack.start()
                }
            })
        }
        flipOut.start()
    }


}
