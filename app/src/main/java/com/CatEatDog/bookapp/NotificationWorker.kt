package com.CatEatDog.bookapp

import android.app.PendingIntent
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.CatEatDog.bookapp.R
import com.CatEatDog.bookapp.activities.FlashCardActivity
import com.CatEatDog.bookapp.models.ModelFlashCard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.TimeUnit

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val uid = user.uid
            val database = FirebaseDatabase.getInstance()
            val flashcardsRef = database.reference.child("Flashcards").child(uid)

            flashcardsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentTime = System.currentTimeMillis()
                    val interval = inputData.getLong("studyInterval", TimeUnit.DAYS.toMillis(1))

                    val dueFlashcards = snapshot.children.mapNotNull { snap ->
                        val flashcard = snap.getValue(ModelFlashCard::class.java)
                        val lastStudiedDate = flashcard?.lastStudiedDate ?: 0L
                        val dueTime = lastStudiedDate + interval
                        if (currentTime >= dueTime) flashcard else null
                    }

                    if (dueFlashcards.isNotEmpty()) {
                        sendNotification(dueFlashcards.size)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle the error
                }
            })
        }
        return Result.success()
    }


    private fun sendNotification(dueCount: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "flashcard_notifications"
        val channelName = "Flashcard Notifications"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }


//        val intent = Intent(context, FlashCardActivity::class.java)
//        val pendingIntent = PendingIntent.getActivity(
//            context,
//            0,
//            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_book_white) // Update with your icon
            .setContentTitle("Study Reminder")
            .setContentText("You have $dueCount flashcards due for review!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}



