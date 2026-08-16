package com.sertum.player.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sertum.player.R
import com.sertum.player.SertumApplication
import com.sertum.player.ui.playback.OutputMode
import com.sertum.player.ui.settings.LanguageOption
import com.sertum.player.ui.settings.SettingsStateHolder
import java.util.Locale

@Composable
fun SettingsScreen() {
    val state by SettingsStateHolder.state.collectAsState()
    val context = LocalContext.current
    val app = context.applicationContext as SertumApplication
    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(app.diagnosticsStore.exportText().toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SectionTitle(stringResource(R.string.settings_scanning))
        RowSwitch(
            title = stringResource(R.string.settings_full_scan_title),
            subtitle = stringResource(
                if (state.fullScanEnabled) R.string.settings_full_scan_on else R.string.settings_full_scan_off,
            ),
            checked = state.fullScanEnabled,
            onCheckedChange = { enable ->
                val isManager = android.os.Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()
                if (enable && !isManager) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + context.packageName),
                    )
                    context.startActivity(intent)
                }
                SettingsStateHolder.update { it.copy(fullScanEnabled = enable) }
            },
        )
        OutlinedButton(onClick = { treeLauncher.launch(null) }, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.settings_add_folder))
        }
        OutlinedButton(
            onClick = { app.requestLibraryScan() },
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.settings_rescan))
        }

        SectionTitle(stringResource(R.string.settings_output_mode))
        listOf(
            OutputMode.STANDARD to R.string.settings_output_auto_standard,
            OutputMode.USB_EXCLUSIVE to R.string.settings_output_usb_exclusive,
        ).forEach { (mode, labelRes) ->
            RadioRow(
                label = stringResource(labelRes),
                selected = state.outputMode == mode,
                onClick = {
                    SettingsStateHolder.update { it.copy(outputMode = mode) }
                    app.playbackController.switchOutputMode(mode)
                },
            )
        }

        SectionTitle(stringResource(R.string.settings_language))
        listOf(
            LanguageOption.SYSTEM to R.string.settings_language_system,
            LanguageOption.ZH to R.string.settings_language_zh,
            LanguageOption.EN to R.string.settings_language_en,
        ).forEach { (option, labelRes) ->
            RadioRow(
                label = stringResource(labelRes),
                selected = state.language == option,
                onClick = {
                    SettingsStateHolder.update { it.copy(language = option) }
                    applyLocale(context, option)
                },
            )
        }

        SectionTitle(stringResource(R.string.settings_theme))
        RowSwitch(
            title = stringResource(R.string.settings_dark_theme),
            subtitle = stringResource(if (state.darkTheme) R.string.settings_dark_on else R.string.settings_dark_off),
            checked = state.darkTheme,
            onCheckedChange = { dark -> SettingsStateHolder.update { it.copy(darkTheme = dark) } },
        )

        SectionTitle(stringResource(R.string.settings_background_playback))
        Text(
            text = stringResource(R.string.settings_background_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = {
                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            },
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.settings_battery_optimization))
        }
        OutlinedButton(
            onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + context.packageName),
                    ),
                )
            },
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.settings_app_details_autostart))
        }

        SectionTitle(stringResource(R.string.settings_about))
        Text("Sertum 0.1.0", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(R.string.settings_offline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val diagnostics = app.diagnosticsStore.counts
        Text(
            text = stringResource(
                R.string.settings_diagnostics_summary,
                diagnostics.totalEntries,
                diagnostics.totalErrors,
                diagnostics.fileCount,
                7,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        OutlinedButton(
            onClick = { exportLauncher.launch("sertum-diagnostics.txt") },
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.settings_export_diagnostics))
        }
    }
}

private fun applyLocale(context: Context, option: LanguageOption) {
    val locale = when (option) {
        LanguageOption.SYSTEM -> ResourcesCompatSystemLocale
        LanguageOption.ZH -> Locale.SIMPLIFIED_CHINESE
        LanguageOption.EN -> Locale.ENGLISH
    }
    Locale.setDefault(locale)
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    @Suppress("DEPRECATION")
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
    (context as? android.app.Activity)?.recreate()
}

private val ResourcesCompatSystemLocale: Locale
    get() = android.content.res.Resources.getSystem().configuration.locales.get(0) ?: Locale.getDefault()

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun RowSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
