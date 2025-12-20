package com.example.qlinic.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qlinic.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarNav(
    title: String,
    onNotificationClick: () -> Unit,
    onLogoClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(
                    onClick = onLogoClick,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(50.dp)
                        .clip(CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_logosmall),
                        contentDescription = "Qlinic Logo",
                        modifier = Modifier.size(40.dp)
                    )
                }
            },
            actions = {
                IconButton(onClick = onNotificationClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_notification),
                        contentDescription = "Notifications",
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        Text(
            text = title,
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 22.sp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 16.dp),
            textAlign = TextAlign.Center
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}
