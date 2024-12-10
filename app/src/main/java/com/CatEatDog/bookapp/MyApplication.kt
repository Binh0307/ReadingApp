package com.CatEatDog.bookapp

import android.app.Application
import android.app.ProgressDialog
import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.github.barteksc.pdfviewer.PDFView
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.Calendar
import java.util.Locale

class MyApplication:Application() {
    override fun onCreate() {
        super.onCreate()
    }

    companion object {
        fun formatTimeStamp(timestamp: Long):String{
            val cal = Calendar.getInstance(Locale.ENGLISH)
            cal.timeInMillis = timestamp
            return DateFormat.format("dd/MM/yyyy",cal).toString()
        }

        fun loadImageFromUrl(url: String, imageView: ImageView) {
            Glide.with(imageView.context)
                .load(url)
                //.placeholder(R.drawable.placeholder_image)  // Optional placeholder image
                //.error(R.drawable.error_image)  // Optional error image
                .into(imageView)
        }



        fun loadPdfSize(pdfUrl:  String, pdfTitle: String, sizeTv: TextView) {
            val TAG = "PDF_SIZ_TAG"
            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
            ref.metadata
                .addOnSuccessListener {storageMetaData->
                    Log.d(TAG,"loadPdfSize: got metedata")
                    val bytes = storageMetaData.sizeBytes.toDouble()
                    Log.d(TAG, "loadPdfSize: Size Byte $bytes")

                    val kb = bytes/1024
                    val mb = kb/1024
                    if (mb>=1) {
                        sizeTv.text = "${String.format("%.2f", mb) }MB"
                    } else if (kb >=1) {
                        sizeTv.text = "${String.format("%.2f", kb) }KB"
                    } else  {
                        sizeTv.text = "${String.format("%.2f", bytes) } bytes"
                    }
                }
                .addOnFailureListener { e->
                    Log.d(TAG,"loadPdfSize: faile due to ${e.message}")
                }
        }

        fun loadPage(pdfUrl: String, pagesTv: TextView?) {
            try {
                // Fetch the PDF from the URL
                val url = URL(pdfUrl)
                val connection = url.openConnection()
                connection.connect()

                // Get InputStream from the connection
                val inputStream: InputStream = connection.getInputStream()

                // Create a temporary file to save the PDF data
                val tempFile = File(pagesTv?.context?.cacheDir, "temp.pdf")
                val outputStream = FileOutputStream(tempFile)

                // Copy the input stream (PDF bytes) into the temp file
                inputStream.copyTo(outputStream)
                outputStream.close()

                // Open the temp PDF file for reading
                val fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val pdfRenderer = PdfRenderer(fileDescriptor)

                // Get the number of pages in the PDF
                val pageCount = pdfRenderer.pageCount

                // Set the number of pages to the TextView
                pagesTv?.text = pageCount.toString()

                // Clean up
                pdfRenderer.close()
                fileDescriptor.close()

            } catch (e: IOException) {
                e.printStackTrace()
                pagesTv?.text = "N/A"
            }
        }



        fun loadPdfFromUrlSinglePage(
            pdfUrl: String,
            pdfTitle: String,
            pdfView: PDFView,
            progressBar: ProgressBar,
            pagesTv: TextView?
        ) {
            val TAG = "PDF_THUMBNAIL_TAG"
            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
            ref.getBytes(Constants.MAX_BYTES_PDF)
                .addOnSuccessListener {bytes->


                    Log.d(TAG, "loadPdfSize: Size Byte $bytes")

                    pdfView.fromBytes(bytes)
                        .pages(0)
                        .spacing(0)
                        .swipeHorizontal(false)
                        .enableSwipe(false)
                        .onError { t->
                            progressBar.visibility = View.INVISIBLE
                            Log.d(TAG, "loadPdfFromUrlSinglePage: ${t.message}")
                        }
                        .onPageError { page, t ->
                            progressBar.visibility = View.INVISIBLE
                            Log.d(TAG, "loadPdfFromUrlSinglePage: ${t.message}")
                        }
                        .onLoad{nbPages ->
                            Log.d(TAG, "loadPdfFromUrlSinglePage: Pages: $nbPages")
                            progressBar.visibility = View.INVISIBLE

                            if (pagesTv != null) {
                                pagesTv.text = "$nbPages"
                            }
                        }
                        .load()

                }
                .addOnFailureListener { e->
                    Log.d(TAG,"loadPdfSize: faile due to ${e.message}")
                }

        }

        fun loadCategory(categoryId: String, categoryTv: TextView){
            val ref = FirebaseDatabase.getInstance().getReference("Categories")

            ref.child(categoryId)
                .addListenerForSingleValueEvent(object: ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val category = "${snapshot.child("category").value}"
                        categoryTv.text = category
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
        }

        fun loadGenre(genreIds: List<String>, genreTv: TextView) {

            val ref = FirebaseDatabase.getInstance().getReference("Genres")
            val genres = mutableListOf<String>()
            val totalGenres = genreIds.size
            Log.d("LOAD_GENRE", "List: $genreIds")

            for (genreId in genreIds) {
                // Directly use the genreId without sanitization
                Log.d("LOAD_GENRE", "Fetching genre for ID: $genreId")

                ref.child(genreId).get().addOnSuccessListener { snapshot ->
                    val genreName = snapshot.child("genre").value as? String ?: "Unknown"
                    genres.add(genreName)

                    // Update TextView when all genres are loaded
                    if (genres.size == totalGenres) {
                        genreTv.text = genres.joinToString(", ")
                    }
                }.addOnFailureListener {
                    Log.d("LOAD_GENRE", "Failed to load genre $genreId")
                }
            }
        }



        fun loadCover(coverUrl: String, coverView: ImageView, progressBar: ProgressBar) {
            progressBar.visibility = View.VISIBLE
            Glide.with(coverView.context)
                .load(coverUrl)
//                .placeholder(R.drawable.placeholder_image)
//                .error(R.drawable.error_image)
                .into(coverView)
                .clearOnDetach()
                .apply {
                    progressBar.visibility = View.GONE
                }
        }



        fun deleteBook(context: Context, bookId: String, bookUrl: String, bookTittle: String) {

            val TAG = "DELETE_BOOK_TAG"
            Log.d(TAG, "deleteBook: deleting")

            val progressDialog = ProgressDialog(context)
            progressDialog.setTitle("Please wait")
            progressDialog.setMessage("Deleting $bookTittle")
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.show()

            Log.d(TAG, "deleteBook: Deleting from storage...") 
            val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
            storageReference.delete()
                .addOnSuccessListener {
                    Log.d(TAG, "deleteBook: deleting")

                    val ref = FirebaseDatabase.getInstance().getReference("Books")
                    ref.child(bookId)
                        .removeValue()
                        .addOnSuccessListener {
                            progressDialog.dismiss()
                            Toast.makeText(context,"Successfull", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e->
                            progressDialog.dismiss()
                            Log.d(TAG, "deleteBook: Fail due to ${e.message}")
                            Toast.makeText(context," Fail due to ${e.message}", Toast.LENGTH_SHORT).show()
                        }

                }
                .addOnFailureListener { e->
                    progressDialog.dismiss()
                    Log.d(TAG, "deleteBook: Fail due to ${e.message}")
                    Toast.makeText(context," Fail due to ${e.message}", Toast.LENGTH_SHORT).show()
                }

            
        }

        fun incrementBookViewCount(bookId: String){
            val ref = FirebaseDatabase.getInstance().getReference("Books")
            ref.child(bookId)
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var viewsCount = "${snapshot.child("viewCount").value}"

                        if (viewsCount=="" || viewsCount == null) {
                            viewsCount = "0"
                        }

                        val newViewsCount = viewsCount.toLong()+1
                        val hashMap = HashMap<String, Any>()
                        hashMap["viewCount"] =  newViewsCount

                        val dbref = FirebaseDatabase.getInstance().getReference("Books")
                        dbref.child(bookId)
                            .updateChildren(hashMap)

                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
        }
    }



}