package com.sertum.player

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.room.Room
import coil3.SingletonImageLoader
import com.sertum.player.audio.PlaybackCoordinator
import com.sertum.player.audio.PlayerEngine
import com.sertum.player.audio.UsbHotplugController
import com.sertum.player.data.covers.CoverStore
import com.sertum.player.data.db.SertumDatabase
import com.sertum.player.data.diagnostics.DiagnosticLevel
import com.sertum.player.data.diagnostics.DiagnosticsStore
import com.sertum.player.data.scan.LibraryScanner
import com.sertum.player.data.scan.ScanStats
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
        if (hasMediaPermission()) {
            requestLibraryScan()
        }
    }

    private fun hasMediaPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    /** Bootstrap scan; safe to call repeatedly (full-rescan semantics). */
    fun requestLibraryScan(onDone: (Result<ScanStats>) -> Unit = {}) {
        libraryScanner.scanNow { result ->
            result.onSuccess {
                diagnosticsStore.log(
                    DiagnosticLevel.INFO,
                    "scan",
                    "mediaStore candidates=${it.candidates} parsed=${it.parsed} " +
                        "failed=${it.failed} albums=${it.albums} artists=${it.artists}",
                )
            }.onFailure {
                diagnosticsStore.log(DiagnosticLevel.ERROR, "scan", "failed: ${it.message}")
            }
            onDone(result)
        }
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

    val libraryScanner: LibraryScanner by lazy {
        LibraryScanner(this, database.libraryDao())
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
