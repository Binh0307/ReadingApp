package com.CatEatDog.bookapp

import com.CatEatDog.bookapp.R
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.CatEatDog.bookapp.activities.LoginActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage


class UserSettingFragment : Fragment() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser

    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var imageUri: Uri


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = firebaseAuth.currentUser!!

        nameTextView = view.findViewById(R.id.name_text_view)
        emailTextView = view.findViewById(R.id.email_text_view)

        val avatarUploadButton: ImageButton = view.findViewById(R.id.avatar_upload_button)
        avatarUploadButton.setOnClickListener {
            openImagePicker()
        }

        val logoutButton: ImageButton = view.findViewById(R.id.logout_button)
        logoutButton.setOnClickListener {
            firebaseAuth.signOut()
            checkUser()
        }


        loadUserData(view)


        val nameSection: LinearLayout = view.findViewById(R.id.name_section)
        val emailSection: LinearLayout = view.findViewById(R.id.email_section)

        nameSection.setOnClickListener { showEditDialog("name") }
        emailSection.setOnClickListener { showEditDialog("email") }
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
}
