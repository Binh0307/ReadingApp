package com.CatEatDog.bookapp.activities

import android.Manifest
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.CatEatDog.bookapp.Constants
import com.CatEatDog.bookapp.MyApplication
import com.CatEatDog.bookapp.R
import com.CatEatDog.bookapp.activities.BookDetailActivity.Companion
import com.CatEatDog.bookapp.databinding.ActivityBookDetailBinding
import com.CatEatDog.bookapp.models.ModelBook
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.FileOutputStream

class BookDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookDetailBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    private var bookId = ""
    private var bookTitle = ""
    private var bookUrl = ""
    private var bookCoverUrl = ""

    private var isInMyFavorite = false
    private var modelBook: ModelBook? = null

    companion object {
        private const val TAG = "BOOK_DETAIL_TAG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookId = intent.getStringExtra("bookId") ?: throw IllegalArgumentException("Book ID must be passed")


        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Plase wait...")
        progressDialog.setCanceledOnTouchOutside(false)

        // Initialize FirebaseAuth and ProgressDialog
        firebaseAuth = FirebaseAuth.getInstance()

        if (firebaseAuth.currentUser != null) {
            checkIsFavorite()
        }

        // Increment book view count
        MyApplication.incrementBookViewCount(bookId)

        // Load book details
        loadBookDetails()

        binding.backBtn.setOnClickListener { onBackPressed() }

        binding.readBookBtn.setOnClickListener {
            val intent = Intent(this, PdfViewActivity::class.java)
            intent.putExtra("bookId", bookId)
            startActivity(intent)
        }

        binding.downloadBookBtn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                downloadBook()
            } else {
                requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        binding.favoriteBtn.setOnClickListener {
            if (firebaseAuth.currentUser == null) {
                Toast.makeText(this, "Not logged in yet", Toast.LENGTH_SHORT).show()
            } else {
                if (isInMyFavorite) removeFromFavorite() else addToFavorite()
            }
        }
    }

    private val requestStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                downloadBook()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private fun downloadBook() {
        progressDialog.setMessage("Downloading book...")
        progressDialog.show()

        FirebaseStorage.getInstance().getReferenceFromUrl(modelBook?.url ?: "")
            .getBytes(Constants.MAX_BYTES_PDF)
            .addOnSuccessListener { bytes ->
                saveToDownloadsFolder(bytes)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveToDownloadsFolder(bytes: ByteArray) {
        val nameWithExtension = "${modelBook?.title}.pdf"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, nameWithExtension)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)?.let { uri ->
                    contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                }
            } else {
                val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                downloadsFolder.mkdirs()

                val filePath = "${downloadsFolder.path}/$nameWithExtension"
                FileOutputStream(filePath).use { it.write(bytes) }
            }

            Toast.makeText(this, "Saved to Downloads Folder", Toast.LENGTH_SHORT).show()
            incrementDownloadCount()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            progressDialog.dismiss()
        }
    }

    private fun incrementDownloadCount() {
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        modelBook?.let { book ->
            ref.child(book.id).get().addOnSuccessListener { snapshot ->
                val downloadCount = snapshot.child("downloadCount").value.toString().toLongOrNull() ?: 0L
                ref.child(book.id).updateChildren(mapOf("downloadCount" to downloadCount + 1))
            }
        }
    }

    private fun loadBookDetails() {
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {

                    // Fetching the genreIds as a string
                    val genreIdsString = snapshot.child("genreIds").value.toString()
                    val genreIdsList = snapshot.child("genreIds").value as? List<String> ?: emptyList()

                    val author = "${snapshot.child("author").value}"
                    val description = "${snapshot.child("description").value}"
                    val downloadCount = "${snapshot.child("downloadsCount").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"
                    val viewCount = "${snapshot.child("viewCount").value}"
                    val title = "${snapshot.child("title").value}"
                    val url = "${snapshot.child("url").value}"
                    val coverUrl = "${snapshot.child("coverUrl").value}"
                    val uid = "${snapshot.child("uid").value}"

                    Log.d("BookDetailActivity", "bookId: $bookId")
                    Log.d("BookDetailActivity", "genreIdsString: $genreIdsString")
                    Log.d("BookDetailActivity", "coverUrl: $coverUrl")


                    bookTitle = title
                    bookUrl = url
                    bookCoverUrl = coverUrl

                    val date = MyApplication.formatTimeStamp(timestamp.toLong())

                    MyApplication.loadGenre(genreIdsList, binding.genreTv)
                    MyApplication.loadCover(bookCoverUrl, binding.coverView, binding.progressBar)
                    MyApplication.loadPdfSize("$url", "$title", binding.sizeTv)


                    binding.titleTv.text = title
                    binding.descriptionTv.text = description
                    binding.viewTv.text = viewCount
                    binding.dateTv.text = date
                    binding.authorTv.text = author

                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun checkIsFavorite(){
        Log.d(BookDetailActivity.TAG, "checkIsFavorite: ")

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isInMyFavorite = snapshot.exists()
                    if (isInMyFavorite) {
                        binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0,
                            R.drawable.ic_favorite_white,0,0)
                        binding.favoriteBtn.text = "Remove Favorite"
                    } else {
                        binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0,
                            R.drawable.ic_favorite_border_white,0,0)
                        binding.favoriteBtn.text = "Add Favorite"
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun addToFavorite() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .setValue(mapOf("bookId" to bookId, "timestamp" to System.currentTimeMillis()))
            .addOnSuccessListener { checkIsFavorite() }
    }

    private fun removeFromFavorite() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId).removeValue()
            .addOnSuccessListener { checkIsFavorite() }
    }
}
