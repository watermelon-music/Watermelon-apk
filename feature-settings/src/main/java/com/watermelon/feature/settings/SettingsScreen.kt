package com.watermelon.feature.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
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
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(avatarColor),
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
                                color = Color.White
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
                        if (!user?.email.isNullOrBlank()) {
                            Text(
                                text = user!!.email,
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
                icon = Icons.Default.Person,
                title = "Avatar",
                subtitle = "Pick a profile color",
                onClick = { showAvatarDialog = true }
            )
            SettingsItem(
                icon = Icons.Default.Brush,
                title = "Theme",
                subtitle = currentThemeLabel(context),
                onClick = { showThemeDialog = true }
            )
            SettingsItem(
                icon = Icons.Default.CleaningServices,
                title = "Clear Cache",
                subtitle = "Free up local storage",
                onClick = { viewModel.clearCache() }
            )
            SettingsItem(
                icon = Icons.Default.Share,
                title = "Share App",
                subtitle = "Invite friends",
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Check out Watermelon - the best free music app!")
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                }
            )
            SettingsItem(
                icon = Icons.Default.Info,
                title = "About",
                subtitle = "Watermelon v$versionName",
                onClick = onNavigateToAbout
            )

            Spacer(modifier = Modifier.weight(1f))

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
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", style = MaterialTheme.typography.labelLarge)
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
}

private fun currentThemeLabel(context: Context): String {
    return AppTheme.fromKey(ThemeManager.get(context)).label
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
        title = { Text("Choose Theme") },
        text = {
            Column {
                AppTheme.all.forEach { theme ->
                    val locked = when {
                        theme.requiresStudent -> currentPlan != SubscriptionPlan.STUDENT && currentPlan != SubscriptionPlan.PREMIUM_INDIVIDUAL && currentPlan != SubscriptionPlan.PREMIUM_FAMILY
                        theme.requiresPremium -> currentPlan != SubscriptionPlan.PREMIUM_INDIVIDUAL && currentPlan != SubscriptionPlan.PREMIUM_FAMILY
                        else -> false
                    }
                    val selected = currentMode == theme.key
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !locked) {
                                if (locked) onNavigateToPremium() else onSelect(theme.key)
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = {
                                if (locked) onNavigateToPremium() else onSelect(theme.key)
                            },
                            enabled = !locked,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = WatermelonRed,
                                disabledSelectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = theme.label + if (locked) " (Locked)" else "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (locked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface
                        )
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
private fun AvatarPickerDialog(
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick Avatar Color") },
        text = {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(AvatarColors.size) { index ->
                    val color = AvatarColors[index]
                    val selected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { onSelect(index) }
                            .then(
                                if (selected) Modifier.padding(2.dp)
                                    .background(Color.White, CircleShape)
                                    .padding(2.dp)
                                    .background(color, CircleShape)
                                else Modifier
                            )
                    )
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
