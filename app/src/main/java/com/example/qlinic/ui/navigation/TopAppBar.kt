package com.example.qlinic.ui.navigation

import android.R.attr.end
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.qlinic.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(navController: NavController){

    val onNotificationClick = { navController.navigate(Routes.Notifications.route) }

    TopAppBar(
        title = {
            Row(
                modifier = Modifier
                    .padding(start = 4.dp, end = 8.dp, bottom = 4.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // logo Qlinic at left
                Image(
                    painter = painterResource(R.drawable.group_14)    ,
                    contentDescription = "Qlinic Logo",
                    modifier = Modifier.size(100.dp)
                )

                // Notification icon at right
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNotificationClick) {
                        Icon(
                            painter = painterResource(R.drawable.notification),
                            contentDescription = "Notification Icon",

                        )
                    }
                }

            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun TopBarPreview() {
    TopBar(navController = rememberNavController())
}