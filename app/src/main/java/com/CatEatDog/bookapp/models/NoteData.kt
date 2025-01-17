package com.CatEatDog.bookapp.models

import android.graphics.RectF

class NoteData {
    var rects: List<RectF> = emptyList()
    var note: String = ""
    var color : String ="#FFFF00"

    constructor()

    constructor(
        rects : List<RectF>,
        note : String,
        color : String
    ){
        this.rects = rects
        this.note = note
        this.color = color
    }
}