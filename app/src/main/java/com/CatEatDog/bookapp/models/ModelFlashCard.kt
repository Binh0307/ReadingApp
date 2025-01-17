package com.CatEatDog.bookapp.models

class ModelFlashCard {
    var id: String = ""
    var user: String = ""
    var timestamp: Long = 0
    var uid: String = ""
    var word: String = ""
    var word_meaning: String = ""
    var lastStudiedDate: Long? = null

    constructor()

    constructor(id: String, user: String, timestamp: Long, uid: String, word: String = "", word_meaning: String = "") {
        this.id = id
        this.user = user
        this.timestamp = timestamp
        this.uid = uid
        this.word = word
        this.word_meaning = word_meaning
    }
}