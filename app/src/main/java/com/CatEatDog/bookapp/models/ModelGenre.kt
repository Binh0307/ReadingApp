package com.CatEatDog.bookapp.models

class ModelGenre {
    var id: String = ""
    var genre: String = "" // name of genre
    var timestamp: Long = 0
    var uid: String = ""

    constructor()

    constructor(id: String, genre: String, timestamp: Long, uid: String) {
        this.id = id
        this.genre = genre
        this.timestamp = timestamp
        this.uid = uid
    }

}