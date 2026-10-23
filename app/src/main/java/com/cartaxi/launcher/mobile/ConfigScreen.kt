package com.cartaxi.launcher.mobile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.cartaxi.launcher.data.ConfigRepository
import com.cartaxi.launcher.data.GridCell
import com.cartaxi.launcher.data.ImageStore
import com.cartaxi.launcher.data.LauncherConfig
import com.cartaxi.launcher.data.SplitMode
import com.cartaxi.launcher.data.WidgetType
import com.cartaxi.launcher.telemetry.TelemetryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private fun hasLocation(ctx: Context) =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(repo: ConfigRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf(repo.load()) }
    var sliderValue by remember { mutableFloatStateOf(config.mapPercent.toFloat()) }
    var locationGranted by remember { mutableStateOf(hasLocation(context)) }
    var pendingCell by remember { mutableIntStateOf(-1) }

    fun update(new: LauncherConfig) {
        config = new
        repo.save(new)
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        locationGranted = hasLocation(context)
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val idx = pendingCell
        if (idx < 0 || uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val paths = withContext(Dispatchers.IO) { uris.mapNotNull { ImageStore.import(context, it) } }
            val cell = config.cells.getOrNull(idx) ?: return@launch
            update(config.withCell(idx, cell.copy(images = cell.images + paths)))
        }
    }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("CarDriverLauncher") }) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Section("Quyền truy cập") {
                Text(if (locationGranted) "Vị trí: đã cấp ✔" else "Vị trí: chưa cấp (cần cho GPS/tốc độ/bản đồ)")
                if (!locationGranted) {
                    Button(onClick = {
                        permLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    }) { Text("Cấp quyền vị trí") }
                }
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }) { Text("Cấp quyền Media (Truy cập thông báo)") }
            }

            Section("Chế độ hiển thị") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = config.mode == SplitMode.HORIZONTAL,
                        onClick = { update(config.copy(mode = SplitMode.HORIZONTAL)) },
                        label = { Text("Chia Trái / Phải") }
                    )
                    FilterChip(
                        selected = config.mode == SplitMode.VERTICAL,
                        onClick = { update(config.copy(mode = SplitMode.VERTICAL)) },
                        label = { Text("Chia Trên / Dưới") }
                    )
                }
            }

            Section("Tỷ lệ: Bản đồ ${sliderValue.roundToInt()}% – Widget ${100 - sliderValue.roundToInt()}%") {
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { update(config.copy(mapPercent = sliderValue.roundToInt())) },
                    valueRange = 30f..80f
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(50, 60, 70).forEach { p ->
                        OutlinedButton(onClick = {
                            sliderValue = p.toFloat()
                            update(config.copy(mapPercent = p))
                        }) { Text("$p/${100 - p}") }
                    }
                }
            }

            Section("Lưới Widget") {
                Stepper("Hàng", config.rows) { update(config.withGrid(it, config.cols)) }
                Stepper("Cột", config.cols) { update(config.withGrid(config.rows, it)) }
                config.cells.forEachIndexed { i, cell ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Ô ${i / config.cols + 1}-${i % config.cols + 1}")
                            TypeDropdown(cell.type) { t ->
                                if (t != WidgetType.IMAGE) ImageStore.delete(cell.images)
                                update(config.withCell(i, GridCell(t, if (t == WidgetType.IMAGE) cell.images else emptyList())))
                            }
                            if (cell.type == WidgetType.IMAGE) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        pendingCell = i
                                        imagePicker.launch(arrayOf("image/*"))
                                    }) { Text("Thêm ảnh") }
                                    Text("${cell.images.size} ảnh")
                                    TextButton(onClick = {
                                        ImageStore.delete(cell.images)
                                        update(config.withCell(i, cell.copy(images = emptyList())))
                                    }) { Text("Xóa hết") }
                                }
                            }
                        }
                    }
                }
            }

            Section("Xem trước") { LayoutPreview(config) }

            OutlinedButton(onClick = { TelemetryRepository.resetTrip() }) { Text("Reset Distance / AVG") }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun Stepper(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("$label: $value")
        OutlinedButton(onClick = { if (value > 1) onChange(value - 1) }) { Text("−") }
        OutlinedButton(onClick = { if (value < LauncherConfig.MAX_GRID) onChange(value + 1) }) { Text("+") }
    }
}

@Composable
private fun TypeDropdown(current: WidgetType, onSelect: (WidgetType) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) { Text(current.label) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            WidgetType.entries.forEach { t ->
                DropdownMenuItem(text = { Text(t.label) }, onClick = { onSelect(t); open = false })
            }
        }
    }
}

@Composable
private fun LayoutPreview(config: LauncherConfig) {
    val mapW = config.mapPercent.toFloat()
    val panelW = 100f - mapW
    val map: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize().padding(2.dp).background(Color(0xFF2E7D32)), contentAlignment = Alignment.Center) {
            Text("Bản đồ", fontSize = 12.sp, color = Color.White)
        }
    }
    val panel: @Composable () -> Unit = {
        Column(Modifier.fillMaxSize()) {
            for (r in 0 until config.rows) {
                Row(Modifier.weight(1f)) {
                    for (c in 0 until config.cols) {
                        Box(
                            Modifier.weight(1f).fillMaxSize().padding(2.dp).background(Color(0xFF37474F)),
                            contentAlignment = Alignment.Center
                        ) { Text(config.cellAt(r, c).type.label, fontSize = 10.sp, color = Color.White) }
                    }
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
        if (config.mode == SplitMode.HORIZONTAL) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(mapW)) { map() }
                Box(Modifier.weight(panelW)) { panel() }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(mapW)) { map() }
                Box(Modifier.weight(panelW)) { panel() }
            }
        }
    }
}
