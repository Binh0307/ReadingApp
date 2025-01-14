package com.CatEatDog.bookapp.models

import android.graphics.RectF

class NoteData {
    var rects: List<RectF> = emptyList()
    var note: String = ""

    constructor()

    constructor(
        rects : List<RectF>,
        note : String
    ){
        this.rects = rects
        this.note = note
    }
}