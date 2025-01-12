package com.CatEatDog.bookapp.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.CatEatDog.bookapp.R
import com.pspdfkit.datastructures.TextSelection
import com.pspdfkit.listeners.OnPreparePopupToolbarListener
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PopupToolbar
import com.pspdfkit.ui.special_mode.controller.TextSelectionController
import com.pspdfkit.ui.special_mode.manager.TextSelectionManager
import com.pspdfkit.ui.toolbar.ContextualToolbarMenuItem
import com.pspdfkit.ui.toolbar.popup.PdfTextSelectionPopupToolbar
import com.pspdfkit.ui.toolbar.popup.PopupToolbarMenuItem

class BookViewActivity : PdfActivity(),
    TextSelectionManager.OnTextSelectionChangeListener,
    TextSelectionManager.OnTextSelectionModeChangeListener,
    OnPreparePopupToolbarListener,
    PopupToolbar.OnPopupToolbarItemClickedListener {


    private var cusPopups = listOf<PopupToolbarMenuItem>(
        PopupToolbarMenuItem(
            R.id.translate_action,
            R.string.translate,
            true,
        ),
        PopupToolbarMenuItem(
            R.id.highlight_action,
            R.string.highlight,
            true
        ),
        PopupToolbarMenuItem(
            R.id.flashcard_action,
            R.string.add_flashcard,
            true
        )
    )


    private var textSelectionController: TextSelectionController? = null
    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MyAct","Success")


        pdfFragment?.setOnPreparePopupToolbarListener(this)
        pdfFragment?.addOnTextSelectionChangeListener(this)
        pdfFragment?.addOnTextSelectionModeChangeListener(this)

    }



    override fun onDestroy() {
        super.onDestroy()
        pdfFragment?.removeOnTextSelectionChangeListener(this)
        pdfFragment?.removeOnTextSelectionModeChangeListener(this)
    }

    override fun onAfterTextSelectionChange(p0: TextSelection?, p1: TextSelection?) {

    }

    override fun onBeforeTextSelectionChange(p0: TextSelection?, p1: TextSelection?): Boolean {

        return true
    }

    override fun onEnterTextSelectionMode(p0: TextSelectionController) {

        textSelectionController = p0

    }

    override fun onExitTextSelectionMode(p0: TextSelectionController) {

    }

    override fun onPrepareTextSelectionPopupToolbar(p0: PdfTextSelectionPopupToolbar) {
        val menuItems = mutableListOf<PopupToolbarMenuItem>()
        menuItems.addAll(cusPopups)
        p0.setMenuItems(menuItems)
        p0.setOnPopupToolbarItemClickedListener(this)
    }

    override fun onItemClicked(p0: PopupToolbarMenuItem): Boolean {
        when(p0.id){
            R.id.highlight_action ->{
                textSelectionController?.let { controller ->
                    val text = controller.textSelection?.text
                    val box = controller.textSelection?.textBlocks
                    controller.highlightSelectedText()
                    // Store text and all bounding box
                }
                return true
            }
            R.id.translate_action ->{
                textSelectionController?.let { controller ->
                    val text  = controller.textSelection?.text
                    Toast.makeText(this,text!!,Toast.LENGTH_SHORT).show()
                    // Translate text and show

                }
                return true
            }
            R.id.flashcard_action->{
                textSelectionController?.let { controller ->
                    val text  = controller.textSelection?.text
                    Toast.makeText(this,text!!,Toast.LENGTH_SHORT).show()
                    // Check if select text is a word and add to flashcard

                }
                return true
            }
        }
        return false
    }

}