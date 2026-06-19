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
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.res.painterResource
import com.watermelon.core.designsystem.theme.AppTheme
import com.watermelon.core.designsystem.theme.ThemeManager
import com.watermelon.core.designsystem.theme.WatermelonRed
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
    onNavigateToProfile: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val cacheCleared by viewModel.cacheCleared.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showAvatarDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    val plan = user?.plan ?: SubscriptionPlan.FREE
    val avatarColor = remember { AvatarColors.getOrNull(AvatarManager.get(context)) ?: AvatarColors.first() }

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
            // Profile card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { onNavigateToProfile() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(avatarColor)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!user?.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = user?.avatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val initial = user?.displayName?.firstOrNull()
                                ?: user?.email?.firstOrNull()
                                ?: 'U'
                            Text(
                                text = initial.uppercase().toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = user?.displayName?.takeIf { it.isNotBlank() }
                                ?: user?.email?.takeIf { it.isNotBlank() }
                                ?: "Guest User",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!user?.username.isNullOrBlank()) {
                            Text(
                                text = "@${user!!.username}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (!user?.email.isNullOrBlank()) {
                            Text(
                                text = user!!.email.substringBefore("@"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (plan != SubscriptionPlan.FREE) {
                            Text(
                                text = plan.name.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall,
                                color = WatermelonRed
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )
            SettingsItem(
                icon = Icons.Default.Brush,
                title = "Theme",
                subtitle = currentThemeLabel(context),
                onClick = { showThemeDialog = true }
            )
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

    if (showThemeDialog) {
        ThemeSelectorDialog(
            currentMode = ThemeManager.get(context),
            currentPlan = plan,
            onDismiss = { showThemeDialog = false },
            onSelect = { mode ->
                ThemeManager.save(context, mode)
                showThemeDialog = false
                (context as? Activity)?.recreate()
            },
            onNavigateToPremium = {
                showThemeDialog = false
                onNavigateToPremium()
            }
        )
    }

    if (showAvatarDialog) {
        AvatarPickerDialog(
            selectedIndex = AvatarManager.get(context),
            onDismiss = { showAvatarDialog = false },
            onSelect = { index ->
                AvatarManager.save(context, index)
                showAvatarDialog = false
                (context as? Activity)?.recreate()
            }
        )
    }

    if (showDeleteDialog) {
        DeleteAccountDialog(
            state = viewModel.deleteState.collectAsStateWithLifecycle().value,
            onDismiss = { showDeleteDialog = false },
            onConfirm = { viewModel.deleteAccount { onLogoutComplete() } }
        )
    }
}

private fun currentThemeLabel(context: Context): String {
    return AppTheme.fromKey(ThemeManager.get(context)).label
}

private fun themePreviewColor(theme: AppTheme): Color {
    return when (theme) {
        AppTheme.Light -> Color(0xFFDC2626)
        AppTheme.Dark -> Color(0xFFE53935)
        else -> Color(0xFFDC2626)
    }
}

@Composable
private fun ThemeSelectorDialog(
    currentMode: String,
    currentPlan: SubscriptionPlan,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onNavigateToPremium: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "App Appearance",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppTheme.all.forEach { theme ->
                    val isSelected = currentMode == theme.key
                    val (bgColor, topColor, textColor) = when (theme) {
                        AppTheme.Light -> Triple(Color(0xFFF8F8F8), Color(0xFFDC2626), Color.Black)
                        AppTheme.Dark -> Triple(Color(0xFF1A1A1A), Color(0xFFDC2626), Color.White)
                        else -> Triple(MaterialTheme.colorScheme.surface, WatermelonRed, MaterialTheme.colorScheme.onSurface)
                    }

                    Card(
                        onClick = { onSelect(theme.key) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        border = if (isSelected) BorderStroke(2.dp, WatermelonRed) else null,
                        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Mini phone preview
                            Box(
                                modifier = Modifier
                                    .size(width = 42.dp, height = 58.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.5.dp, WatermelonRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .background(bgColor)
                            ) {
                                // Top bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(14.dp)
                                        .background(topColor)
                                )
                                // Content lines
                                Column(
                                    modifier = Modifier
                                        .padding(top = 18.dp, start = 4.dp, end = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    repeat(3) { i ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(if (i == 2) 0.6f else 1f)
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(textColor.copy(alpha = 0.2f))
                                        )
                                    }
                                }
                                // Red accent dot
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(WatermelonRed)
                                )
                            }

                            // Label
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = theme.label,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = textColor
                                    )
                                )
                                Text(
                                    text = when (theme) {
                                        AppTheme.Light -> "Bright white background"
                                        AppTheme.Dark -> "Deep dark background"
                                        else -> ""
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = textColor.copy(alpha = 0.6f)
                                    )
                                )
                            }

                            // Checkmark
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(WatermelonRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, textColor.copy(alpha = 0.2f), CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", style = MaterialTheme.typography.labelLarge)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
private fun AvatarPickerDialog(
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick Avatar Color") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(180.dp)
            ) {
                items(AvatarColors.size) { index ->
                    val color = AvatarColors[index]
                    val selected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { onSelect(index) }
                            .then(
                                if (selected) {
                                    Modifier.border(3.dp, Color.White, CircleShape)
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Text("✓", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun DeleteAccountDialog(
    state: DeleteAccountState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var confirmation by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!state.isDeleting) onDismiss() },
        icon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Delete Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "This will permanently delete your account, playlists, favorites and all data. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Type \"DELETE\" below to confirm.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = { Text("Confirmation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (state.error != null) {
                    Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = confirmation == "DELETE" && !state.isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                if (state.isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Delete Forever", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isDeleting) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = WatermelonRed,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = isChecked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = WatermelonRed,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
