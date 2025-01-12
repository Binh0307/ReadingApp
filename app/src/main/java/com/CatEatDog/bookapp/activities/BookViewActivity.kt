package com.CatEatDog.bookapp.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.CatEatDog.bookapp.R
import com.CatEatDog.bookapp.TranslationDialogFragment
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

class BookViewActivity : PdfActivity(),
    TextSelectionManager.OnTextSelectionChangeListener,
    TextSelectionManager.OnTextSelectionModeChangeListener,
    OnPreparePopupToolbarListener,
    PopupToolbar.OnPopupToolbarItemClickedListener,
    TranslationDialogFragment.OnTranslationActionListener {

    private var cusPopups = listOf(
        PopupToolbarMenuItem(R.id.translate_action, R.string.translate, true),
        PopupToolbarMenuItem(R.id.highlight_action, R.string.highlight, true),
        PopupToolbarMenuItem(R.id.flashcard_action, R.string.add_flashcard, true)
    )

    private var textSelectionController: TextSelectionController? = null
    private var translator: Translator? = null

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("BookViewActivity", "Activity Created")

        pdfFragment?.setOnPreparePopupToolbarListener(this)
        pdfFragment?.addOnTextSelectionChangeListener(this)
        pdfFragment?.addOnTextSelectionModeChangeListener(this)

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
            R.id.flashcard_action -> {
                textSelectionController?.let { controller ->
                    val text = controller.textSelection?.text
                    Toast.makeText(this, text ?: "No text selected", Toast.LENGTH_SHORT).show()
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
}
