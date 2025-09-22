package com.example.cardsstudy

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.GeoPoint

data class StudyLocation(
    @DocumentId @get:Exclude var id: String = "",
    val userId: String = "",
    val name: String = "",
    val location: GeoPoint? = null
)