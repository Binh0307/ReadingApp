package com.CatEatDog.bookapp.activities

import  com.CatEatDog.bookapp.R
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.CatEatDog.bookapp.Constants
import com.CatEatDog.bookapp.databinding.ActivityPdfViewBinding
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

class PdfViewActivity : AppCompatActivity(), OnLoadCompleteListener, OnPageChangeListener {

    private lateinit var binding: ActivityPdfViewBinding
    private var currentPage = 0
    private var bookId = ""
    private var genreIds: List<String>? = null
    private var highlightMode = false // Flag to check if highlighting is enabled
    private var startX = 0f
    private var startY = 0f
    private var endX = 0f
    private var endY = 0f

    private companion object {
        const val TAG = "PDF_VIEW_TAG"
    }

    // Store the highlights and notes
    private val highlights = mutableListOf<HighlightNote>()
    private var highlightColor = "#FFFF00"  // Default yellow color


    private var startTime: Long = 0
    private var endTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookId = intent.getStringExtra("bookId")!!
        genreIds = intent.getStringArrayListExtra("genreIds") ?: emptyList()
        loadBookDetails()

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        // Button to toggle highlight mode
        binding.highlightBtn.setOnClickListener {
            highlightMode = !highlightMode
            if (highlightMode) {
                binding.highlightBtn.text = "Disable Highlighting"
                Toast.makeText(this, "Highlighting Enabled", Toast.LENGTH_SHORT).show()
            } else {
                binding.highlightBtn.text = "Enable Highlighting"
                Toast.makeText(this, "Highlighting Disabled", Toast.LENGTH_SHORT).show()
            }
        }

        startTime = System.currentTimeMillis()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Log the end time when the activity is destroyed
        endTime = System.currentTimeMillis()
        logReadingTime()
    }

    private fun logReadingTime() {
        val duration = endTime - startTime // in milliseconds
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Log the reading time to the database
        val readingLog = hashMapOf(
            "bookId" to bookId,
            "genreIds" to genreIds, // Use the retrieved genreIds
            "startTime" to startTime,
            "endTime" to endTime,
            "duration" to duration
        )
        FirebaseDatabase.getInstance()
            .getReference("users/$userId/readingLogs")
            .push()
            .setValue(readingLog)
    }


    private fun loadBookDetails() {
        Log.d(TAG, "loadBookDetails: get pdf from db")

        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pdfUrl = snapshot.child("url").value.toString()
                    Log.d(TAG, "onDataChange: PDF_URL $pdfUrl")
                    loadBookFromUrl(pdfUrl)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d(TAG, "onCancelled: ${error.message}")
                }
            })
    }

    private fun loadBookFromUrl(pdfUrl: String) {
        Log.d(TAG, "loadBookFromUrl: Get pdf from storage")

        val reference = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
        reference.getBytes(Constants.MAX_BYTES_PDF)
            .addOnSuccessListener { bytes ->
                Log.d(TAG, "loadBookFromUrl: pdf got from url")
                binding.pdfView.fromBytes(bytes)
                    .swipeHorizontal(false)
                    .onPageChange(this)
                    .onLoad(this)
                    .load()
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "loadBookFromUrl: Failed due to ${e.message}")
                binding.progressBar.visibility = View.GONE
            }
    }

    override fun loadComplete(nbPages: Int) {
        // Handle page load completion
        // Enable touch listener when PDF is loaded
        binding.pdfView.setOnTouchListener { v, event ->
            if (highlightMode) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.x
                        startY = event.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        endX = event.x
                        endY = event.y
                        // Here, you can update a visual highlight box as the user moves the finger
                    }
                    MotionEvent.ACTION_UP -> {
                        endX = event.x
                        endY = event.y
                        handleHighlight(startX, startY, endX, endY)
                    }
                }
                true
            } else {
                false
            }
        }
    }

    override fun onPageChanged(page: Int, pageCount: Int) {
        currentPage = page
    }

    // Handle highlight logic when the user selects an area on the screen
    private fun handleHighlight(startX: Float, startY: Float, endX: Float, endY: Float) {
        if (startX != endX && startY != endY) {
            // Only show the dialog if a valid area was selected
            showHighlightAndNoteDialog(startX, startY, endX, endY)
        } else {
            Toast.makeText(this, "No area selected!", Toast.LENGTH_SHORT).show()
        }
    }

    // Show dialog to add highlight and note
    private fun showHighlightAndNoteDialog(startX: Float, startY: Float, endX: Float, endY: Float) {
        // Simulate text selection - you can adjust this part to get actual selected text
        val selectedText = "Selected area: [startX: $startX, startY: $startY, endX: $endX, endY: $endY]"

        // Inflate the dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_note, null)
        val noteInput = dialogView.findViewById<EditText>(R.id.noteInputEt)

        // Create and show the dialog
        AlertDialog.Builder(this)
            .setTitle("Add Note")
            .setView(dialogView)  // Pass the inflated dialog view here
            .setPositiveButton("Save") { _, _ ->
                val note = noteInput.text.toString().trim()
                if (selectedText.isNotEmpty() && note.isNotEmpty()) {
                    saveHighlightAndNote(selectedText, note)
                } else {
                    Toast.makeText(this, "Please select text and add a note!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Save the highlight and note
    private fun saveHighlightAndNote(highlightText: String, note: String) {
        val highlightNote = HighlightNote(
            pageNumber = currentPage,
            highlightedText = highlightText,
            note = note,
            highlightColor = highlightColor
        )
        highlights.add(highlightNote)

        // Save to Firebase Realtime Database
        val ref: DatabaseReference = FirebaseDatabase.getInstance().getReference("Books")
            .child(bookId).child("Highlights").push()
        ref.setValue(highlightNote)
            .addOnSuccessListener {
                Toast.makeText(this, "Highlight and note saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Data class to store highlight and note data
    data class HighlightNote(
        val pageNumber: Int,
        val highlightedText: String,
        val note: String,
        val highlightColor: String
    )
}
