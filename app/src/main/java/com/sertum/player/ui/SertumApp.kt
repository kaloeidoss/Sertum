package com.sertum.player.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sertum.player.ui.navigation.SertumDestinations
import com.sertum.player.ui.screens.library.AlbumDetailScreen
import com.sertum.player.ui.screens.library.AlbumsScreen
import com.sertum.player.ui.screens.library.ArtistDetailScreen
import com.sertum.player.ui.screens.library.ArtistsScreen
import com.sertum.player.ui.screens.library.SongsScreen
import com.sertum.player.ui.screens.nowplaying.MiniPlayer
import com.sertum.player.ui.screens.nowplaying.NowPlayingScreen
import com.sertum.player.ui.screens.nowplaying.QueueScreen
import com.sertum.player.ui.screens.settings.SettingsScreen

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
)

private val topLevel = listOf(
    TopLevelDestination(SertumDestinations.SONGS, "Songs", { Icon(Icons.Filled.LibraryMusic, contentDescription = "Songs") }),
    TopLevelDestination(SertumDestinations.ALBUMS, "Albums", { Icon(Icons.Filled.Album, contentDescription = "Albums") }),
    TopLevelDestination(SertumDestinations.ARTISTS, "Artists", { Icon(Icons.Filled.Person, contentDescription = "Artists") }),
    TopLevelDestination(SertumDestinations.SETTINGS, "Settings", { Icon(Icons.Filled.Settings, contentDescription = "Settings") }),
)

@Composable
fun SertumApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in topLevel.map { it.route }) {
                androidx.compose.foundation.layout.Column {
                    MiniPlayer(onExpand = { navController.navigate(SertumDestinations.NOW_PLAYING) })
                    NavigationBar {
                        topLevel.forEach { dest ->
                            NavigationBarItem(
                                selected = currentRoute == dest.route,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = dest.icon,
                                label = { Text(dest.label) },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = SertumDestinations.SONGS,
            modifier = Modifier.padding(padding),
        ) {
            composable(SertumDestinations.SONGS) { SongsScreen() }
            composable(SertumDestinations.ALBUMS) {
                AlbumsScreen(onAlbumClick = { key -> navController.navigate(SertumDestinations.albumDetail(key)) })
            }
            composable(SertumDestinations.ARTISTS) {
                ArtistsScreen(onArtistClick = { name -> navController.navigate(SertumDestinations.artistDetail(name)) })
            }
            composable(SertumDestinations.SETTINGS) { SettingsScreen() }
            composable(
                SertumDestinations.ALBUM_DETAIL,
                arguments = listOf(navArgument("albumKey") { type = NavType.StringType }),
            ) { entry ->
                AlbumDetailScreen(albumKey = entry.arguments?.getString("albumKey").orEmpty())
            }
            composable(
                SertumDestinations.ARTIST_DETAIL,
                arguments = listOf(navArgument("artistName") { type = NavType.StringType }),
            ) { entry ->
                ArtistDetailScreen(
                    artistName = entry.arguments?.getString("artistName").orEmpty(),
                    onAlbumClick = { key -> navController.navigate(SertumDestinations.albumDetail(key)) },
                )
            }
            composable(SertumDestinations.NOW_PLAYING) {
                NowPlayingScreen(onOpenQueue = { navController.navigate(SertumDestinations.QUEUE) })
            }
            composable(SertumDestinations.QUEUE) { QueueScreen() }
        }
    }
}
