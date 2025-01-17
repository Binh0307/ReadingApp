package com.CatEatDog.bookapp.activities

import com.CatEatDog.bookapp.R
import android.Manifest
import android.provider.Settings

import android.widget.PopupMenu

import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.CatEatDog.bookapp.Constants
import com.CatEatDog.bookapp.MyApplication

import com.CatEatDog.bookapp.RatingDialogFragment
import com.CatEatDog.bookapp.adapters.BookAdapter
import com.CatEatDog.bookapp.adapters.RecommendedBooksAdapter
import com.CatEatDog.bookapp.databinding.ActivityBookDetailBinding
import com.CatEatDog.bookapp.models.ModelBook
import com.CatEatDog.bookapp.models.ModelReview
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.ui.DocumentDescriptor
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import java.io.File
import java.io.FileOutputStream


class BookDetailActivity : AppCompatActivity(), RatingDialogFragment.OnRatingSubmittedListener{


    private lateinit var binding: ActivityBookDetailBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    private var isListenerEnabled = true
    private var isRatingDialogShowing = false
    private var bookId = ""
    private var bookTitle = ""
    private var bookUrl = ""
    private var bookCoverUrl = ""
    private var bookAuthor = ""
    private var genreIds: List<String>? = null


    private var isInMyFavorite = false
    private var modelBook: ModelBook? = null

    companion object {
        private const val TAG = "BOOK_DETAIL_TAG"
    }
    override fun onRatingSubmitted(rating: Float, review: String, isShowing : Boolean) {
        binding.ratingBar.rating = rating
        binding.reviewTv.setText(review)
        addOrUpdateReview(rating,review)
        makeRatingStatistic()
        isRatingDialogShowing = !isShowing

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


        binding.moreBtn.setOnClickListener {
            showPopupMenu()
        }



        binding.backBtn.setOnClickListener { onBackPressed() }

        binding.readBookBtn.setOnClickListener {
            loadBookReaderView()

            markBookAsRead()
        }

        binding.downloadBookBtn.setOnClickListener {
            Log.d("BOOK_DETAIL_TAG", "Download button clicked.")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // For Android 14 (API level 34) and above, check if the app has permission to access all files
                if (Environment.isExternalStorageManager()) {
                    Log.d("BOOK_DETAIL_TAG", "Permission already granted.")
                    downloadBook()
                } else {
                    Log.d("BOOK_DETAIL_TAG", "Permission not granted, requesting permission.")
                    requestStoragePermissionLauncher.launch(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                }
            } else {
                // For older Android versions, check for WRITE_EXTERNAL_STORAGE permission
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d("BOOK_DETAIL_TAG", "Permission already granted.")
                    downloadBook()
                } else {
                    Log.d("BOOK_DETAIL_TAG", "Permission not granted, requesting permission.")
                    requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }



        binding.favoriteBtn.setOnClickListener {
            if (firebaseAuth.currentUser == null) {
                Toast.makeText(this, "Not logged in yet", Toast.LENGTH_SHORT).show()
            } else {
                if (isInMyFavorite) removeFromFavorite() else addToFavorite()
            }
        }

        binding.ratingBar.setOnRatingBarChangeListener{ _, rating, _ ->
            if(isListenerEnabled) {
                if (!isRatingDialogShowing) {
                    val review: String = binding.reviewTv.text.toString()
                    val dialog = RatingDialogFragment.newInstance(rating, review)
                    isRatingDialogShowing = !isRatingDialogShowing
                    dialog.show(supportFragmentManager, "RatingDialog")

                }
            }
        }
        binding.ratingRightArrowIco.setOnClickListener{
            val intent = Intent(this, ReviewsActivity::class.java)
            intent.putExtra("bookId", bookId)
            startActivity(intent)
        }

        displayUserRating()
        makeRatingStatistic()



    }

    private fun markBookAsRead() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val readLog = hashMapOf(
            "bookId" to bookId,
            "genreIds" to modelBook?.genreIds,
            "timestamp" to System.currentTimeMillis()
        )

        FirebaseDatabase.getInstance()
            .getReference("users/$userId/readBooks")
            .child(bookId)
            .setValue(readLog)
    }

    private fun showPopupMenu() {
        val popupMenu = PopupMenu(this, binding.moreBtn)
        popupMenu.menuInflater.inflate(R.menu.menu_book_options, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    val intent = Intent(this, BookEditActivity::class.java)
                    intent.putExtra("bookId", bookId)
                    startActivity(intent)
                    true
                }
                R.id.action_delete -> {
                    confirmDeleteBook()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun confirmDeleteBook() {
        AlertDialog.Builder(this)
            .setTitle("Delete Book")
            .setMessage("Are you sure you want to delete this book?")
            .setPositiveButton("Yes") { _, _ ->
                deleteBook()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Function to delete the book
    private fun deleteBook() {
        val ref = FirebaseDatabase.getInstance().getReference("Books").child(bookId)
        ref.removeValue().addOnSuccessListener {
            Toast.makeText(this, "Book deleted successfully", Toast.LENGTH_SHORT).show()
            finish() // Close the activity
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to delete book: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadBookReaderView(){
        val reference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
        val localFile = File.createTempFile("tempDocument", ".pdf")
        reference.getFile(localFile)
        .addOnSuccessListener {
            openPdf(localFile)
        }
        .addOnFailureListener { exception ->

        }
    }

    private fun openPdf(file: File) {
        // Load the PDF into PSPDFKit
        val pdfUri = Uri.fromFile(file)
        val documentDescriptor = DocumentDescriptor.fromUri(pdfUri)
        documentDescriptor.setTitle(bookTitle)
        val configuration = PdfActivityConfiguration
                .Builder(this)
                .undoEnabled(false)
                .redoEnabled(false)
                .build()
        val intent = PdfActivityIntentBuilder.fromDocumentDescriptor(this, documentDescriptor)
            .configuration(configuration)
            .activityClass(BookViewActivity::class.java)
            .build()
        intent.putExtra("bookId", bookId)
        startActivity(intent)
    }

    private fun loadBookDetails() {
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {


                    // Fetching the genreIds as a string
                    val genreIdsString = snapshot.child("genreIds").value.toString()
                    val genreIdsList = snapshot.child("genreIds").value as? List<String> ?: emptyList()

                    val genreIdsList1 = snapshot.child("genreIds").value
                        ?.let { it as? List<*> }
                        ?.filterIsInstance<String>()
                        ?: emptyList()


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
                    bookAuthor = author
                    genreIds = genreIdsList
                    Log.d(TAG, "Author: ${bookAuthor}")

                    val modelBook = ModelBook(
                        downloadCount = snapshot.child("downloadsCount").value as? Long ?: 0L,
                        viewCount = snapshot.child("viewCount").value as? Long ?: 0L,
                        timestamp = snapshot.child("timestamp").value as? Long ?: 0L,
                        url = snapshot.child("url").value.toString(),
                        description = snapshot.child("description").value.toString(),
                        title = snapshot.child("title").value.toString(),
                        genreIds = genreIdsList,
                        coverUrl = snapshot.child("coverUrl").value.toString(),
                        author = snapshot.child("author").value.toString(),
                        id = snapshot.child("id").value.toString(),
                        uid = snapshot.child("uid").value.toString()
                    )

                    val date = MyApplication.formatTimeStamp(timestamp.toLong())
                    val genreContainer = binding.genreContainer
                    MyApplication.loadGenre(genreIdsList){
                            genres ->
                        for (genre in genres) {
                            val textView = TextView(this@BookDetailActivity).apply {
                                text = genre
                                setPadding(30, 8, 30, 8) // Padding in pixels
                                background = ContextCompat.getDrawable(context, R.drawable.rounded_gray_background)
                                setTextColor(ContextCompat.getColor(context, R.color.primary_tint))
                                textSize = 20f // Text size in sp

                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    setMargins(0, 0, 20, 0) // Margins in pixels
                                }
                                setOnClickListener {
                                    // Handle click event for each genre
                                    Toast.makeText(context, "Clicked: $genre", Toast.LENGTH_SHORT).show()
                                }
                            }

                            // Add the TextView to the container
                            genreContainer.addView(textView)

                        }

                    }

                    MyApplication.loadCover(bookCoverUrl, binding.coverView, binding.progressBar)
//                    MyApplication.loadPdfSize("$url", "$title", binding.sizeTv)
                    Glide.with(this@BookDetailActivity)
                        .load(bookCoverUrl)
                        .into(binding.backgroundImageView)

                    binding.backgroundImageView.alpha = 0.1f



                    binding.titleTv.text = title
                    binding.descriptionTv.text = description
                    binding.viewTv.text = " " + viewCount
                    binding.dateTv.text = " " + date
                    binding.authorTv.text = author

                    loadRecommendedBooks()
                    loadBooksByCommonGenres(genreIdsList)

                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun loadBooksByCommonGenres(genreIdsList: List<String>) {
        val booksRef = FirebaseDatabase.getInstance().getReference("Books")
        booksRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bookList = ArrayList<ModelBook>()
                for (ds in snapshot.children) {
                    val book = ds.getValue(ModelBook::class.java)
                    if (book != null && book.id != bookId) {
                        // Check if any of the genres match
                        val commonGenres = book.genreIds.intersect(genreIdsList)
                        if (commonGenres.isNotEmpty()) {
                            bookList.add(book)
                        }
                    }
                }

                // Limit the list to 5 books
                val limitedBooks = bookList.take(5)
                setupRecommendedBooksByGenreRv(limitedBooks)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load books by common genre: ${error.message}")
            }
        })
    }

    private fun setupRecommendedBooksByGenreRv(bookList: List<ModelBook>) {
        val adapter = BookAdapter()
        binding.recommendedBooksByGenreRv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recommendedBooksByGenreRv.adapter = adapter
        Log.d(TAG, "Books with common genres: $bookList")
        adapter.submitList(bookList)
    }


    private fun loadRecommendedBooks() {
        val booksRef = FirebaseDatabase.getInstance().getReference("Books")
        booksRef.orderByChild("author").equalTo(bookAuthor)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val bookList = ArrayList<ModelBook>()
                    for (ds in snapshot.children) {
                        val book = ds.getValue(ModelBook::class.java)
                        if (book != null && book.id != bookId) {
                            bookList.add(book)
                        }
                    }

                    setupRecommendedBooksRv(bookList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to load recommended books: ${error.message}")
                }
            })
    }

    private fun setupRecommendedBooksRv(bookList: List<ModelBook>) {
        val adapter = BookAdapter()
        binding.recommendedBooksRv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recommendedBooksRv.adapter = adapter
        Log.d(TAG, "Recommended books: $bookList")
        adapter.submitList(bookList)
    }




    private val requestStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Log.d("BOOK_DETAIL_TAG", "Permission request result: $isGranted")

            if (isGranted) {
                // Permission granted, proceed with download
                Log.d("BOOK_DETAIL_TAG", "Permission granted. Starting download.")
                downloadBook()
            } else {
                // Permission denied, show message
                Log.d("BOOK_DETAIL_TAG", "Permission denied.")
                Toast.makeText(this, "Permission denied. Please enable it in the app settings.", Toast.LENGTH_SHORT).show()

                // Optionally, prompt the user to open the settings page for manual permission granting
                openAppSettings()
            }
        }


    private fun openAppSettings() {
        try {
            // Open the app's settings page, where users can enable permissions
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // If there's an issue with opening the settings, log it
            Log.e("BOOK_DETAIL_TAG", "Activity not found to handle the intent.", e)
            Toast.makeText(this, "Failed to open app settings", Toast.LENGTH_SHORT).show()
        }
    }




    private fun downloadBook() {
        progressDialog.setMessage("Downloading book...")
        progressDialog.show()

        FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
            .getBytes(Constants.MAX_BYTES_PDF)
            .addOnSuccessListener { bytes ->
                saveToDownloadsFolder(bytes)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Save file to Downloads folder
    private fun saveToDownloadsFolder(bytes: ByteArray) {
        val nameWithExtension = "${bookTitle}.pdf"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // For Android 14 and above, using MediaStore for Downloads directory
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, nameWithExtension)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS) // Scoped Storage
                }

                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(bytes)
                    }
                    Toast.makeText(this, "Saved to Downloads Folder", Toast.LENGTH_SHORT).show()
                    incrementDownloadCount()
                } ?: run {
                    Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show()
                }
            } else {
                // For older versions, save to Downloads directly
                val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                downloadsFolder.mkdirs()
                val filePath = "${downloadsFolder.path}/$nameWithExtension"
                FileOutputStream(filePath).use { it.write(bytes) }

                Toast.makeText(this, "Saved to Downloads Folder", Toast.LENGTH_SHORT).show()
                incrementDownloadCount()
            }
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



    private fun checkIsFavorite(){
        Log.d(BookDetailActivity.TAG, "checkIsFavorite: ")

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isInMyFavorite = snapshot.exists()
                    if (isInMyFavorite) {
                        binding.favoriteBtn.setImageResource(R.drawable.ic_favorite_white)
//                        binding.favoriteBtn.text = "Remove Favorite"
                    } else {
                        binding.favoriteBtn.setImageResource(R.drawable.ic_favorite_border_white)
//                        binding.favoriteBtn.text = "Add Favorite"
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
    private fun displayUserRating(){
        val ref = FirebaseDatabase.getInstance().getReference("Reviews").
                orderByChild("user").equalTo(firebaseAuth.uid)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for(reviewSnapshot in snapshot.children){
                    val review = reviewSnapshot.getValue(ModelReview::class.java)
                    if(review != null && review.book == bookId){
                        isListenerEnabled = false

                        binding.ratingBar.rating = review.star.toFloat()
                        binding.reviewTv.setText(review.review)

                        isListenerEnabled = true
                        break
                    }
                }

            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun makeRatingStatistic(){
        val ref = FirebaseDatabase.getInstance().getReference("Reviews")

        val starCounts = mutableMapOf(
            1 to 0,
            2 to 0,
            3 to 0,
            4 to 0,
            5 to 0
        )

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalReviews = 0
                var sumStars = 0
                for (reviewSnapshot in snapshot.children) {
                    val star = reviewSnapshot.child("star").getValue(Int::class.java)
                    val book_id = reviewSnapshot.child("book").getValue(String::class.java)
                    if (book_id == bookId && star != null && star in 1..5) {
                        starCounts[star] = starCounts[star]!! + 1
                        totalReviews++
                        sumStars += star
                    }
                }
                val rawPercentages = starCounts.mapValues { (key, value) ->
                    if (totalReviews > 0) {
                        (value * 100.0) / totalReviews // Raw percentage as Double
                    } else {
                        0.0
                    }
                }

                val roundedPercentages = rawPercentages.mapValues { (_, value) ->
                    value.toInt()
                }.toMutableMap()

                val averageRatingValue = if (totalReviews > 0) sumStars.toDouble() / totalReviews else 0.0
                val roundedAverage = String.format("%.1f", averageRatingValue)
                binding.averageRating.setText(roundedAverage)

                binding.averageRatingBar.rating = roundedAverage.toFloat()
                binding.totalReviews.setText("${totalReviews}")

                binding.bar5Stars.progress = roundedPercentages[5] ?: 0
                binding.bar4Stars.progress = roundedPercentages[4] ?: 0
                binding.bar3Stars.progress = roundedPercentages[3] ?: 0
                binding.bar2Stars.progress = roundedPercentages[2] ?: 0
                binding.bar1Stars.progress = roundedPercentages[1] ?: 0


            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }



    private fun addOrUpdateReview(rating : Float, reviewText : String){
        val ref = FirebaseDatabase.getInstance().getReference("Reviews")
            .orderByChild("user").equalTo(firebaseAuth.uid)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var found = false
                for(reviewSnapshot in snapshot.children){
                    val review = reviewSnapshot.getValue(ModelReview::class.java)
                    if(review != null && review.book == bookId){

                        found = true
                        val updateValue = mutableMapOf(
                            "star" to rating.toInt(),
                            "review" to reviewText
                        )
                        val ref = FirebaseDatabase.getInstance().getReference("Reviews")
                        ref.child(reviewSnapshot.key!!).updateChildren(updateValue as Map<String, Any>)
                        break
                    }
                }

                if(!found){
                    val timestamp = System.currentTimeMillis()
                    val hashmap = mutableMapOf(
                        "id" to "${timestamp}",
                        "user" to "${firebaseAuth.uid}",
                        "book" to bookId,
                        "timestamp" to timestamp,
                        "uid" to "${firebaseAuth.uid}",
                        "star" to rating.toInt(),
                        "review" to reviewText
                    )
                    val ref = FirebaseDatabase.getInstance().getReference("Reviews")
                    ref.child("${timestamp}").setValue(hashmap)

                }

            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

}
