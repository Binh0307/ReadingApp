package com.CatEatDog.bookapp.models

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Canvas

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint


class HighlightView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var startX = 0f
    private var startY = 0f
    private var endX = 0f
    private var endY = 0f
    private val paint = Paint()

    init {
        paint.color = Color.Yellow // Set highlight color to yellow

    }

    // Set the coordinates for the highlight
    fun setHighlight(startX: Float, startY: Float, endX: Float, endY: Float) {
        this.startX = startX
        this.startY = startY
        this.endX = endX
        this.endY = endY
        invalidate() // Redraw the view
    }

//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        // Draw the highlight rectangle
//        canvas.drawRect(startX, startY, endX, endY, paint)
//    }
}
