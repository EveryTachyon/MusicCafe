package com.example.musiccafe

import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musiccafe.ui.theme.MusicCafeTheme

private val SidebarBackground = Color(0xFF100E13)
private val ContentBackground = Color(0xFF18161C)
private val SidebarText = Color(0xFF8B898C)
private val AccentGreen = Color(0xFF00D51B)
private val CardBackground = Color(0xFF242128)
private val SoftText = Color(0xFFB9B6BC)

private val libraryItems = listOf("Playlists", "Tracks", "Artists", "Albums", "Import songs")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicCafeTheme {
                MusicCafeApp()
            }
        }
    }
}

@Composable
fun MusicCafeSidebar() {
    MusicCafeApp()
}

@Composable
private fun MusicCafeApp() {
    val context = LocalContext.current
    var selectedItem by remember { mutableStateOf("Home") }
    var isSidebarOpen by remember { mutableStateOf(true) }
    var importedSongs by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val chooseFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { folderUri ->
        if (folderUri != null) {
            importedSongs = scanAudioFiles(context.contentResolver, folderUri)
        }
    }
    val chooseAudioFiles = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { audioUris ->
        importedSongs = audioUris
    }

    Box(modifier = Modifier.fillMaxSize().background(ContentBackground)) {
        LandingContent(
            selectedItem = selectedItem,
            onOpenSidebar = { isSidebarOpen = true },
            importedSongs = importedSongs,
            onChooseFolder = { chooseFolder.launch(null) },
            onChooseAudioFiles = { chooseAudioFiles.launch(arrayOf("audio/*")) }
        )
        if (isSidebarOpen) {
            MusicCafeNavigation(
                selectedItem = selectedItem,
                onItemSelected = {
                    selectedItem = it
                    isSidebarOpen = false
                },
                onClose = { isSidebarOpen = false }
            )
        }
    }
}

@Composable
private fun MusicCafeNavigation(
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(SidebarBackground)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "MusicCafe",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close sidebar",
                    tint = SidebarText
                )
            }
        }
        Spacer(modifier = Modifier.height(34.dp))
        SidebarItem("Home", selectedItem, onItemSelected)
        Spacer(modifier = Modifier.height(24.dp))
        SidebarSection(title = "Library")
        libraryItems.forEach { item ->
            SidebarItem(item, selectedItem, onItemSelected)
        }
        Spacer(modifier = Modifier.height(36.dp))
        SidebarSection(title = "Playlists")
        SidebarItem("Create playlist", selectedItem, onItemSelected)
        Spacer(modifier = Modifier.weight(1f))
        SidebarItem("Settings", selectedItem, onItemSelected)
    }
}

@Composable
private fun SidebarSection(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 23.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Composable
private fun SidebarItem(
    label: String,
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .height(64.dp)
            .fillMaxWidth()
            .background(
                color = if (label == selectedItem) Color(0xFF28252D) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onItemSelected(label) }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (label == selectedItem) AccentGreen else SidebarText,
            fontSize = 20.sp,
            fontWeight = if (label == selectedItem) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LandingContent(
    selectedItem: String,
    onOpenSidebar: () -> Unit,
    importedSongs: List<Uri>,
    onChooseFolder: () -> Unit,
    onChooseAudioFiles: () -> Unit
) {
    val details = when (selectedItem) {
        "Home" -> PageDetails("Welcome back", "Your music, gathered in one place.", listOf("Recently played", "Made for you", "New additions"))
        "Playlists" -> PageDetails("Your playlists", "Organize the songs that belong together.", listOf("Morning commute", "Late night", "Favorites"))
        "Tracks" -> PageDetails("Tracks", "Browse every song in your MusicCafe library.", listOf("No tracks imported yet", "Import songs to start building your library"))
        "Artists" -> PageDetails("Artists", "Find music by the people who made it.", listOf("Your artists will appear here"))
        "Albums" -> PageDetails("Albums", "Keep complete records together.", listOf("Your albums will appear here"))
        "Import songs" -> PageDetails("Import songs", "Bring your local collection into MusicCafe.", listOf("Choose a folder", "Scan for audio files", "Review imported songs"))
        "Settings" -> PageDetails("Settings", "Manage your MusicCafe preferences.", listOf("Playback", "Appearance", "Library settings"))
        else -> PageDetails("Create playlist", "Start a new collection for any mood.", listOf("Name your playlist", "Add songs", "Save playlist"))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenSidebar) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = "Open sidebar",
                        tint = Color.White
                    )
                }
                Text(
                    text = details.title,
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                text = details.subtitle,
                color = SoftText,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        items(details.cards) { cardTitle ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        when {
                            selectedItem == "Import songs" && cardTitle == "Choose a folder" -> onChooseFolder()
                            selectedItem == "Import songs" && cardTitle == "Scan for audio files" -> onChooseAudioFiles()
                        }
                    },
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = cardTitle,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
        if (selectedItem == "Import songs" && importedSongs.isNotEmpty()) {
            item {
                Text(
                    text = "${importedSongs.size} item${if (importedSongs.size == 1) "" else "s"} ready to review",
                    color = AccentGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private data class PageDetails(
    val title: String,
    val subtitle: String,
    val cards: List<String>
)

@Preview(showBackground = true, widthDp = 900, heightDp = 800)
@Composable
private fun MusicCafePreview() {
    MusicCafeTheme(dynamicColor = false, darkTheme = true) {
        MusicCafeApp()
    }
}

private fun scanAudioFiles(contentResolver: android.content.ContentResolver, treeUri: Uri): List<Uri> {
    val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
    val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocumentId)
    return scanAudioFiles(contentResolver, childUri, treeUri)
}

private fun scanAudioFiles(
    contentResolver: android.content.ContentResolver,
    directoryUri: Uri,
    treeUri: Uri
): List<Uri> {
    val audioFiles = mutableListOf<Uri>()
    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_MIME_TYPE
    )

    contentResolver.query(directoryUri, projection, null, null, null)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
        while (cursor.moveToNext()) {
            val documentId = cursor.getString(idColumn)
            val mimeType = cursor.getString(mimeColumn)
            val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
            if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
                audioFiles += scanAudioFiles(contentResolver, childrenUri, treeUri)
            } else if (mimeType.startsWith("audio/")) {
                audioFiles += documentUri
            }
        }
    }
    return audioFiles
}