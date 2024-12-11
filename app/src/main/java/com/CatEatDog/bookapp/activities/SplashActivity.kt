package com.CatEatDog.bookapp.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.CatEatDog.bookapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SplashActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private val TAG = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Log.d(TAG, "onCreate: Start plash")

        firebaseAuth = FirebaseAuth.getInstance()

        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "onCreate: Check user")
            checkUser()
            Log.d(TAG, "onCreate: Check user complete")
        }, 5000)
    }

    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
        Log.d(TAG, "checkUser: checking user ${firebaseUser}")


        if (firebaseUser == null) {
            Log.d(TAG, "checkUser: to main")
            navigateToMainActivity()
        } else {
            Log.d(TAG, "checkUser: to user")
            Log.d(TAG, "checkUser: ${firebaseUser.uid}")
            val ref = FirebaseDatabase.getInstance().getReference("Users")

            ref.child(firebaseUser.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        Log.d(TAG, "DataSnapshot: ${snapshot.value}")
                        if (!snapshot.exists()) {
                            Log.d(TAG, "User data does not exist.")
                            firebaseAuth.signOut()
                            navigateToMainActivity()
                        } else {
                            val userType = snapshot.child("userType").value?.toString()
                            Log.d(TAG, "User type: $userType")
                            when (userType) {
                                "user" -> startActivity(Intent(this@SplashActivity, DashboardReaderActivity::class.java))
                                "admin" -> startActivity(Intent(this@SplashActivity, DashboardActivity::class.java))
                                else -> navigateToMainActivity()
                            }
                            finish()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Database Error: ${error.message}")
                        error.toException().printStackTrace()
                        Toast.makeText(this@SplashActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                        navigateToMainActivity()
                    }
                })
        }
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this@SplashActivity, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }
}
