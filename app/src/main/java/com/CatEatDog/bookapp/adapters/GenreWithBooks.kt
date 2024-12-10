package com.CatEatDog.bookapp.adapters

import com.CatEatDog.bookapp.models.ModelBook
import com.CatEatDog.bookapp.models.ModelGenre

data class GenreWithBooks(
    val genre: ModelGenre,
    val books: MutableList<ModelBook>
)