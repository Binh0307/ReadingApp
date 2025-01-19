package com.CatEatDog.bookapp

import com.CatEatDog.bookapp.R
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner

import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.work.Data
import com.CatEatDog.bookapp.activities.FlashCardActivity
import com.CatEatDog.bookapp.activities.LoginActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import androidx.work.*
import com.CatEatDog.bookapp.NotificationWorker
import android.provider.Settings


class UserSettingFragment : Fragment() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser

    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var imageUri: Uri

    private lateinit var notificationSwitch: Switch
    private lateinit var intervalSpinner: Spinner

    private lateinit var sharedPreferences: SharedPreferences

    private val intervalOptions = mapOf(
        "15 Minutes" to 15 * 60 * 1000L,
        "1 Day" to 24 * 60 * 60 * 1000L,
        "3 Days" to 3 * 24 * 60 * 60 * 1000L,
        "7 Days" to 7 * 24 * 60 * 60 * 1000L,
        "14 Days" to 14 * 24 * 60 * 60 * 1000L,
        "1 Month" to 30L * 24 * 60 * 60 * 1000L
    )



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPreferences = requireActivity().getSharedPreferences("UserSettings", Activity.MODE_PRIVATE)


        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = firebaseAuth.currentUser!!

        nameTextView = view.findViewById(R.id.name_text_view)
        emailTextView = view.findViewById(R.id.email_text_view)

        notificationSwitch = view.findViewById(R.id.notification_switch)
        intervalSpinner = view.findViewById(R.id.interval_spinner)

        setupNotificationSettings()
        setupIntervalSelection()

        val avatarUploadButton: ImageButton = view.findViewById(R.id.avatar_upload_button)
        avatarUploadButton.setOnClickListener {
            openImagePicker()
        }

        val logoutButton: ImageButton = view.findViewById(R.id.logout_button)
        logoutButton.setOnClickListener {
            firebaseAuth.signOut()
            checkUser()
        }

        val flashcardBtn: Button = view.findViewById(R.id.flashcardBtn)
        flashcardBtn.setOnClickListener {
            val intent = Intent(context, FlashCardActivity::class.java)
            startActivity(intent)
        }



        loadUserData(view)


        val nameSection: LinearLayout = view.findViewById(R.id.name_section)
        val emailSection: LinearLayout = view.findViewById(R.id.email_section)

        nameSection.setOnClickListener { showEditDialog("name") }
        emailSection.setOnClickListener { showEditDialog("email") }
    }

    private fun setupNotificationSettings() {
        val isNotificationEnabled = sharedPreferences.getBoolean("notificationsEnabled", false)

        notificationSwitch.setOnCheckedChangeListener(null) // Temporarily remove listener
        notificationSwitch.isChecked = isNotificationEnabled
        intervalSpinner.isEnabled = isNotificationEnabled

        // Reattach the listener
        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                enableNotifications()
            } else {
                disableNotifications()
            }
        }
    }



    private fun enableNotifications() {
        intervalSpinner.isEnabled = true
        sharedPreferences.edit().putBoolean("notificationsEnabled", true).apply()
        scheduleNotifications()
        Toast.makeText(requireContext(), "Notifications enabled", Toast.LENGTH_SHORT).show()
    }

    private fun disableNotifications() {
        intervalSpinner.isEnabled = false
        sharedPreferences.edit().putBoolean("notificationsEnabled", false).apply()
        cancelNotifications()
        Toast.makeText(requireContext(), "Notifications disabled", Toast.LENGTH_SHORT).show()
    }






    private fun setupIntervalSelection() {
        val intervals = intervalOptions.keys.toList()
        val selectedInterval = sharedPreferences.getLong("studyInterval", 86400000L)
        val selectedIndex = intervalOptions.values.indexOf(selectedInterval)

        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_list, intervals)
        adapter.setDropDownViewResource(R.layout.dropdown_item)
        intervalSpinner.adapter = adapter

        var isInitialSetup = true

        intervalSpinner.setSelection(selectedIndex)

        intervalSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isInitialSetup) {
                    isInitialSetup = false
                    return
                }

                val selectedKey = intervals[position]
                val selectedValue = intervalOptions[selectedKey] ?: 86400000L

                if (notificationSwitch.isChecked) {
                    sharedPreferences.edit().putLong("studyInterval", selectedValue).apply()
                    Toast.makeText(requireContext(), "Interval updated to $selectedKey", Toast.LENGTH_SHORT).show()
                    scheduleNotifications()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }



    private fun scheduleNotifications() {
        val interval = sharedPreferences.getLong("studyInterval", 86400000L)
        val selectedMinutes = (interval / 60000).toInt()

        val notificationWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            selectedMinutes.toLong(), java.util.concurrent.TimeUnit.MINUTES
        )
            .setInputData(
                Data.Builder()
                    .putLong("studyInterval", interval)
                    .build()
            )
            .build()


        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "FlashcardNotificationWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            notificationWorkRequest
        )
    }


    private fun cancelNotifications() {
        WorkManager.getInstance(requireContext()).cancelUniqueWork("FlashcardNotificationWork")
    }

    private fun loadUserData(view: View) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null && isAdded) {
            nameTextView.text = firebaseUser.displayName ?: "N/A"

            emailTextView.text = firebaseUser.email ?: "N/A"

            val avatarImageView: ImageView = view.findViewById(R.id.avatar_image_view)


            FirebaseDatabase.getInstance().getReference("Users")
                .child(firebaseUser.uid)
                .child("profileImage")
                .get()
                .addOnSuccessListener { snapshot ->
                    val imageUrl = snapshot.getValue(String::class.java)
                    if (imageUrl != null) {

                        Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_person_white)
                            .circleCrop()
                            .into(avatarImageView)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to load profile picture", Toast.LENGTH_SHORT).show()
                }

        }
    }





    private fun showEditDialog(field: String) {
        if (isAdded) {
            val builder = AlertDialog.Builder(requireContext())
            val input = EditText(requireContext())
            input.hint = "Enter new $field"

            builder.setTitle("Update $field")
                .setView(input)
                .setPositiveButton("Save") { _, _ ->
                    val newValue = input.text.toString().trim()
                    if (newValue.isNotEmpty()) {
                        updateUserField(field, newValue)
                    } else {
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Field cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()
                .show()
        }
    }

    private fun updateUserField(field: String, value: String) {
        val updates = hashMapOf<String, Any>(
            field to value
        )
        FirebaseDatabase.getInstance().getReference("Users")
            .child(firebaseUser.uid)
            .updateChildren(updates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (field == "name") {
                        val profileUpdates = userProfileChangeRequest {
                            displayName = value
                        }
                        firebaseUser.updateProfile(profileUpdates)
                            .addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    refreshFirebaseUser { nameTextView.text = value }
                                    Toast.makeText(requireContext(), "Name updated", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else if (field == "email") {
                        firebaseUser.updateEmail(value)
                            .addOnCompleteListener { emailTask ->
                                if (emailTask.isSuccessful) {
                                    refreshFirebaseUser { emailTextView.text = value }
                                    Toast.makeText(requireContext(), "Email updated", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(requireContext(), "Failed to update email", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to update $field", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun refreshFirebaseUser(onComplete: () -> Unit) {
        firebaseAuth.currentUser?.reload()
            ?.addOnCompleteListener { reloadTask ->
                if (reloadTask.isSuccessful) {
                    firebaseUser = firebaseAuth.currentUser!!
                    onComplete()
                } else {
                    Toast.makeText(requireContext(), "Failed to refresh user", Toast.LENGTH_SHORT).show()
                }
            }
    }


    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data!!
            uploadImageToFirebaseStorage()
        }
    }

    private fun uploadImageToFirebaseStorage() {
        val storageRef = FirebaseStorage.getInstance().reference.child("avatars/${firebaseUser.uid}.jpg")
        val uploadTask = storageRef.putFile(imageUri)

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                throw task.exception!!
            }
            storageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                saveImageUrlToDatabase(downloadUri.toString())
            } else {
                Toast.makeText(requireContext(), "Image upload failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveImageUrlToDatabase(imageUrl: String) {
        if (!isAdded) return

        val updates = hashMapOf<String, Any>(
            "profileImage" to imageUrl
        )

        FirebaseDatabase.getInstance().getReference("Users")
            .child(firebaseUser.uid)
            .updateChildren(updates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Profile picture updated", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Failed to update profile picture", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }




    private fun checkUser() {
        if (firebaseAuth.currentUser == null) {

            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Log.d("USER_SETTING_TAG", "Notification permission request result: $isGranted")

            if (isGranted) {
                Log.d("USER_SETTING_TAG", "Notification permission granted. Notifications can be enabled.")
                Toast.makeText(requireContext(), "Notification permission granted.", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("USER_SETTING_TAG", "Notification permission denied.")
                Toast.makeText(
                    requireContext(),
                    "Permission denied. Please enable notifications in app settings.",
                    Toast.LENGTH_SHORT
                ).show()
                openAppSettings()
            }
        }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", requireContext().packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("USER_SETTING_TAG", "Failed to open app settings.", e)
            Toast.makeText(requireContext(), "Failed to open app settings", Toast.LENGTH_SHORT).show()
        }
    }

}
