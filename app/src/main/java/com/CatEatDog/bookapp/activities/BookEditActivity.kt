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
import com.CatEatDog.bookapp.databinding.ActivityBookEditBinding
import com.CatEatDog.bookapp.models.ModelBook
import com.CatEatDog.bookapp.models.ModelGenre
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class BookEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookEditBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var genreArrayList: ArrayList<ModelGenre>
    private var bookUri: Uri? = null
    private var coverUri: Uri? = null

    private var selectedGenreIds = ArrayList<String>()
    private var bookId: String = ""

    private var TAG = "BOOK_EDIT_TAG"

    private var isCoverUpdated = false
    private var isBookUpdated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        loadBookGenres()
        loadBookData()

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.genreTv.setOnClickListener {
            genrePickDialog()
        }

        binding.attachBookBtn.setOnClickListener {
            bookPickIntent()
        }

        binding.attachCoverBtn.setOnClickListener {
            coverPickIntent()
        }

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
        } else {
            uploadCoverToStorage()
        }
    }

    private fun loadBookGenres() {
        Log.d(TAG, "loadBookGenres: Loading book genres")

        genreArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Genres")

        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                genreArrayList.clear()
                for (ds in snapshot.children) {
                    val model = ds.getValue(ModelGenre::class.java)
                    genreArrayList.add(model!!)
                    Log.d(TAG, "onDataChange: ${model.genre}")
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadBookData() {
        // Get the book ID passed from the previous activity
        bookId = intent.getStringExtra("bookId") ?: return

        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val book = snapshot.getValue(ModelBook::class.java)
                    book?.let {
                        // Populate the form with the current book data
                        binding.titleEt.setText(it.title)
                        binding.descriptionEt.setText(it.description)
                        binding.authorEt.setText(it.author)
                        selectedGenreIds = it.genreIds as ArrayList<String>

                        // Show selected genres in the text view
                        binding.genreTv.text = "${selectedGenreIds.size} genres selected"

                        // Set existing book file and cover if available, otherwise keep them null
                        bookUri = if (it.url.isNullOrEmpty()) null else Uri.parse(it.url)
                        coverUri = if (it.coverUrl.isNullOrEmpty()) null else Uri.parse(it.coverUrl)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun uploadCoverToStorage() {
        Log.d(TAG, "uploadCoverToStorage: Uploading cover image to storage")

        progressDialog.setMessage("Uploading cover image...")
        progressDialog.show()

        val timestamp = System.currentTimeMillis()
        val coverPathName = "Covers/$timestamp"
        Log.d(TAG, "coverUri: $coverUri")

        if (isCoverUpdated && coverUri != null) {
            // Only upload if a new cover is selected
            val storageReference = FirebaseStorage.getInstance().getReference(coverPathName)
            storageReference.putFile(coverUri!!).addOnSuccessListener { taskSnapshot ->
                Log.d(TAG, "uploadCoverToStorage: Cover uploaded, now getting URL")

                val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                val uploadedCoverUrl = "${uriTask.result}"

                uploadBookToStorage(uploadedCoverUrl, timestamp)
            }.addOnFailureListener { e ->
                Log.d(TAG, "uploadCoverToStorage: Failed due to ${e.message}")
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to upload cover due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // If no new cover image, just use the existing cover URL from the database
            uploadBookToStorage(null, timestamp)
        }
    }

    private fun uploadBookToStorage(uploadedCoverUrl: String?, timestamp: Long) {
        Log.d(TAG, "uploadBookToStorage: Uploading book file to storage")

        val filePathName = "Books/$timestamp"

        if (isBookUpdated && bookUri != null) {
            // Only upload if a new book file is selected
            val storageReference = FirebaseStorage.getInstance().getReference(filePathName)
            storageReference.putFile(bookUri!!).addOnSuccessListener { taskSnapshot ->
                Log.d(TAG, "uploadBookToStorage: Book uploaded, now getting URL")

                val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                val uploadedBookUrl = "${uriTask.result}"

                uploadBookInfoToDb(uploadedBookUrl, uploadedCoverUrl, timestamp)
            }.addOnFailureListener { e ->
                Log.d(TAG, "uploadBookToStorage: Failed due to ${e.message}")
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to upload book due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // If no new book file, just use the existing book URL from the database
            uploadBookInfoToDb(null, uploadedCoverUrl, timestamp)
        }
    }

    private fun uploadBookInfoToDb(uploadedBookUrl: String?, uploadedCoverUrl: String?, timestamp: Long) {
        Log.d(TAG, "uploadBookInfoToDb: Uploading book info to database")
        progressDialog.setMessage("Uploading book info...")

        val uid = firebaseAuth.uid

        // Get reference to the current book to preserve existing values
        val ref = FirebaseDatabase.getInstance().getReference("Books").child(bookId)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Check if the book exists in the database
                if (snapshot.exists()) {
                    // Get the current book data
                    val currentBook = snapshot.getValue(ModelBook::class.java)

                    // Use the current values for fields that should not change
                    val currentUid = currentBook?.uid ?: ""
                    val currentId = currentBook?.id ?: ""
                    val currentViewCount = currentBook?.viewCount ?: 0
                    val currentDownloadCount = currentBook?.downloadCount ?: 0
                    val currentCoverUrl = currentBook?.coverUrl ?: ""
                    val currentBookUrl = currentBook?.url ?: ""

                    // Create a map to store the updated book data
                    val hashMap: HashMap<String, Any> = HashMap()
                    hashMap["uid"] = currentUid
                    hashMap["id"] = currentId
                    hashMap["genreIds"] = selectedGenreIds
                    hashMap["author"] = author
                    hashMap["title"] = title
                    hashMap["description"] = description
                    hashMap["coverUrl"] = uploadedCoverUrl ?: currentCoverUrl
                    hashMap["url"] = uploadedBookUrl ?: currentBookUrl
                    hashMap["timestamp"] = timestamp
                    hashMap["viewCount"] = currentViewCount
                    hashMap["downloadCount"] = currentDownloadCount

                    ref.updateChildren(hashMap).addOnSuccessListener {
                        progressDialog.dismiss()
                        Toast.makeText(this@BookEditActivity, "Book updated successfully", Toast.LENGTH_SHORT).show()
                        finish()  // Close the activity
                    }.addOnFailureListener { e ->
                        progressDialog.dismiss()
                        Toast.makeText(this@BookEditActivity, "Failed to update book: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun genrePickDialog() {
        val genres = genreArrayList.map { it.genre }.toTypedArray()

        val selectedGenres = BooleanArray(genres.size)
        AlertDialog.Builder(this)
            .setTitle("Pick Genres")
            .setMultiChoiceItems(genres, selectedGenres) { _, which, isChecked ->
                if (isChecked) {
                    selectedGenreIds.add(genreArrayList[which].id)
                } else {
                    selectedGenreIds.remove(genreArrayList[which].id)
                }
            }
            .setPositiveButton("OK") { _, _ ->
                binding.genreTv.text = "${selectedGenreIds.size} genres selected"
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun bookPickIntent() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        bookActivityResultLauncher.launch(intent)
    }

    private fun coverPickIntent() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        coverActivityResultLauncher.launch(intent)
    }

    private val bookActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            bookUri = result.data!!.data
            isBookUpdated = true
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
            isCoverUpdated = true
            binding.attachCoverBtn.text = "Cover Image Selected"
        } else {
            binding.attachCoverBtn.text = "Pick Cover Image"
            Toast.makeText(this, "No cover image selected", Toast.LENGTH_SHORT).show()
        }
    }
}
