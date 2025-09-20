package com.example.cardsstudy

import com.google.firebase.firestore.Exclude

data class Deck(
    var id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    @get:Exclude var dueCardsCount: Int = 0
)