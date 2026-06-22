package com.watermelon.app.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.watermelon.core.designsystem.theme.WatermelonRed
import com.watermelon.domain.model.User
import java.text.NumberFormat
import java.util.Locale

private val CardBackground = Color(0xFF111111)
private val CardBorder = Color(0xFF1A1A1A)
private val SecondaryText = Color(0xFF888888)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val profileStats by viewModel.profileStats.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val editState by viewModel.editState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    if (!editState.isEditing) {
                        IconButton(onClick = { viewModel.toggleEdit() }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit profile")
                        }
                    } else {
                        IconButton(onClick = { viewModel.toggleEdit() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel edit")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (editState.isEditing) {
            ProfileEditContent(
                paddingValues = padding,
                user = user,
                editState = editState,
                viewModel = viewModel,
                onLogout = onLogout
            )
        } else {
            GamifiedProfileContent(
                paddingValues = padding,
                user = user,
                isPremium = isPremium,
                profileStats = profileStats,
                achievements = achievements,
                isLoading = isLoading,
                viewModel = viewModel,
                onLogout = onLogout
            )
        }
    }
}

@Composable
fun GamifiedProfileContent(
    paddingValues: PaddingValues,
    user: User?,
    isPremium: Boolean,
    profileStats: com.watermelon.domain.model.ProfileStats?,
    achievements: List<com.watermelon.domain.model.AchievementBadge>,
    isLoading: Boolean,
    viewModel: ProfileViewModel,
    onLogout: () -> Unit
) {
    val scrollState = rememberScrollState()
    val rankColor = getRankColor(profileStats?.rankTier)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // A. HEADER SECTION
        ProfileHeader(
            user = user,
            profileStats = profileStats,
            rankColor = rankColor,
            isPremium = isPremium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // B. PROGRESSION BAR
        if (profileStats != null) {
            LevelProgressBar(
                xpTotal = profileStats.xpTotal,
                level = profileStats.xpLevel,
                rankTier = profileStats.rankTier,
                viewModel = viewModel
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // C. STATS GRID
        if (profileStats != null) {
            StatsGrid(profileStats = profileStats)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // D. ACHIEVEMENT BADGES
        AchievementsSection(achievements = achievements)

        Spacer(modifier = Modifier.height(24.dp))

        // E. LISTENING ANALYTICS
        if (profileStats != null) {
            ListeningAnalytics(profileStats = profileStats)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // F. LOGOUT BUTTON
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ProfileHeader(
    user: User?,
    profileStats: com.watermelon.domain.model.ProfileStats?,
    rankColor: Color,
    isPremium: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aura")
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraScale"
    )
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraAlpha"
    )

    Box(contentAlignment = Alignment.Center) {
        // Aura glow ring
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(auraScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            rankColor.copy(alpha = auraAlpha),
                            rankColor.copy(alpha = 0f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Avatar with glow border
        Box(
            modifier = Modifier
                .size(120.dp)
                .border(4.dp, rankColor, CircleShape)
                .clip(CircleShape)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val avatarUrl = user?.avatarUrl
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Avatar",
                    modifier = Modifier.size(64.dp),
                    tint = WatermelonRed
                )
            }
        }

        // Online indicator
        Box(
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.BottomEnd)
                .background(Color(0xFF4CAF50), CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Username + Display Name
    Text(
        text = user?.displayName ?: user?.email?.substringBefore("@") ?: "Guest",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )

    if (!user?.username.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "@${user!!.username}",
            style = MaterialTheme.typography.bodyLarge,
            color = SecondaryText
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Rank Badge
    profileStats?.let { stats ->
        Card(
            colors = CardDefaults.cardColors(
                containerColor = rankColor.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = "${stats.rankTier}",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = rankColor
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Level + XP
    profileStats?.let { stats ->
        Text(
            text = "Level ${stats.xpLevel} · ${NumberFormat.getNumberInstance(Locale.US).format(stats.xpTotal)} XP",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText
        )
    }

    // Premium badge
    if (isPremium) {
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = WatermelonRed.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = WatermelonRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Premium Member",
                    style = MaterialTheme.typography.labelLarge,
                    color = WatermelonRed
                )
            }
        }
    }
}

@Composable
fun LevelProgressBar(
    xpTotal: Long,
    level: Int,
    rankTier: String,
    viewModel: ProfileViewModel
) {
    val progress = viewModel.calculateLevelProgress(xpTotal, level)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000),
        label = "progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Level Progress",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WatermelonRed,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Animated progress bar with glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF2A2A2A))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(12.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(WatermelonRed, WatermelonRed.copy(alpha = 0.7f))
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rank transition text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rankTier,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                Text(
                    text = getNextRank(rankTier),
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }
        }
    }
}

@Composable
fun StatsGrid(profileStats: com.watermelon.domain.model.ProfileStats) {
    val stats = listOf(
        Triple("⏱", "Hours", formatHours(profileStats.hoursListened)),
        Triple("🎵", "Songs", profileStats.songsPlayed.toString()),
        Triple("🔥", "Streak", "${profileStats.streakDays} days"),
        Triple("📀", "Playlists", profileStats.playlistsCreated.toString()),
        Triple("🌊", "Artists", profileStats.artistsDiscovered.toString()),
        Triple("❤️", "Liked", profileStats.likedSongsCount.toString())
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                emoji = stats[0].first,
                label = stats[0].second,
                value = stats[0].third
            )
            StatCard(
                modifier = Modifier.weight(1f),
                emoji = stats[1].first,
                label = stats[1].second,
                value = stats[1].third
            )
            StatCard(
                modifier = Modifier.weight(1f),
                emoji = stats[2].first,
                label = stats[2].second,
                value = stats[2].third
            )
        }
        // Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                emoji = stats[3].first,
                label = stats[3].second,
                value = stats[3].third
            )
            StatCard(
                modifier = Modifier.weight(1f),
                emoji = stats[4].first,
                label = stats[4].second,
                value = stats[4].third
            )
            StatCard(
                modifier = Modifier.weight(1f),
                emoji = stats[5].first,
                label = stats[5].second,
                value = stats[5].third
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun AchievementsSection(achievements: List<com.watermelon.domain.model.AchievementBadge>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "Achievements",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (achievements.isEmpty()) {
            // Placeholder achievement badges
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val placeholderBadges = listOf(
                    Triple("🎖", "First Steps", true),
                    Triple("🌟", "Rising Star", true),
                    Triple("🎯", "Perfect Pitch", false),
                    Triple("🔮", "Deep Listener", false),
                    Triple("⚡", "Speed Demon", false)
                )
                items(placeholderBadges) { (emoji, name, unlocked) ->
                    AchievementBadgeChip(
                        emoji = emoji,
                        name = name,
                        unlocked = unlocked
                    )
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(achievements) { badge ->
                    AchievementBadgeChip(
                        emoji = badge.emoji,
                        name = badge.name,
                        unlocked = badge.unlockedAt != null
                    )
                }
            }
        }
    }
}

@Composable
fun AchievementBadgeChip(
    emoji: String,
    name: String,
    unlocked: Boolean
) {
    val borderColor by animateColorAsState(
        targetValue = if (unlocked) WatermelonRed else Color.Gray,
        label = "borderColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (unlocked) Color.White else Color.Gray,
        label = "textColor"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) WatermelonRed.copy(alpha = 0.1f) else Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (unlocked) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                fontWeight = if (unlocked) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun ListeningAnalytics(profileStats: com.watermelon.domain.model.ProfileStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Listening",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Activity bars placeholder
            Text(
                text = "Weekly Activity",
                style = MaterialTheme.typography.labelMedium,
                color = SecondaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            WeeklyActivityBars()

            Spacer(modifier = Modifier.height(16.dp))

            // Top genre
            profileStats.topGenre?.let { genre ->
                Text(
                    text = "Top Genre",
                    style = MaterialTheme.typography.labelMedium,
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val genres = listOf(genre, "Pop", "Rock", "Jazz", "Electronic")
                    items(genres) { g ->
                        GenrePill(genre = g, isHighlighted = g == genre)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Top artist
            profileStats.topArtist?.let { artist ->
                Text(
                    text = "Top Artist",
                    style = MaterialTheme.typography.labelMedium,
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun WeeklyActivityBars() {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val heights = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.6f, 0.8f, 0.3f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEachIndexed { index, day ->
            val barHeight by animateFloatAsState(
                targetValue = heights[index],
                animationSpec = tween(500),
                label = "barHeight$index"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height((50 * barHeight).dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(WatermelonRed.copy(alpha = 0.8f))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText
                )
            }
        }
    }
}

@Composable
fun GenrePill(genre: String, isHighlighted: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) WatermelonRed.copy(alpha = 0.2f) else Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isHighlighted) androidx.compose.foundation.BorderStroke(1.dp, WatermelonRed) else null
    ) {
        Text(
            text = genre,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (isHighlighted) WatermelonRed else Color.White
        )
    }
}

@Composable
fun ProfileEditContent(
    paddingValues: PaddingValues,
    user: User?,
    editState: EditState,
    viewModel: ProfileViewModel,
    onLogout: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val avatar = editState.avatarUrl
            if (avatar.isNotBlank()) {
                AsyncImage(
                    model = avatar,
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Avatar",
                    modifier = Modifier.size(64.dp),
                    tint = WatermelonRed
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = editState.displayName,
            onValueChange = { viewModel.setDisplayName(it) },
            label = { Text("Display Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = editState.username,
            onValueChange = { viewModel.setUsername(it) },
            label = { Text("Username") },
            singleLine = true,
            prefix = { Text("@") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        if (editState.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = editState.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Avatar Style",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        val dicebearStyles = remember {
            listOf(
                "toon-head" to "Toon",
                "adventurer" to "Adventure",
                "avataaars" to "Avatar",
                "big-ears" to "Ears",
                "bottts" to "Bot",
                "fun-emoji" to "Emoji",
                "lorelei" to "Lorelei",
                "notionists" to "Notion"
            )
        }
        val seed = user?.username ?: user?.email ?: "user"
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(dicebearStyles) { (style, label) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = "https://api.dicebear.com/10.x/$style/svg?seed=$seed",
                        contentDescription = label,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { viewModel.updateAvatar(seed, style) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                val randomStyle = dicebearStyles.random().first
                                val randomSeed = (1000..9999).random().toString()
                                viewModel.updateAvatar(randomSeed, randomStyle)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = "Random",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Random",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.saveProfile() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !editState.isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = WatermelonRed)
        ) {
            if (editState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Save", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun formatHours(hours: Float): String {
    return if (hours >= 1000) {
        String.format(Locale.US, "%.1fk", hours / 1000)
    } else {
        String.format(Locale.US, "%.1f", hours)
    }
}

private fun getNextRank(currentRank: String): String {
    val ranks = listOf(
        "🌱 Seed Listener",
        "🍃 Sprout Wave",
        "🎧 Pulse Rider",
        "🌊 Echo Drift",
        "🎶 Resonance",
        "📀 Vinyl Hunter",
        "🎵 Frequency Soul",
        "🌌 NovaBeat",
        "💿 Harmonic Flow",
        "🔥 Reverb X",
        "⚡ Soundrift",
        "🌠 Celestia Tone",
        "🎼 Wave Architect",
        "🌈 Spectrum Lord",
        "👑 Eternal Echo"
    )
    val currentIndex = ranks.indexOf(currentRank)
    return if (currentIndex >= 0 && currentIndex < ranks.size - 1) {
        ranks[currentIndex + 1]
    } else {
        "👑 Eternal Echo"
    }
}