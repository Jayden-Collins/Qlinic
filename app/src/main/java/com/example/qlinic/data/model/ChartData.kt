package com.example.qlinic.data.model

import androidx.compose.ui.graphics.Color
import com.google.firebase.firestore.Exclude

data class ChartData(
    val value: Float,
    val label: String,

    @get:Exclude
    val color: Color
) {
    // 2. The required empty constructor
    constructor() : this(0f, "", Color.Transparent)
}
