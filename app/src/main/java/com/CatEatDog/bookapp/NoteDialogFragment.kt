package com.CatEatDog.bookapp

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.CatEatDog.bookapp.RatingDialogFragment.OnRatingSubmittedListener
import com.CatEatDog.bookapp.activities.BookViewActivity


class NoteDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialog = Dialog(requireContext())
        dialog.window?.setBackgroundDrawableResource(R.drawable.shape_round_corner_dialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setContentView(R.layout.fragment_note_dialog)



        // Resize the dialog
        val window = dialog.window
        val params = window?.attributes
        params?.width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        params?.height = WindowManager.LayoutParams.WRAP_CONTENT
        window?.attributes = params

        val noteEdt : EditText = dialog.findViewById(R.id.noteEdt)
        val writeBtn : Button = dialog.findViewById(R.id.writeBtn)
        val cancelBtn : Button = dialog.findViewById(R.id.cancelBtn)

        val noteText = arguments?.getString(ARG_NOTETEXT) ?: ""

        noteEdt.setText(noteText)

        writeBtn.setOnClickListener{
            val note = noteEdt.text.toString()
            (activity as BookViewActivity).getNote(note)
            dismiss()
        }

        cancelBtn.setOnClickListener{
            dismiss()
        }


        return dialog
    }

    companion object {
        private const val ARG_NOTETEXT = "notetext"

        fun newInstance(noteText: String): NoteDialogFragment {
            val fragment = NoteDialogFragment()
            val args = Bundle().apply {
                putString(ARG_NOTETEXT, noteText)
            }
            fragment.arguments = args

            return fragment
        }
    }

}