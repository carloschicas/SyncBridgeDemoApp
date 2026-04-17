package com.syncbridge.demo.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val ColorOnSurface = Color(0xFF1A1B23)
private val ColorSurfaceContainerHigh = Color(0xFFE8E7F2)
private val ColorSurfaceContainerLowest = Color(0xFFFFFFFF)

enum class LogType(val icon: String, val label: String) {
    SYNCED("✅", "SYNCED"),
    CACHED("♻️", "CACHED"),
    CONFLICT("⚠️", "CONFLICT"),
    ERROR("❌", "ERROR"),
    PENDING("⏳", "PENDING")
}

data class LogEntry(
    val type: LogType,
    val txnId: String,
    val endpoint: String,
    val statusCode: Int,
    val message: String,
    val timestamp: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
)

@Composable
fun LiveLogPanel(
    entries: List<LogEntry>,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val listState = rememberLazyListState()

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) listState.animateScrollToItem(entries.lastIndex)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        LogPanelHeader(
            entryCount = entries.size,
            expanded = expanded,
            onToggle = { expanded = !expanded }
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFF0D0E18))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (entries.isEmpty()) {
                    Text(
                        text = "No hay eventos aún. Crea un pedido para ver los logs.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.35f),
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(entries) { entry ->
                            LogEntryRow(entry = entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogPanelHeader(
    entryCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1B23))
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF2D2E3A))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "SYNC LOG",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF7B9AFF),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontSize = 9.sp
                    )
                )
            }
            Text(
                text = "$entryCount eventos",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.40f),
                    fontFamily = FontFamily.Monospace
                )
            )
        }
        Text(
            text = if (expanded) "▲ COLAPSAR" else "▼ EXPANDIR",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.35f),
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp,
                fontSize = 9.sp
            )
        )
    }
}

@Composable
private fun LogEntryRow(entry: LogEntry) {
    val (iconColor, statusColor) = when (entry.type) {
        LogType.SYNCED -> Color(0xFF4CAF50) to Color(0xFF4CAF50)
        LogType.CACHED -> Color(0xFF2196F3) to Color(0xFF2196F3)
        LogType.CONFLICT -> Color(0xFFFF9800) to Color(0xFFFF9800)
        LogType.ERROR -> Color(0xFFE53935) to Color(0xFFE53935)
        LogType.PENDING -> Color(0xFF9E9E9E) to Color(0xFF9E9E9E)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = entry.timestamp,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.30f),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp
            )
        )
        Text(
            text = "${entry.type.icon} ${entry.type.label}",
            style = MaterialTheme.typography.labelSmall.copy(
                color = iconColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        )
        Text(
            text = "|",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.20f),
                fontFamily = FontFamily.Monospace
            )
        )
        Text(
            text = "txn=${entry.txnId.take(8)}",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.55f),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        )
        Text(
            text = "|",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.20f),
                fontFamily = FontFamily.Monospace
            )
        )
        Text(
            text = entry.endpoint,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.45f),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = entry.statusCode.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = statusColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        )
    }
}
