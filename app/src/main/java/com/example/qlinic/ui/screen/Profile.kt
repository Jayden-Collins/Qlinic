package com.example.qlinic.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.qlinic.ui.theme.QlinicTheme

@Composable
fun Profile(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "This is the Profile Screen")
    }
}

@Preview(showBackground = true)
@Composable
fun ProfilePreview() {
    QlinicTheme {
        Profile(paddingValues = PaddingValues())
    }
}
