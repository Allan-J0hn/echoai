package com.example.echoai.ui.permissions

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.echoai.ui.theme.Dimens

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Dimens.spacing_l),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Permissions Required",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Dimens.spacing_m))
        Text(
            "This app needs access to your microphone and notifications to function properly.",
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Dimens.spacing_xl))
        Button(onClick = onRequestPermission) {
            Text("Grant Permissions")
        }
    }
}
