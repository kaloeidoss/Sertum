package com.sertum.player.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.drop
import com.sertum.player.ui.screens.library.AlbumsScreen
import com.sertum.player.ui.screens.library.ArtistsScreen
import com.sertum.player.ui.screens.library.SongsScreen
import com.sertum.player.ui.screens.settings.SettingsScreen

/**
 * The four top-level tabs as one swipeable pager (user preference, 2026-08-16):
 * 歌曲 -> 专辑 -> 艺术家 -> 设置. Tab selection and the pager position are
 * kept in sync by [selectedTab]/[onTabChange].
 */
@Composable
fun MainTabs(
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = selectedTab) { 4 }

    LaunchedEffect(selectedTab) {
        if (pagerState.settledPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.drop(1).collect { onTabChange(it) }
    }

    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        when (page) {
            0 -> SongsScreen()
            1 -> AlbumsScreen(onAlbumClick = onAlbumClick)
            2 -> ArtistsScreen(onArtistClick = onArtistClick)
            else -> SettingsScreen()
        }
    }
}
