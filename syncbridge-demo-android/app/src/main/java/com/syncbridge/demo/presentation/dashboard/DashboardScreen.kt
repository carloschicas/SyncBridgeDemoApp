package com.syncbridge.demo.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.syncbridge.demo.data.local.OrderEntity
import com.syncbridge.demo.presentation.OrderViewModel

private val ColorPrimary = Color(0xFF000666)
private val ColorOnSurface = Color(0xFF1A1B23)
private val ColorSurface = Color(0xFFFBF8FF)
private val ColorSurfaceContainerLow = Color(0xFFF3F2FE)
private val ColorSurfaceContainerLowest = Color(0xFFFFFFFF)
private val ColorSurfaceContainerHigh = Color(0xFFE8E7F2)
private val ColorSecondary = Color(0xFF1B6D24)
private val ColorSecondaryContainer = Color(0xFFA0F399)
private val ColorTertiaryFixedVariant = Color(0xFF773200)
private val ColorTertiaryFixed = Color(0xFFFFDBCA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: OrderViewModel,
    onNavigateToCreateOrder: () -> Unit
) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = ColorSurface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ARCHITECTURAL SUITE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp
                        )
                    )
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateOrder,
                containerColor = ColorPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo pedido")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ColorSurface),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item { HeroSection() }
            item { StatsSection(orderCount = orders.size) }
            item { QuinceSection() }
            item { OrdersHeader() }
            if (orders.isEmpty()) {
                item { EmptyState() }
            } else {
                items(orders, key = { it.id }) { order ->
                    OrderCard(order = order)
                }
            }
        }
    }
}

@Composable
private fun HeroSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(ColorPrimary, Color(0xFF1A237E)),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Column {
            Text(
                text = "Fuerza de\nVentas",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 42.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Hoy · Vendedor Principal",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.65f)
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ColorSecondaryContainer)
                    )
                    Text(
                        text = "CONECTADO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ColorSecondaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                    )
                }
                Text(
                    text = "VER DETALLES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.55f),
                        letterSpacing = 1.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun StatsSection(orderCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurfaceContainerLow)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = orderCount.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ColorOnSurface,
                            fontSize = 52.sp
                        )
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ColorPrimary.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "↑",
                            color = ColorPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Pedidos hoy en DFS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = ColorOnSurface.copy(alpha = 0.55f),
                        letterSpacing = 0.3.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun QuinceSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurface)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column {
            Text(
                text = "Rendimiento Quincenal",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = ColorOnSurface
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val barHeights = listOf(0.4f, 0.6f, 0.45f, 0.8f, 0.55f, 0.7f, 0.9f, 0.65f, 0.5f, 0.75f, 0.85f, 0.6f)
                barHeights.forEachIndexed { index, height ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height((48 * height).dp)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(
                                if (index == barHeights.lastIndex) ColorPrimary
                                else ColorSurfaceContainerHigh
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun OrdersHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Órdenes Recientes",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = ColorOnSurface
            )
        )
        TextButton(onClick = {}) {
            Text(
                text = "VER TODOS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = ColorPrimary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}

@Composable
private fun OrderCard(order: OrderEntity) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(8.dp),
                ambientColor = ColorOnSurface.copy(alpha = 0.06f),
                spotColor = ColorOnSurface.copy(alpha = 0.06f)
            )
            .clip(RoundedCornerShape(8.dp))
            .background(ColorSurfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = order.clientName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = ColorOnSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = order.productName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ColorOnSurface.copy(alpha = 0.5f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusBadge(status = order.syncStatus)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "× ${order.quantity}",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = ColorOnSurface,
                    fontSize = 26.sp,
                    letterSpacing = (-0.5).sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "unidades",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = ColorOnSurface.copy(alpha = 0.45f),
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (textColor, bgColor, label) = when (status) {
        "SYNCED" -> Triple(ColorSecondary, ColorSecondaryContainer, "SINCRONIZADO")
        "CONFLICT" -> Triple(Color(0xFF8B0000), Color(0xFFFFDAD6), "CONFLICTO")
        "SYNCING" -> Triple(ColorPrimary, ColorSurfaceContainerHigh, "SINCRONIZANDO")
        else -> Triple(ColorTertiaryFixedVariant, ColorTertiaryFixed, "PENDIENTE")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9999.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                fontSize = 9.sp
            )
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No hay pedidos.\nPulsa + para crear uno.",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = ColorOnSurface.copy(alpha = 0.35f),
                lineHeight = 24.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}
