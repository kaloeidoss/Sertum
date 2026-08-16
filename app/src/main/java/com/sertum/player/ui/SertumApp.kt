package com.sertum.player.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sertum.player.R
import com.sertum.player.SertumApplication
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
import com.sertum.player.ui.theme.SertumTheme

private data class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val icon: @Composable () -> Unit,
)

private val topLevel = listOf(
    TopLevelDestination(SertumDestinations.SONGS, R.string.nav_songs, { Icon(Icons.Filled.LibraryMusic, contentDescription = stringResource(R.string.nav_songs)) }),
    TopLevelDestination(SertumDestinations.ALBUMS, R.string.nav_albums, { Icon(Icons.Filled.Album, contentDescription = stringResource(R.string.nav_albums)) }),
    TopLevelDestination(SertumDestinations.ARTISTS, R.string.nav_artists, { Icon(Icons.Filled.Person, contentDescription = stringResource(R.string.nav_artists)) }),
    TopLevelDestination(SertumDestinations.SETTINGS, R.string.nav_settings, { Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.nav_settings)) }),
)

@Composable
fun SertumApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val settings by com.sertum.player.ui.settings.SettingsStateHolder.state.collectAsState()
    val context = LocalContext.current
    val controller = (context.applicationContext as SertumApplication).playbackController
    val snackbarHostState = remember { SnackbarHostState() }
    val userMessage by controller.userMessage.collectAsState()

    LaunchedEffect(userMessage) {
        val message = userMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            controller.consumeUserMessage()
        }
    }

    SertumTheme(darkTheme = settings.darkTheme) {
        Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentRoute in topLevel.map { it.route }) {
                androidx.compose.foundation.layout.Column {
                    MiniPlayer(onExpand = { navController.navigate(SertumDestinations.NOW_PLAYING) })
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
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
                                label = { Text(stringResource(dest.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = com.sertum.player.ui.theme.WarmGold,
                                    selectedTextColor = com.sertum.player.ui.theme.WarmGold,
                                    indicatorColor = com.sertum.player.ui.theme.WarmGoldDim,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
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
}
