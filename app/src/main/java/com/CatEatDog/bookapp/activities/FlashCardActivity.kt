package com.CatEatDog.bookapp.activities

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.CompoundButton
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
import java.util.*
import androidx.work.*
import com.CatEatDog.bookapp.NotificationWorker


class FlashCardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFlashCardBinding
    private lateinit var flashcardRecyclerView: RecyclerView
    private lateinit var flashCardAdapter: FlashCardAdapter
    private val flashcardList = mutableListOf<ModelFlashCard>()

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFlashCardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //sharedPreferences = getSharedPreferences("FlashcardPrefs", Context.MODE_PRIVATE)
        sharedPreferences = getSharedPreferences("UserSettings", MODE_PRIVATE)

        setupRecyclerView()
        loadFlashcards()
//        setupNotificationToggle()
//        setupIntervalSelection()
//        scheduleNotifications()
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
        val interval = sharedPreferences.getLong("studyInterval", 86400000L) // Default: 1 day
        flashcardList.sortBy { flashcard ->
            (flashcard.lastStudiedDate ?: 0) + interval
        }
        flashCardAdapter.notifyDataSetChanged()
    }


    private fun updateLastStudiedDate(flashcardId: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val uid = user.uid
            val flashcardsRef = FirebaseDatabase.getInstance().reference.child("Flashcards").child(uid).child(flashcardId)
            val currentTime = System.currentTimeMillis()

            flashcardsRef.child("lastStudiedDate").setValue(currentTime)
                .addOnSuccessListener {
                    Toast.makeText(this, "Flashcard updated!", Toast.LENGTH_SHORT).show()
                    loadFlashcards() // Reload flashcards after update
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error updating flashcard: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
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
                    loadFlashcards() // Reload flashcards after deletion
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error deleting flashcard: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

//    private fun setupNotificationToggle() {
//        val isNotificationEnabled = sharedPreferences.getBoolean("notificationsEnabled", true)
//        binding.notificationSwitch.isChecked = isNotificationEnabled
//
//        binding.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
//            sharedPreferences.edit().putBoolean("notificationsEnabled", isChecked).apply()
//            Toast.makeText(this, if (isChecked) "Notifications enabled" else "Notifications disabled", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun setupIntervalSelection() {
//        val intervalOptions = mapOf(
//            R.id.radio30Seconds to 30000L,
//            R.id.radio1Minute to 60000L,
//            R.id.radio2Minute to 120000L,
//            R.id.radio1Day to 86400000L,
//            R.id.radio4Days to 345600000L,
//            R.id.radio7Days to 604800000L
//        )
//
//        val selectedInterval = sharedPreferences.getLong("studyInterval", 86400000L) // Default: 1 day
//        val selectedRadioButtonId = intervalOptions.entries.firstOrNull { it.value == selectedInterval }?.key
//        if (selectedRadioButtonId != null) {
//            binding.intervalRadioGroup.check(selectedRadioButtonId)
//        }
//
//        binding.intervalRadioGroup.setOnCheckedChangeListener { _, checkedId ->
//            val newInterval = intervalOptions[checkedId] ?: 86400000L
//            sharedPreferences.edit().putLong("studyInterval", newInterval).apply()
//            Toast.makeText(this, "Interval updated!", Toast.LENGTH_SHORT).show()
//            sortFlashcardsByDueDate()
//        }
//    }
//
//    private fun scheduleNotifications() {
//        val isNotificationEnabled = sharedPreferences.getBoolean("notificationsEnabled", true)
//        if (!isNotificationEnabled) return
//
//        val interval = sharedPreferences.getLong("studyInterval", 86400000L) // Default: 1 day
//
//        val workData = Data.Builder()
//            .putLong("studyInterval", interval)
//            .build()
//
//        val notificationWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
//            1, // Minimum interval for periodic work is 15 minutes
//            java.util.concurrent.TimeUnit.MINUTES
//        )
//            .setInputData(workData)
//            .build()
//
//        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
//            "FlashcardNotificationWork",
//            ExistingPeriodicWorkPolicy.KEEP,
//            notificationWorkRequest
//        )
//    }


}
