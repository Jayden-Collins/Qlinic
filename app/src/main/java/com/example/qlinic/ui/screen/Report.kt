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
fun ReportScreen(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "This is the Report Screen")
    }
}

@Preview(showBackground = true)
@Composable
fun ReportPreview() {
    QlinicTheme {
        ReportScreen(paddingValues = PaddingValues())
    }
}
