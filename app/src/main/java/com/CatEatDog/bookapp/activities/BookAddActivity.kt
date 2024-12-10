package com.CatEatDog.bookapp.activities

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.CatEatDog.bookapp.databinding.ActivityBookAddBinding
import com.CatEatDog.bookapp.models.ModelCategory
import com.CatEatDog.bookapp.models.ModelGenre
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage


class BookAddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookAddBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var progressDialog: ProgressDialog

    private lateinit var genreArrayList: ArrayList<ModelGenre>

    private var bookUri: Uri? = null
    private var coverUri: Uri? = null // For cover image

    private val selectedGenreIds = ArrayList<String>() // Stores selected genres

    private var TAG = "BOOK_ADD_TAG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        loadBookGenres()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        // Handle click: Show genre picker dialog
        binding.genreTv.setOnClickListener {
            genrePickDialog()
        }

        // Handle click: Pick book file
        binding.attachBookBtn.setOnClickListener {
            bookPickIntent()
        }

        // Handle click: Pick cover image
        binding.attachCoverBtn.setOnClickListener {
            coverPickIntent()
        }

        // Handle click: Start uploading book
        binding.submitBtn.setOnClickListener {
            validateData()
        }
    }

    private var title = ""
    private var description = ""
    private var author = ""
    private fun validateData() {
        Log.d(TAG, "validateData: Validating data")

        title = binding.titleEt.text.toString().trim()
        description = binding.descriptionEt.text.toString().trim()
        author = binding.authorEt.text.toString().trim()

        if (bookUri == null) {
            binding.attachBookBtn.text = "Pick Book File (Required)"
        }

        if (coverUri == null) {
            binding.attachCoverBtn.text = "Pick Cover Image (Required)"
        }


        if (title.isEmpty()) {
            Toast.makeText(this, "Enter title...", Toast.LENGTH_SHORT).show()
        } else if (description.isEmpty()) {
            Toast.makeText(this, "Enter description...", Toast.LENGTH_SHORT).show()
        } else if (author.isEmpty()) {
            Toast.makeText(this, "Enter author name...", Toast.LENGTH_SHORT).show()
        } else if (selectedGenreIds.isEmpty()) {
            Toast.makeText(this, "Select at least one genre...", Toast.LENGTH_SHORT).show()
        } else if (bookUri == null) {
            Toast.makeText(this, "Pick a book file...", Toast.LENGTH_SHORT).show()
        } else if (coverUri == null) {
            Toast.makeText(this, "Pick a cover image...", Toast.LENGTH_SHORT).show()
        }  else {
            uploadCoverToStorage()
        }
    }

    private fun loadBookGenres() {
        Log.d(TAG, "loadBookGenrs: Loading book genres")

        genreArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Genres")

        ref.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                genreArrayList.clear()
                for (ds in snapshot.children) {
                    val model = ds.getValue(ModelGenre::class.java)
                    genreArrayList.add(model!!)
                    Log.d(TAG, "onDataChange: ${model.genre}")
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }


    private fun uploadCoverToStorage() {
        Log.d(TAG, "uploadCoverToStorage: Uploading cover image to storage")

        progressDialog.setMessage("Uploading cover image...")
        progressDialog.show()

        val timestamp = System.currentTimeMillis()
        val coverPathName = "Covers/$timestamp"

        val storageReference = FirebaseStorage.getInstance().getReference(coverPathName)
        storageReference.putFile(coverUri!!)
            .addOnSuccessListener { taskSnapshot ->
                Log.d(TAG, "uploadCoverToStorage: Cover uploaded, now getting URL")

                val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                val uploadedCoverUrl = "${uriTask.result}"

                uploadBookToStorage(uploadedCoverUrl, timestamp)
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "uploadCoverToStorage: Failed due to ${e.message}")
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to upload cover due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadBookToStorage(uploadedCoverUrl: String, timestamp: Long) {
        Log.d(TAG, "uploadBookToStorage: Uploading book file to storage")

        val filePathName = "Books/$timestamp"

        val storageReference = FirebaseStorage.getInstance().getReference(filePathName)
        storageReference.putFile(bookUri!!)
            .addOnSuccessListener { taskSnapshot ->
                Log.d(TAG, "uploadBookToStorage: Book uploaded, now getting URL")

                val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                val uploadedBookUrl = "${uriTask.result}"

                uploadBookInfoToDb(uploadedBookUrl, uploadedCoverUrl, timestamp)
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "uploadBookToStorage: Failed due to ${e.message}")
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to upload book due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadBookInfoToDb(uploadedBookUrl: String, uploadedCoverUrl: String, timestamp: Long) {
        Log.d(TAG, "uploadBookInfoToDb: Uploading book info to database")
        progressDialog.setMessage("Uploading book info...")

        val uid = firebaseAuth.uid

        val hashMap: HashMap<String, Any> = HashMap()
        hashMap["uid"] = "$uid"
        hashMap["id"] = "$timestamp"
        hashMap["title"] = "$title"
        hashMap["description"] = "$description"
        hashMap["author"] = "$author"
        hashMap["genreIds"] = selectedGenreIds // Add selected genres as a list
        hashMap["url"] = "$uploadedBookUrl"
        hashMap["coverUrl"] = "$uploadedCoverUrl"
        hashMap["timestamp"] = timestamp
        hashMap["viewCount"] = 0
        hashMap["downloadCount"] = 0

        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child("$timestamp")
            .setValue(hashMap)
            .addOnSuccessListener {
                Log.d(TAG, "uploadBookInfoToDb: Uploaded successfully")
                progressDialog.dismiss()
                Toast.makeText(this, "Book uploaded successfully", Toast.LENGTH_SHORT).show()
                bookUri = null
                coverUri = null
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "uploadBookInfoToDb: Failed due to ${e.message}")
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to upload due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun genrePickDialog() {
        Log.d(TAG, "genrePickDialog: Showing genre picker dialog")

        // Create an array of genre names
        val genresArray = Array(genreArrayList.size) { i -> genreArrayList[i].genre }

        // Create a boolean array to reflect the state of selected genres
        val selectedItems = BooleanArray(genreArrayList.size) { i ->
            selectedGenreIds.contains(genreArrayList[i].id)
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pick Genres")
            .setMultiChoiceItems(genresArray, selectedItems) { _, which, isChecked ->
                val genreId = genreArrayList[which].id
                if (isChecked) {
                    if (!selectedGenreIds.contains(genreId)) {
                        selectedGenreIds.add(genreId)
                    }
                } else {
                    selectedGenreIds.remove(genreId)
                }
            }
            .setPositiveButton("Done") { dialog, _ ->
                binding.genreTv.text = "${selectedGenreIds.size} genres selected"
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }



    private fun bookPickIntent() {
        val intent = Intent()
        intent.type = "application/pdf"
        intent.action = Intent.ACTION_GET_CONTENT
        bookActivityResultLauncher.launch(intent)
    }

    private fun coverPickIntent() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        coverActivityResultLauncher.launch(intent)
    }

    private val bookActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            bookUri = result.data!!.data
            binding.attachBookBtn.text = "Book File Selected"
        } else {
            binding.attachBookBtn.text = "Pick Book File"
            Toast.makeText(this, "No book file selected", Toast.LENGTH_SHORT).show()
        }
    }

    private val coverActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            coverUri = result.data!!.data
            binding.attachCoverBtn.text = "Cover Image Selected"
        } else {
            binding.attachCoverBtn.text = "Pick Cover Image"
            Toast.makeText(this, "No cover image selected", Toast.LENGTH_SHORT).show()
        }
    }



    private fun resetForm() {
        bookUri = null
        coverUri = null
        binding.attachBookBtn.text = "Pick Book File"
        binding.attachCoverBtn.text = "Pick Cover Image"
    }


}
