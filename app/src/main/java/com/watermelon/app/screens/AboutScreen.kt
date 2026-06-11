package com.watermelon.app.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.watermelon.app.BuildConfig
import com.watermelon.app.R
import com.watermelon.core.designsystem.theme.WatermelonRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App logo
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Watermelon",
                modifier = Modifier
                    .size(120.dp)
                    .padding(16.dp)
            )

            Text(
                text = "Watermelon",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}  Build ${BuildConfig.VERSION_CODE}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Made with passion for music lovers everywhere.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Your personal music universe. Stream millions of songs, discover new artists, build playlists, and enjoy curated radio stations from around the world.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Action buttons
            OutlinedButton(
                onClick = { /* TODO: open privacy policy URL */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Policy, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Privacy Policy")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { /* TODO: open terms URL */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Terms of Service")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { /* TODO: open support email */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.MailOutline, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Contact Support")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "2026 Watermelon Music. All rights reserved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
