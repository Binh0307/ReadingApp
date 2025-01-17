package com.CatEatDog.bookapp

import com.CatEatDog.bookapp.R
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.CatEatDog.bookapp.models.ModelFlashCard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.common.model.DownloadConditions
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class TranslationDialogFragment : DialogFragment() {
    interface OnTranslationActionListener {
        fun onTranslationAction(selectedText: String, translatedText: String, isFlashCardAdded: Boolean)
    }

    private lateinit var listener: OnTranslationActionListener
    private var translationMode = TranslationMode.EN_TO_VI
    private lateinit var translator: Translator

    enum class TranslationMode {
        EN_TO_VI,
        VI_TO_EN,
        EN_TO_EN
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.window?.setBackgroundDrawableResource(R.drawable.shape_round_corner_dialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.fragment_translation_dialog)


        // Resize the dialog
        val window = dialog.window
        val params = window?.attributes
        params?.width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        params?.height = WindowManager.LayoutParams.WRAP_CONTENT
        window?.attributes = params

        val selectedTextView: TextView = dialog.findViewById(R.id.selectedTextView)
        val translatedTextView: TextView = dialog.findViewById(R.id.translatedTextView)
        val addFlashcardBtn: Button = dialog.findViewById(R.id.addToFlashcardBtn)
        val cancelBtn: Button = dialog.findViewById(R.id.cancelBtn)
        val toggleLanguageBtn: Button = dialog.findViewById(R.id.toggleLanguageBtn)

        val selectedText = arguments?.getString(ARG_SELECTED_TEXT) ?: ""
        selectedTextView.text = selectedText

        val isSingleWord = selectedText.matches(Regex("^[a-zA-Z]+$")) // Regex to check for a single word
        if (!isSingleWord && translationMode == TranslationMode.EN_TO_EN) {
            translationMode = TranslationMode.EN_TO_VI // Default to EN -> VI if it's not a word
        }

        toggleLanguageBtn.text = getToggleButtonText(translationMode, isSingleWord)
        setupTranslatorOrDictionary(selectedText, translatedTextView, isSingleWord)

        toggleLanguageBtn.setOnClickListener {
            translationMode = when (translationMode) {
                TranslationMode.EN_TO_VI -> if (isSingleWord) TranslationMode.EN_TO_EN else TranslationMode.VI_TO_EN
                TranslationMode.EN_TO_EN -> TranslationMode.VI_TO_EN
                TranslationMode.VI_TO_EN -> TranslationMode.EN_TO_VI
            }

            toggleLanguageBtn.text = getToggleButtonText(translationMode, isSingleWord)
            setupTranslatorOrDictionary(selectedText, translatedTextView, isSingleWord)
        }

        addFlashcardBtn.setOnClickListener {
            val selectedText = selectedTextView.text.toString()
            val translatedText = translatedTextView.text.toString()

            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val uid = user.uid
                val timestamp = System.currentTimeMillis()
                val flashcardId = FirebaseDatabase.getInstance().reference.child("Flashcards").child(uid).push().key ?: ""

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

    private fun setupTranslatorOrDictionary(selectedText: String, translatedTextView: TextView, isSingleWord: Boolean) {
        if (translationMode == TranslationMode.EN_TO_EN && isSingleWord) {
            fetchEnglishDefinition(selectedText, translatedTextView)
        } else {
            setupTranslator(selectedText, translatedTextView)
        }
    }

    private fun setupTranslator(selectedText: String, translatedTextView: TextView) {
        val sourceLanguage = if (translationMode == TranslationMode.EN_TO_VI) TranslateLanguage.ENGLISH else TranslateLanguage.VIETNAMESE
        val targetLanguage = if (translationMode == TranslationMode.EN_TO_VI) TranslateLanguage.VIETNAMESE else TranslateLanguage.ENGLISH

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguage)
            .setTargetLanguage(targetLanguage)
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
    }

    private fun fetchEnglishDefinition(word: String, translatedTextView: TextView) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.dictionaryapi.dev/api/v2/entries/en/$word")
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val jsonResponse = JSONArray(response.body?.string() ?: "[]")
                val definitions = StringBuilder()

                jsonResponse.getJSONObject(0).getJSONArray("meanings").let { meanings ->
                    for (i in 0 until meanings.length()) {
                        val meaning = meanings.getJSONObject(i)
                        val partOfSpeech = meaning.getString("partOfSpeech")
                        val definition = meaning.getJSONArray("definitions").getJSONObject(0).getString("definition")

                        definitions.append("$partOfSpeech: $definition\n")
                    }
                }

                activity?.runOnUiThread {
                    translatedTextView.text = definitions.toString()
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    translatedTextView.text = "Error fetching definition: ${e.message}"
                }
            }
        }.start()
    }

    private fun getToggleButtonText(mode: TranslationMode, isSingleWord: Boolean): String {
        return when (mode) {
            TranslationMode.EN_TO_VI -> "English -> Vietnamese"
            TranslationMode.VI_TO_EN -> "Vietnamese -> English"
            TranslationMode.EN_TO_EN -> if (isSingleWord) "English -> English" else "English -> Vietnamese"
        }
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
