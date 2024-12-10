package com.CatEatDog.bookapp.models

class ModelBook {

    var uid: String = ""
    var id: String = ""
    var genreIds: List<String> = emptyList() // Changed from String to List<String>
    var author: String = ""
    var title: String = ""
    var description: String = ""
    var coverUrl: String = ""
    var url: String = ""
    var timestamp: Long = 0
    var viewCount: Long = 0
    var downloadCount: Long = 0

    constructor()

    constructor(
        downloadCount: Long,
        viewCount: Long,
        timestamp: Long,
        url: String,
        description: String,
        title: String,
        genreIds: List<String>,
        coverUrl: String,
        author: String,
        id: String,
        uid: String
    ) {
        this.downloadCount = downloadCount
        this.viewCount = viewCount
        this.timestamp = timestamp
        this.url = url
        this.coverUrl = coverUrl
        this.description = description
        this.title = title
        this.genreIds = genreIds // Updated here
        this.author = author
        this.id = id
        this.uid = uid
    }
}
