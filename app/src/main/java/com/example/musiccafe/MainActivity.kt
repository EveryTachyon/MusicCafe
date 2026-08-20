package com.example.musiccafe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musiccafe.ui.theme.MusicCafeTheme

private val SidebarBackground = Color(0xFF100E13)
private val SidebarText = Color(0xFF8B898C)
private val AccentGreen = Color(0xFF00D51B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicCafeTheme {
                MusicCafeSidebar()
            }
        }
    }
}

@Composable
fun MusicCafeSidebar() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SidebarBackground
    ) {
        Column(
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight()
                .background(SidebarBackground)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Top
        ) {
            BrandHeader()
            Spacer(modifier = Modifier.height(42.dp))
            SidebarItem(icon = Icons.Outlined.Home, label = "Home")
            Spacer(modifier = Modifier.height(26.dp))
            SidebarSection(title = "Library")
            SidebarItem(label = "Playlists")
            SidebarItem(label = "Tracks")
            SidebarItem(label = "Artists")
            SidebarItem(label = "Albums")
            SidebarItem(label = "Import songs")
            Spacer(modifier = Modifier.height(54.dp))
            SidebarSection(title = "Playlists")
            SidebarItem(icon = Icons.Outlined.PlaylistAdd, label = "Create playlist")
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.MusicNote,
            contentDescription = null,
            tint = AccentGreen,
            modifier = Modifier.width(48.dp)
        )
        Text(
            text = "eSound",
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp)
        )
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
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier
            .height(64.dp)
            .padding(horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SidebarText,
                modifier = Modifier.width(40.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(0.dp))
        }
        Text(
            text = label,
            color = SidebarText,
            fontSize = 25.sp,
            modifier = Modifier.padding(start = if (icon == null) 0.dp else 16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 240, heightDp = 800)
@Composable
private fun MusicCafeSidebarPreview() {
    MusicCafeTheme(dynamicColor = false, darkTheme = true) {
        MusicCafeSidebar()
    }
}