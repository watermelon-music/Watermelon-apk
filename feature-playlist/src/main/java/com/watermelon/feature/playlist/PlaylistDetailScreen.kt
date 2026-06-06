package com.watermelon.feature.playlist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.watermelon.core.designsystem.theme.WatermelonSpacing
import com.watermelon.domain.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onBackClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onShuffleClick: () -> Unit = {},
    onPlayAllClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val songs = remember(playlistId) { getMockSongsForPlaylist(playlistId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playlist") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val ids = songs.joinToString(",") { it.id }
                        val link = "watermelon://playlist?songs=$ids"
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Playlist", link))
                        Toast.makeText(context, "Playlist link copied!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(WatermelonSpacing.md)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = WatermelonSpacing.md),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = "https://picsum.photos/seed/$playlistId/400/400",
                        contentDescription = "Playlist cover",
                        modifier = Modifier.fillMaxSize()
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = "Playlist",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
            ) {
                Button(
                    onClick = onPlayAllClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play All")
                }
                OutlinedButton(
                    onClick = onShuffleClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Shuffle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shuffle")
                }
            }

            Spacer(modifier = Modifier.height(WatermelonSpacing.md))

            Text(
                text = "Songs",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = WatermelonSpacing.md)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
            ) {
                items(songs, key = { it.id }) { song ->
                    PlaylistSongItem(song = song, onClick = { onSongClick(song) })
                }
            }
        }
    }
}

private fun getMockSongsForPlaylist(playlistId: String): List<Song> {
    val pool = listOf(
        Song("dQw4w9WgXcQ","Never Gonna Give You Up","rick","Rick Astley",null,null,213000L,"https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg","https://www.youtube.com/watch?v=dQw4w9WgXcQ","Pop","1987"),
        Song("9bZkp7q19f0","Gangnam Style","psy","PSY",null,null,253000L,"https://i.ytimg.com/vi/9bZkp7q19f0/hqdefault.jpg","https://www.youtube.com/watch?v=9bZkp7q19f0","K-Pop","2012"),
        Song("kJQP7kiw5Fk","Despacito","luis","Luis Fonsi",null,null,229000L,"https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg","https://www.youtube.com/watch?v=kJQP7kiw5Fk","Latin","2017"),
        Song("JGwWNGJdvx8","Shape of You","ed","Ed Sheeran",null,null,234000L,"https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg","https://www.youtube.com/watch?v=JGwWNGJdvx8","Pop","2017"),
        Song("RgKAFK5djSk","See You Again","wiz","Wiz Khalifa",null,null,230000L,"https://i.ytimg.com/vi/RgKAFK5djSk/hqdefault.jpg","https://www.youtube.com/watch?v=RgKAFK5djSk","Hip-Hop","2015"),
        Song("OPf0YbXqDm0","Uptown Funk","mark","Mark Ronson",null,null,270000L,"https://i.ytimg.com/vi/OPf0YbXqDm0/hqdefault.jpg","https://www.youtube.com/watch?v=OPf0YbXqDm0","Funk","2014"),
        Song("CevxZvSJLk8","Roar","katy","Katy Perry",null,null,231000L,"https://i.ytimg.com/vi/CevxZvSJLk8/hqdefault.jpg","https://www.youtube.com/watch?v=CevxZvSJLk8","Pop","2013"),
        Song("pRpeEdMmmQ0","Waka Waka","shakira","Shakira",null,null,220000L,"https://i.ytimg.com/vi/pRpeEdMmmQ0/hqdefault.jpg","https://www.youtube.com/watch?v=pRpeEdMmmQ0","Pop","2010")
    )
    return when (playlistId) {
        "p1" -> pool.take(4)
        "p2" -> pool.drop(2).take(4)
        else -> pool.shuffled().take(4)
    }
}

@Composable
private fun PlaylistSongItem(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.coverUrl,
            contentDescription = song.title,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(WatermelonSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artistName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
