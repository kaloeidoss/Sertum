package com.sertum.player

import android.app.Application
import androidx.room.Room
import com.sertum.player.data.db.SertumDatabase

class SertumApplication : Application() {
    val database: SertumDatabase by lazy {
        Room.databaseBuilder(this, SertumDatabase::class.java, "sertum.db").build()
    }
}
