package com.CatEatDog.bookapp.activities

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.CatEatDog.bookapp.NoteDialogFragment
import com.CatEatDog.bookapp.R
import com.CatEatDog.bookapp.TranslationDialogFragment
import com.CatEatDog.bookapp.models.NoteData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pspdfkit.datastructures.TextSelection
import com.pspdfkit.listeners.OnPreparePopupToolbarListener
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PopupToolbar
import com.pspdfkit.ui.special_mode.controller.TextSelectionController
import com.pspdfkit.ui.special_mode.manager.TextSelectionManager
import com.pspdfkit.ui.toolbar.popup.PdfTextSelectionPopupToolbar
import com.pspdfkit.ui.toolbar.popup.PopupToolbarMenuItem
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.AnnotationProvider.OnAnnotationUpdatedListener
import com.pspdfkit.annotations.HighlightAnnotation
import com.pspdfkit.annotations.configuration.AnnotationConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.listeners.SimpleDocumentListener
import com.pspdfkit.ui.PdfFragment
import com.pspdfkit.ui.editor.AnnotationEditor
import com.pspdfkit.ui.special_mode.controller.AnnotationEditingController
import com.pspdfkit.ui.special_mode.controller.AnnotationSelectionController
import com.pspdfkit.ui.special_mode.controller.AnnotationTool
import com.pspdfkit.ui.special_mode.manager.AnnotationManager
import com.pspdfkit.ui.toolbar.AnnotationEditingToolbar

class BookViewActivity : PdfActivity(),
    TextSelectionManager.OnTextSelectionChangeListener,
    TextSelectionManager.OnTextSelectionModeChangeListener,
    OnPreparePopupToolbarListener,
    PopupToolbar.OnPopupToolbarItemClickedListener,
    TranslationDialogFragment.OnTranslationActionListener{

    private var cusPopups = listOf(
        PopupToolbarMenuItem(R.id.translate_action, R.string.translate, true),
        PopupToolbarMenuItem(R.id.highlight_action, R.string.highlight, true)
    )

    private var textSelectionController: TextSelectionController? = null
    private var translator: Translator? = null


    private var currentPage = 0
    var userId : String? = ""
    var bookId : String? = ""
    private var noteText = ""
    private lateinit var box : MutableList<RectF>


    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("BookViewActivity", "Activity Created")

        userId = FirebaseAuth.getInstance().currentUser?.uid
        bookId = intent.getStringExtra("bookId")

        pdfFragment?.setOnPreparePopupToolbarListener(this)
        pdfFragment?.addOnTextSelectionChangeListener(this)
        pdfFragment?.addOnTextSelectionModeChangeListener(this)

        // Load all highlight
        pdfFragment?.addDocumentListener(object : SimpleDocumentListener(){
            override fun onDocumentLoaded(document: PdfDocument) {
                super.onDocumentLoaded(document)
                loadHighlightAndNote(userId!!, bookId!!)

            }
        })

        updateHighlightWhenDeleted()

        // Create TranslatorOptions for initial language direction (English to Vietnamese)
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.VIETNAMESE)
            .build()

        translator = Translation.getClient(options)
    }


    override fun onDestroy() {
        super.onDestroy()
        pdfFragment?.removeOnTextSelectionChangeListener(this)
        pdfFragment?.removeOnTextSelectionModeChangeListener(this)
        translator?.close() // Close the translator when done
        Log.d("BookViewActivity", "Activity Destroyed")
    }

    override fun onAfterTextSelectionChange(p0: TextSelection?, p1: TextSelection?) {}

    override fun onBeforeTextSelectionChange(p0: TextSelection?, p1: TextSelection?): Boolean {
        return true
    }

    override fun onEnterTextSelectionMode(p0: TextSelectionController) {
        textSelectionController = p0
        currentPage = pdfFragment?.pageIndex!!
    }

    override fun onExitTextSelectionMode(p0: TextSelectionController) {}

    override fun onPrepareTextSelectionPopupToolbar(p0: PdfTextSelectionPopupToolbar) {
        val menuItems = mutableListOf<PopupToolbarMenuItem>()
        menuItems.addAll(cusPopups)
        p0.setMenuItems(menuItems)
        p0.setOnPopupToolbarItemClickedListener(this)
    }

    override fun onItemClicked(p0: PopupToolbarMenuItem): Boolean {
        when (p0.id) {
            R.id.highlight_action -> {
                textSelectionController?.let { controller ->
                    controller.highlightSelectedText()
                    val text = controller.textSelection?.text
                    box = controller.textSelection!!.textBlocks

                    var noteTxt = ""
                    var dialog = NoteDialogFragment.newInstance(noteTxt)
                    dialog.show(supportFragmentManager,"NoteDialog")

                }
                return true
            }
            R.id.translate_action -> {
                textSelectionController?.let { controller ->
                    val text = controller.textSelection?.text
                    if (!text.isNullOrEmpty()) {
                        val conditions = DownloadConditions.Builder().build()
                        Log.d("BookViewActivity", "Triggering model download.")
                        translator?.downloadModelIfNeeded(conditions)
                            ?.addOnSuccessListener {
                                Log.d("BookViewActivity", "Model downloaded successfully.")
                                val dialogFragment = TranslationDialogFragment.newInstance(text)
                                dialogFragment.show(supportFragmentManager, "translation_dialog")
                            }
                            ?.addOnFailureListener { exception ->
                                Log.e("BookViewActivity", "Model download failed: ${exception.localizedMessage}")
                                Toast.makeText(this, "Model download failed: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "No text selected", Toast.LENGTH_SHORT).show()
                    }
                }
                return true
            }
        }
        return false
    }

    override fun onTranslationAction(selectedText: String, translatedText: String, isFlashCardAdded: Boolean) {
        if (isFlashCardAdded) {
            Toast.makeText(this, "Flashcard added for: $selectedText", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Translation action completed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveHighlightAndNote(
        userId : String,
        bookId : String,
        pageNumber : Int,
        note : NoteData
    ) {
        val timestamp = System.currentTimeMillis()
        val ref = FirebaseDatabase.getInstance().getReference("HighlightNote")

        ref.child("${userId}").child("${bookId}")
            .child("$pageNumber").child("$timestamp")
            .setValue(mapOf(
                "rects" to note.rects,
                "note" to note.note
            ))

    }

    private fun loadHighlightAndNote(
        userId: String,
        bookId: String
    ){
        val ref = FirebaseDatabase.getInstance().getReference("HighlightNote")
        ref.child("${userId}/${bookId}").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for(pageSnapshot in snapshot.children){
                    val pageNumber = pageSnapshot.key?.toIntOrNull() ?: 0
                    for(noteSnap in pageSnapshot.children){
                        val note = noteSnap.child("note").getValue(String::class.java)
                        var Rects : MutableList<RectF> = mutableListOf()
                        val rectsSnap = noteSnap.child("rects")
                        for(rectF in rectsSnap.children){
                            val rect = rectF.getValue(RectF::class.java)
                            Rects.add(rect!!)
                        }

                        val highlightAnnotation = HighlightAnnotation(pageNumber!!, Rects)
                        highlightAnnotation.color = Color.YELLOW
                        pdfFragment?.addAnnotationToPage(highlightAnnotation, false)

                    }
                }

            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
    private fun deleteHighlightAndNote(
        userId : String,
        bookId : String,
        page : Int,
        highlight : RectF
    ){
        val path = "${userId}/${bookId}/${page}"
        val ref = FirebaseDatabase.getInstance().getReference("HighlightNote")
        ref.child(path).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for(noteSnapshot in snapshot.children){
                    for(rectSnapshot in noteSnapshot.child("rects").children){
                        val rectF = rectSnapshot.getValue(RectF::class.java)

                        if(highlight.contains(rectF!!) || highlight == rectF){
                            rectSnapshot.ref.removeValue()
                        }
                    }
                    noteSnapshot.ref.child("rects").get().addOnSuccessListener { snapshot ->
                        if(!snapshot.exists() || !snapshot.hasChildren()){
                            noteSnapshot.ref.removeValue()
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    override fun onGenerateMenuItemIds(menuItems: MutableList<Int>): MutableList<Int> {
        menuItems.remove(PdfActivity.MENU_OPTION_EDIT_ANNOTATIONS)
        menuItems.remove(PdfActivity.MENU_OPTION_EDIT_CONTENT)
        return menuItems
    }

    fun getNote(text: String) {
        noteText = text
        var note = NoteData(box,noteText)
        saveHighlightAndNote(userId!!,bookId!!,currentPage,note)
    }

    private fun updateHighlightWhenDeleted(){
        pdfFragment?.addOnAnnotationUpdatedListener(object : OnAnnotationUpdatedListener{
            override fun onAnnotationUpdated(p0: Annotation) {

            }

            override fun onAnnotationCreated(p0: Annotation) {

            }

            override fun onAnnotationRemoved(p0: Annotation) {
                val box = p0.boundingBox
                val page = p0.pageIndex
                val deleteBox = RectF(
                    box.left - 5f,
                    box.top - 5f,
                    box.right + 5f,
                    box.bottom + 5f
                )
                deleteHighlightAndNote(userId!!,bookId!!,page,deleteBox)
            }

            override fun onAnnotationZOrderChanged(
                p0: Int,
                p1: MutableList<Annotation>,
                p2: MutableList<Annotation>
            ) {

            }
        })
    }


}

