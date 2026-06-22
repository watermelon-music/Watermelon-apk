package com.watermelon.app.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.watermelon.domain.model.AchievementBadge
import com.watermelon.domain.model.ProfileStats
import com.watermelon.domain.model.User
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    onBack: () -> Unit,
) {
    val user by viewModel.user.collectAsState()
    val stats by viewModel.profileStats.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val editState by viewModel.editState.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showEditSheet by remember { mutableStateOf(false) }

    if (showEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            EditProfileSheet(
                editState = editState,
                user = user,
                onDisplayNameChange = viewModel::setDisplayName,
                onUsernameChange = viewModel::setUsername,
                onAvatarSelected = { seed -> viewModel.updateAvatar(seed) },
                onSave = {
                    viewModel.saveProfile()
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showEditSheet = false }
                },
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showEditSheet = false }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back", color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = { showEditSheet = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit profile")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            ProfileHero(user, stats, isLoading) { showEditSheet = true }
            Spacer(modifier = Modifier.height(24.dp))
            if (stats != null) XpProgressSection(stats!!, viewModel::calculateLevelProgress)
            Spacer(modifier = Modifier.height(24.dp))
            if (stats != null) QuickStatsGrid(stats!!, achievements.size)
            Spacer(modifier = Modifier.height(24.dp))
            if (achievements.isNotEmpty()) {
                AchievementsSection(achievements)
                Spacer(modifier = Modifier.height(24.dp))
            }
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileHero(user: User?, stats: ProfileStats?, isLoading: Boolean, onAvatarClick: () -> Unit) {
    val rankColor = getRankColor(stats?.rankTier)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(88.dp).clip(CircleShape)
                    .background(rankColor.copy(alpha = 0.15f))
                    .border(3.dp, rankColor, CircleShape)
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(user?.avatarUrl).crossfade(true).build(),
                    contentDescription = "Profile photo",
                    modifier = Modifier.size(76.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd).size(24.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "Change photo", modifier = Modifier.size(14.dp), tint = Color.White)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(user?.displayName ?: user?.username ?: "User", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("@${user?.username ?: "user"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                stats?.rankTier?.let { rank ->
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            .background(rankColor.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(rank, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = rankColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun XpProgressSection(stats: ProfileStats, calculateProgress: (Long, Int) -> Float) {
    val progress by animateFloatAsState(
        targetValue = calculateProgress(stats.xpTotal, stats.xpLevel),
        animationSpec = tween(800), label = "xp_progress"
    )
    val next = ((stats.xpLevel + 1) * (stats.xpLevel + 1) * 100).toLong()
    val base = (stats.xpLevel * stats.xpLevel * 100).toLong()
    val into = stats.xpTotal - base
    val need = next - base
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Level ${stats.xpLevel}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("$into / $need XP", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(progress).height(10.dp).clip(RoundedCornerShape(5.dp))
                    .background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)))
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("${(progress * 100).toInt()}% to Level ${stats.xpLevel + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QuickStatsGrid(stats: ProfileStats, achievementsCount: Int) {
    val items = listOf(
        Triple("Songs", "${stats.songsPlayed}", Icons.Outlined.MusicNote),
        Triple("Hours", "%.1f".format(stats.hoursListened), Icons.Outlined.History),
        Triple("Streak", "${stats.streakDays}d", Icons.Outlined.LocalFireDepartment),
        Triple("Badges", "$achievementsCount", Icons.Outlined.EmojiEvents),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { (label, value, icon) ->
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AchievementsSection(achievements: List<AchievementBadge>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Achievements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
            items(achievements, key = { it.id }) { badge -> BadgeChip(badge) }
        }
    }
}

@Composable
private fun BadgeChip(badge: AchievementBadge) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(badge.emoji, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(badge.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun EditProfileSheet(
    editState: EditState, user: User?,
    onDisplayNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onAvatarSelected: (String) -> Unit,
    onSave: () -> Unit, onDismiss: () -> Unit
) {
    var selectedSeed by remember { mutableStateOf(user?.username ?: "user") }
    var selectedStyle by remember { mutableStateOf("toon-head") }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> uri?.let { onAvatarSelected(it.toString()) } }
    val styles = listOf("toon-head" to "Toon", "adventurer" to "Adventurer", "open-peeps" to "Peeps", "bottts" to "Robot", "fun-emoji" to "Emoji")

    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text("Edit Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = editState.avatarUrl.ifBlank { "https://api.dicebear.com/10.x/$selectedStyle/svg?seed=$selectedSeed" },
                contentDescription = "Avatar preview",
                modifier = Modifier.size(72.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Choose Avatar", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { photoPicker.launch("image/*") }) { Text("Gallery") }
                    TextButton(onClick = { selectedSeed = System.currentTimeMillis().toString(); onAvatarSelected("https://api.dicebear.com/10.x/$selectedStyle/svg?seed=$selectedSeed") }) { Text("Random") }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(styles) { (style, label) ->
                val isSelected = selectedStyle == style
                Card(
                    modifier = Modifier.clickable { selectedStyle = style; onAvatarSelected("https://api.dicebear.com/10.x/$style/svg?seed=$selectedSeed") },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = editState.displayName, onValueChange = onDisplayNameChange, label = { Text("Display Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = editState.username, onValueChange = onUsernameChange, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        editState.error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(onClick = onSave, modifier = Modifier.weight(1f), enabled = !editState.isSaving, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                if (editState.isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Save")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
