package com.sertum.player

import android.app.Application
import androidx.room.Room
import coil3.SingletonImageLoader
import com.sertum.player.audio.PlaybackCoordinator
import com.sertum.player.audio.PlayerEngine
import com.sertum.player.audio.UsbHotplugController
import com.sertum.player.data.covers.CoverStore
import com.sertum.player.data.db.SertumDatabase
import com.sertum.player.data.diagnostics.DiagnosticsStore
import com.sertum.player.domain.playback.RoomResumePositionStore
import com.sertum.player.ui.imaging.buildSertumImageLoader
import java.io.File

class SertumApplication : Application() {

    private var hotplugController: UsbHotplugController? = null

    override fun onCreate() {
        super.onCreate()
        SingletonImageLoader.setSafe { context -> buildSertumImageLoader(context) }
        hotplugController = UsbHotplugController(
            this,
            onUsbAttached = { playbackController.onUsbDeviceAttached() },
            onUsbDetached = { playbackController.onUsbDeviceDetached() },
            onBluetoothChanged = { playbackController.setBluetoothConnected(it) },
        )
    }

    val database: SertumDatabase by lazy {
        Room.databaseBuilder(this, SertumDatabase::class.java, "sertum.db")
            .addMigrations(SertumDatabase.MIGRATION_1_2)
            .build()
    }

    val coverStore: CoverStore by lazy { CoverStore(filesDir) }

    val diagnosticsStore: DiagnosticsStore by lazy {
        DiagnosticsStore(File(filesDir, "diagnostics"))
    }

    private val resumeStore: RoomResumePositionStore by lazy {
        RoomResumePositionStore(database.libraryDao())
    }

    val playerEngine: PlayerEngine by lazy { PlayerEngine(this) }

    val playbackController: PlaybackCoordinator by lazy {
        PlaybackCoordinator(
            context = this,
            engine = playerEngine,
            resumeStore = resumeStore,
            diagnostics = diagnosticsStore,
            markTrackUnplayable = database.libraryDao()::markTrackUnplayable,
        )
    }
}
