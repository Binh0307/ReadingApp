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
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.annotations.HighlightAnnotation
import com.pspdfkit.annotations.configuration.AnnotationConfiguration
import com.pspdfkit.bookmarks.Bookmark
import com.pspdfkit.bookmarks.BookmarkProvider
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.listeners.SimpleDocumentListener
import com.pspdfkit.ui.PdfFragment
import com.pspdfkit.ui.editor.AnnotationEditor
import com.pspdfkit.ui.special_mode.controller.AnnotationEditingController
import com.pspdfkit.ui.special_mode.controller.AnnotationSelectionController
import com.pspdfkit.ui.special_mode.controller.AnnotationTool
import com.pspdfkit.ui.special_mode.manager.AnnotationManager
import com.pspdfkit.ui.toolbar.AnnotationEditingToolbar
import com.pspdfkit.ui.toolbar.ContextualToolbar
import com.pspdfkit.ui.toolbar.ContextualToolbarMenuItem
import com.pspdfkit.ui.toolbar.ToolbarCoordinatorLayout.OnContextualToolbarLifecycleListener
import java.util.Date
import java.util.EnumSet

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
    private var color = "#FFFF00"
    private lateinit var currentHighlight: Annotation
    private lateinit var box : MutableList<RectF>

    private var numOfBookmars = 0
    private lateinit var bookmarkProvider : BookmarkProvider

    //log
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var genreIds: List<String>? = null


    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("BookViewActivity", "Activity Created")
        startTime = System.currentTimeMillis()

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
                bookmarkProvider = document.bookmarkProvider
                loadBookmark()
                updateBookmark()

            }
        })

        val pdfOutlineView = pspdfKitViews.outlineView
        val annotationTypes = EnumSet.of(AnnotationType.HIGHLIGHT, AnnotationType.NOTE)
        pdfOutlineView?.setListedAnnotationTypes(annotationTypes)

        customizeAnnotationEditingToolbar()

        updateHighlightDatabase()

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
        translator?.close()
        endTime = System.currentTimeMillis()
        logReadingTime()
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
//                    controller.highlightSelectedText()
                    val text = controller.textSelection?.text
                    box = controller.textSelection!!.textBlocks

                    val highlightAnnotation = HighlightAnnotation(currentPage, box)
                    highlightAnnotation.color = Color.parseColor(color)
                    pdfFragment?.addAnnotationToPage(highlightAnnotation, false)

                    var noteTxt = ""
                    var dialog = NoteDialogFragment.newInstance(noteTxt,0)
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
                "note" to note.note,
                "color" to note.color
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
                        val highlightColor = noteSnap.child("color").getValue(String::class.java)
                        var Rects : MutableList<RectF> = mutableListOf()
                        val rectsSnap = noteSnap.child("rects")
                        for(rectF in rectsSnap.children){
                            val rect = rectF.getValue(RectF::class.java)
                            Rects.add(rect!!)
                        }

                        val highlightAnnotation = HighlightAnnotation(pageNumber!!, Rects)
                        color = highlightColor!!
                        highlightAnnotation.color = Color.parseColor(highlightColor)
                        highlightAnnotation.contents = note
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
        highlights : List<RectF>
    ){
        val path = "${userId}/${bookId}/${page}"
        val ref = FirebaseDatabase.getInstance().getReference("HighlightNote")
        ref.child(path).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for(noteSnapshot in snapshot.children){
                    for(rectSnapshot in noteSnapshot.child("rects").children){
                        val rectF = rectSnapshot.getValue(RectF::class.java)

                        for(highlight in highlights){
                            if(highlight.contains(rectF!!) || highlight == rectF){
                                rectSnapshot.ref.removeValue()
                            }
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

    fun getNote(text: String, action : Int) {
        noteText = text
        var note = NoteData(box,noteText,color)
        if(action == 0){
            val anno = pdfFragment?.document?.annotationProvider?.getAnnotations(currentPage)
            val highlightAnnotation = anno?.lastOrNull()
            highlightAnnotation?.contents = noteText
            saveHighlightAndNote(userId!!,bookId!!,currentPage,note)
        }
        else if(action == 1){
            currentHighlight.contents = noteText
            updateNote(userId!!,bookId!!,currentPage,note.rects,noteText)
        }

    }
    private fun updateNote(
        userId : String,
        bookId : String,
        page : Int,
        highlights : List<RectF>,
        note : String
    ){
        val path = "${userId}/${bookId}/${page}"
        val ref = FirebaseDatabase.getInstance().getReference("HighlightNote")
        ref.child(path).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (noteSnapshot in snapshot.children) {
                    for (rectSnapshot in noteSnapshot.child("rects").children) {
                        val rectF = rectSnapshot.getValue(RectF::class.java)
                        for(highlight in highlights){
                            if (highlight.contains(rectF!!) || highlight == rectF) {
                                noteSnapshot.child("note").ref.setValue(note)
                            }
                        }
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun updateHighlightColor(
        userId : String,
        bookId : String,
        page : Int,
        highlights : List<RectF>,
        color : String
    ){
        val path = "${userId}/${bookId}/${page}"
        val ref = FirebaseDatabase.getInstance().getReference("HighlightNote")
        ref.child(path).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (noteSnapshot in snapshot.children) {
                    for (rectSnapshot in noteSnapshot.child("rects").children) {
                        val rectF = rectSnapshot.getValue(RectF::class.java)
                        for(highlight in highlights){
                            if (highlight.contains(rectF!!) || highlight == rectF) {
                                noteSnapshot.child("color").ref.setValue(color)
                            }
                        }
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun updateHighlightDatabase(){
        pdfFragment?.addOnAnnotationUpdatedListener(object : OnAnnotationUpdatedListener{
            override fun onAnnotationUpdated(p0: Annotation) {
                if(p0 is HighlightAnnotation){
                    val selectBox = p0.rects
                    val page = p0.pageIndex
                    val highlightColor = p0.color.let { String.format("#%06X", (0xFFFFFF and it))}
                    updateHighlightColor(userId!!,bookId!!,page,selectBox,highlightColor)


                    }
            }

            override fun onAnnotationCreated(p0: Annotation) {

            }

            override fun onAnnotationRemoved(p0: Annotation) {
                if(p0 is HighlightAnnotation){
                    val selectBox = p0.rects
                    val page = p0.pageIndex
                    deleteHighlightAndNote(userId!!,bookId!!,page,selectBox)
                }
            }

            override fun onAnnotationZOrderChanged(
                p0: Int,
                p1: MutableList<Annotation>,
                p2: MutableList<Annotation>
            ) {

            }
        })
    }

    private fun loadBookmark(){
        numOfBookmars = 0
        val path = "${userId}/${bookId}"
        val ref = FirebaseDatabase.getInstance().getReference("Bookmarks")
        ref.child(path).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for(snap in snapshot.children){
                    val page = snap.getValue(Int::class.java)
                    numOfBookmars++
                    bookmarkProvider.addBookmark(Bookmark(page!!))

                }
            }
            override fun onCancelled(error: DatabaseError) {

            }
        })

    }
    private fun updateBookmark(){
        bookmarkProvider.addBookmarkListener(object : BookmarkProvider.BookmarkListener{
            override fun onBookmarksChanged(p0: MutableList<Bookmark>) {

                val allBookmarks = p0.map{it -> it.pageIndex!!}
                val size = p0.size

                val path = "${userId}/${bookId}"
                if(numOfBookmars < size){
                    numOfBookmars = size

                    val ref = FirebaseDatabase.getInstance().getReference("Bookmarks")
                    val newestBookmark = pdfFragment?.pageIndex
                    ref.child(path).push().setValue(newestBookmark)
                }
                else if(numOfBookmars > size){
                    numOfBookmars = size
                    val ref = FirebaseDatabase.getInstance().getReference("Bookmarks")
                    ref.child(path).addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for(snap in snapshot.children){
                                val page = snap.getValue(Int::class.java)
                                if(!allBookmarks.contains(page)){
                                    snap.ref.removeValue()
                                }
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {

                        }
                    })
                }

            }
        })
    }

    private fun logReadingTime() {
        val duration = endTime - startTime
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return


        val readingLog = hashMapOf(
            "bookId" to bookId,
            "genreIds" to genreIds,
            "startTime" to startTime,
            "endTime" to endTime,
            "duration" to duration
        )
        FirebaseDatabase.getInstance()
            .getReference("users/$userId/readingLogs")
            .push()
            .setValue(readingLog)
    }

    private fun customizeAnnotationEditingToolbar(){
        setOnContextualToolbarLifecycleListener(object : OnContextualToolbarLifecycleListener {
            override fun onDisplayContextualToolbar(p0: ContextualToolbar<*>) {}

            override fun onPrepareContextualToolbar(p0: ContextualToolbar<*>) {
                if (p0 is AnnotationEditingToolbar) {

                    p0.setOnMenuItemClickListener(object : ContextualToolbar.OnMenuItemClickListener{
                        override fun onToolbarMenuItemClick(
                            p0: ContextualToolbar<*>,
                            p1: ContextualToolbarMenuItem
                        ): Boolean {
                            when(p1.id){
                                com.pspdfkit.R.id.pspdf__annotation_editing_toolbar_item_annotation_note ->{
                                    val currentAnnotation = (p0 as AnnotationEditingToolbar).controller?.currentSingleSelectedAnnotation
                                    if(currentAnnotation is HighlightAnnotation){
                                        var noteTxt = currentAnnotation.contents!!
                                        box = currentAnnotation.rects
                                        color = currentAnnotation.color.let { String.format("#%06X", (0xFFFFFF and it))}
                                        currentHighlight = currentAnnotation
                                        var dialog = NoteDialogFragment.newInstance(noteTxt,1)
                                        dialog.show(supportFragmentManager,"NoteDialog")
                                    }
                                    return true
                                }
                                else -> return false
                            }

                        }
                    })
                }
            }
            override fun onRemoveContextualToolbar(p0: ContextualToolbar<*>) {

            }
        })
    }


}

