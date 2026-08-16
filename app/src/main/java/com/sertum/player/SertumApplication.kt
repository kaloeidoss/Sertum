package com.sertum.player

import android.app.Application
import androidx.room.Room
import coil3.SingletonImageLoader
import com.sertum.player.data.covers.CoverStore
import com.sertum.player.data.db.SertumDatabase
import com.sertum.player.ui.imaging.buildSertumImageLoader

class SertumApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SingletonImageLoader.setSafe { context -> buildSertumImageLoader(context) }
    }

    val database: SertumDatabase by lazy {
        Room.databaseBuilder(this, SertumDatabase::class.java, "sertum.db").build()
    }

    val coverStore: CoverStore by lazy { CoverStore(filesDir) }
}
