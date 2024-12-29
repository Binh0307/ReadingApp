package com.CatEatDog.bookapp.models

class ModelReview {
    var id: String = ""
    var user: String = ""
    var book: String = ""
    var timestamp: Long = 0
    var uid: String = ""
    var star: Int = 0
    var review: String = ""

    constructor()

    constructor(id: String, user: String, book:String, timestamp: Long, uid: String, star: Int, review: String) {
        this.id = id
        this.user = user
        this.book = book
        this.timestamp = timestamp
        this.uid = uid
        this.star = star
        this.review = review
    }

}