package com.sertum.player.ui.imaging

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade

/**
 * A-24 image pipeline: bounded memory LRU + generous disk cache, no crossfade
 * (avoids smear during fast scrolling). Coil3 prefetches Lazy items ahead of
 * the viewport by default when AsyncImage is used.
 */
fun buildSertumImageLoader(context: Context): ImageLoader =
    ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, 0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .maxSizeBytes(512L * 1024 * 1024)
                .build()
        }
        .crossfade(false)
        .build()
