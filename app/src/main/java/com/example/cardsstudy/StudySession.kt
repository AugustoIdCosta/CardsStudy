package com.example.cardsstudy

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class StudySession(
    val userId: String = "",
    val deckId: String = "",
    val deckName: String = "",
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    @ServerTimestamp val timestamp: Date? = null
)