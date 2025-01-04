package com.CatEatDog.bookapp

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.fragment.app.DialogFragment

class RatingDialogFragment : DialogFragment() {
    interface OnRatingSubmittedListener {
        fun onRatingSubmitted(rating: Float, review: String, isDialogShowing : Boolean)
    }

    private lateinit var listener: OnRatingSubmittedListener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {


        val dialog = Dialog(requireContext())
        dialog.window?.setBackgroundDrawableResource(R.drawable.shape_round_corner_dialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_rating_comment)

        val ratingBar : RatingBar = dialog.findViewById(R.id.dialogRatingBar)
        val reviewEdt : EditText = dialog.findViewById(R.id.reviewEdt)
        val postBtn : Button = dialog.findViewById(R.id.postBtn)
        val cancelBtn : Button = dialog.findViewById(R.id.cancelBtn)

        val rating = arguments?.getFloat(ARG_RATING) ?: 0f
        val review = arguments?.getString(ARG_REVIEW) ?: ""

        ratingBar.rating = rating
        reviewEdt.setText(review)

        postBtn.setOnClickListener {
            val rating = ratingBar.rating
            val review = reviewEdt.text.toString()
            listener.onRatingSubmitted(rating,review,true)
            dismiss()
        }

        cancelBtn.setOnClickListener {
            dismiss()
        }

        return dialog
    }
    companion object {
        private const val ARG_RATING = "rating"
        private const val ARG_REVIEW = "review"

        fun newInstance(rating: Float, review: String): RatingDialogFragment {
            val fragment = RatingDialogFragment()
            val args = Bundle().apply {
                putFloat(ARG_RATING, rating)
                putString(ARG_REVIEW, review)
            }
            fragment.arguments = args

            return fragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnRatingSubmittedListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnRatingSubmittedListener")
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
    }

}