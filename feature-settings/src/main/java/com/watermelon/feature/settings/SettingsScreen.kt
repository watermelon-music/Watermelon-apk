package com.watermelon.feature.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import com.watermelon.core.designsystem.theme.AppTheme
import com.watermelon.core.designsystem.theme.ThemeManager
import com.watermelon.domain.model.SubscriptionPlan

private val AvatarColors = listOf(
    Color(0xFFDC2626),
    Color(0xFF2563EB),
    Color(0xFF16A34A),
    Color(0xFFD97706),
    Color(0xFF9333EA),
    Color(0xFFDB2777),
    Color(0xFF0891B2),
    Color(0xFF4B5563)
)

object AvatarManager {
    private const val PREFS = "watermelon_avatar"
    private const val KEY_COLOR_INDEX = "avatar_color_index"

    fun save(context: Context, colorIndex: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_COLOR_INDEX, colorIndex).apply()
    }

    fun get(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_COLOR_INDEX, 0)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogoutComplete: () -> Unit,
    
    onNavigateToAbout: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val cacheCleared by viewModel.cacheCleared.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    val plan = user?.plan ?: SubscriptionPlan.FREE
    

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
        ) {
            // Autoplay is now permanently enabled and hidden from settings
            SettingsItem(
                icon = Icons.Default.Share,
                title = "Share App",
                subtitle = "Invite friends",
                onClick = {
                    val shareText = "Check out Watermelon - the best free music app! 🍉\n\nDownload directly to your phone with one click:\nhttps://watermelon-api-oxx2.onrender.com/share"
                    val cacheFile = java.io.File(context.cacheDir, "watermelon_share.jpg")
                    try {
                        if (!cacheFile.exists()) {
                            context.resources.openRawResource(com.watermelon.core.designsystem.R.drawable.watermelon_field).use { input ->
                                java.io.FileOutputStream(cacheFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        
                        val contentUri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "com.watermelon.app.fileprovider",
                            cacheFile
                        )
                        
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, contentUri)
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share App"))
                    } catch (e: Exception) {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    }
                }
            )
            SettingsItem(
                icon = Icons.Default.Info,
                title = "About",
                subtitle = "Watermelon v$versionName",
                onClick = onNavigateToAbout
            )

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                )
            ) {
                Text("Delete Account", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.logout(onLogoutComplete) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", style = MaterialTheme.typography.labelLarge)
            }
        }
        }
    }

    if (cacheCleared) {
        LaunchedEffect(Unit) {
            viewModel.resetCacheFlag()
        }
    }

    
