package com.sertum.player.ui.screens.settings

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.unit.dp
import com.sertum.player.ui.playback.OutputMode
import com.sertum.player.ui.settings.LanguageOption
import com.sertum.player.ui.settings.SettingsStateHolder

@Composable
fun SettingsScreen() {
    val state by SettingsStateHolder.state.collectAsState()
    val context = LocalContext.current
    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SectionTitle("Scanning")
        RowSwitch(
            title = "Full-disk scan (advanced)",
            subtitle = if (state.fullScanEnabled) "All files access enabled" else "Off by default; opt-in only",
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
            Text("Add music folder (SAF)")
        }

        SectionTitle("Output mode")
        listOf(
            OutputMode.STANDARD to "Auto / standard",
            OutputMode.USB_EXCLUSIVE to "USB exclusive",
        ).forEach { (mode, label) ->
            RadioRow(
                label = label,
                selected = state.outputMode == mode,
                onClick = { SettingsStateHolder.update { it.copy(outputMode = mode) } },
            )
        }

        SectionTitle("Language")
        listOf(
            LanguageOption.SYSTEM to "Follow system",
            LanguageOption.ZH to "中文",
            LanguageOption.EN to "English",
        ).forEach { (option, label) ->
            RadioRow(
                label = label,
                selected = state.language == option,
                onClick = { SettingsStateHolder.update { it.copy(language = option) } },
            )
        }

        SectionTitle("Theme")
        RowSwitch(
            title = "Dark theme",
            subtitle = if (state.darkTheme) "Near-pure black" else "Paper light",
            checked = state.darkTheme,
            onCheckedChange = { dark -> SettingsStateHolder.update { it.copy(darkTheme = dark) } },
        )

        SectionTitle("About")
        Text("Sertum 0.1.0", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "100% offline · no accounts · no telemetry",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = { /* diagnostics export lands in M5 */ },
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text("Export diagnostics")
        }
    }
}

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
