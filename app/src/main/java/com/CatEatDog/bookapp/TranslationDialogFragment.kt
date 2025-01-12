package com.CatEatDog.bookapp

import com.CatEatDog.bookapp.R
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.CatEatDog.bookapp.models.ModelFlashCard
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.common.model.DownloadConditions

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class TranslationDialogFragment : DialogFragment() {
    interface OnTranslationActionListener {
        fun onTranslationAction(selectedText: String, translatedText: String, isFlashCardAdded: Boolean)
    }

    private lateinit var listener: OnTranslationActionListener
    private var isEnglishToVietnamese = true
    private lateinit var translator: Translator

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.window?.setBackgroundDrawableResource(R.drawable.shape_round_corner_dialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.fragment_translation_dialog)

        val selectedTextView: TextView = dialog.findViewById(R.id.selectedTextView)
        val translatedTextView: TextView = dialog.findViewById(R.id.translatedTextView)
        val addFlashcardBtn: Button = dialog.findViewById(R.id.addToFlashcardBtn)
        val cancelBtn: Button = dialog.findViewById(R.id.cancelBtn)
        val toggleLanguageBtn: Button = dialog.findViewById(R.id.toggleLanguageBtn)

        val selectedText = arguments?.getString(ARG_SELECTED_TEXT) ?: ""
        selectedTextView.text = selectedText

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(if (isEnglishToVietnamese) TranslateLanguage.VIETNAMESE else TranslateLanguage.ENGLISH)
            .build()
        translator = Translation.getClient(options)

        val conditions = DownloadConditions.Builder().build()
        translator.downloadModelIfNeeded(conditions)
            ?.addOnSuccessListener {
                translator.translate(selectedText)
                    .addOnSuccessListener { translatedText ->
                        translatedTextView.text = translatedText
                    }
                    .addOnFailureListener {
                        translatedTextView.text = "Translation failed"
                    }
            }

        toggleLanguageBtn.setOnClickListener {
            isEnglishToVietnamese = !isEnglishToVietnamese
            toggleLanguageBtn.text = if (isEnglishToVietnamese) "English -> Vietnamese" else "Vietnamese -> English"

            val sourceLanguage = if (isEnglishToVietnamese) TranslateLanguage.ENGLISH else TranslateLanguage.VIETNAMESE
            val targetLanguage = if (isEnglishToVietnamese) TranslateLanguage.VIETNAMESE else TranslateLanguage.ENGLISH

            val newOptions = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguage)
                .build()

            translator.close()
            translator = Translation.getClient(newOptions)

            val conditions = DownloadConditions.Builder().build()
            translator.downloadModelIfNeeded(conditions)
                ?.addOnSuccessListener {
                    translator.translate(selectedText)
                        .addOnSuccessListener { translatedText ->
                            translatedTextView.text = translatedText // Update the translation
                        }
                        .addOnFailureListener {
                            translatedTextView.text = "Translation failed"
                        }
                }
        }


        addFlashcardBtn.setOnClickListener {

            val selectedText = selectedTextView.text.toString()
            val translatedText = translatedTextView.text.toString()

            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val uid = user.uid
                val timestamp = System.currentTimeMillis()
                val flashcardId = FirebaseDatabase.getInstance().reference.child("Flashcards").child(uid).push().key ?: "" // Auto-generate a key for the flashcard


                val flashcard = ModelFlashCard(
                    id = flashcardId,
                    user = uid,
                    timestamp = timestamp,
                    uid = uid,
                    word = selectedText,
                    word_meaning = translatedText
                )


                val database = FirebaseDatabase.getInstance()
                val flashcardRef = database.reference.child("Flashcards").child(uid).child(flashcardId)

                flashcardRef.setValue(flashcard)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Flashcard added!", Toast.LENGTH_SHORT).show()
                        listener.onTranslationAction(selectedText, translatedText, true)
                        dismiss()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(requireContext(), "Error adding flashcard: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }

        cancelBtn.setOnClickListener {
            dismiss()
        }

        return dialog
    }

    companion object {
        private const val ARG_SELECTED_TEXT = "selected_text"

        fun newInstance(selectedText: String): TranslationDialogFragment {
            val fragment = TranslationDialogFragment()
            val args = Bundle().apply {
                putString(ARG_SELECTED_TEXT, selectedText)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnTranslationActionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnTranslationActionListener")
        }
    }
}
