package com.syncbridge.demo.presentation.create_order

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syncbridge.demo.presentation.OrderViewModel

private val ColorPrimary = Color(0xFF000666)
private val ColorOnSurface = Color(0xFF1A1B23)
private val ColorSurface = Color(0xFFFBF8FF)
private val ColorSurfaceContainerLow = Color(0xFFF3F2FE)
private val ColorSurfaceContainerHighest = Color(0xFFE3E1F5)
private val ColorSecondary = Color(0xFF1B6D24)
private val ColorSecondaryContainer = Color(0xFFA0F399)

private val ClientOptions = listOf(
    "Estudio Mendoza & Assoc.",
    "Constructora Noroeste",
    "GHL Proyectos",
    "Urbanizadora del Sur"
)

private val ProductOptions = listOf(
    "Architectural Suite Pro",
    "Structural Analysis Module",
    "BIM Integration Pack",
    "Site Survey Bundle"
)

private val ProductPrices = mapOf(
    "Architectural Suite Pro" to 856.0,
    "Structural Analysis Module" to 1200.0,
    "BIM Integration Pack" to 950.0,
    "Site Survey Bundle" to 750.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    viewModel: OrderViewModel,
    onNavigateBack: () -> Unit
) {
    var selectedClient by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var clientExpanded by remember { mutableStateOf(false) }
    var productExpanded by remember { mutableStateOf(false) }

    val qty = quantity.toIntOrNull() ?: 0
    val unitPrice = ProductPrices[selectedProduct] ?: 856.0
    val estimatedTotal = qty * unitPrice

    Scaffold(
        containerColor = ColorSurface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "New Order",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorPrimary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .background(ColorSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "Architectural\nSuite",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ColorPrimary,
                        lineHeight = 38.sp
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Draft a new acquisition agreement. Ensure all technical specifications align with the client directory profile.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ColorOnSurface.copy(alpha = 0.60f),
                        lineHeight = 22.sp
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorSurfaceContainerLow)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("CLIENT DIRECTORY")
                    ExposedDropdownMenuBox(
                        expanded = clientExpanded,
                        onExpandedChange = { clientExpanded = it }
                    ) {
                        TextField(
                            value = selectedClient,
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Select an established client") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = ColorPrimary.copy(alpha = 0.55f)
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientExpanded)
                            },
                            isError = showError && selectedClient.isBlank(),
                            colors = editorialTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = clientExpanded,
                            onDismissRequest = { clientExpanded = false }
                        ) {
                            ClientOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedClient = option
                                        clientExpanded = false
                                        showError = false
                                    }
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("PRODUCT CATALOG")
                    ExposedDropdownMenuBox(
                        expanded = productExpanded,
                        onExpandedChange = { productExpanded = it }
                    ) {
                        TextField(
                            value = selectedProduct,
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Select from architectural catalog") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Category,
                                    contentDescription = null,
                                    tint = ColorPrimary.copy(alpha = 0.55f)
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded)
                            },
                            isError = showError && selectedProduct.isBlank(),
                            colors = editorialTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = productExpanded,
                            onDismissRequest = { productExpanded = false }
                        ) {
                            ProductOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedProduct = option
                                        productExpanded = false
                                        showError = false
                                    }
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("ORDER MAGNITUDE")
                    TextField(
                        value = quantity,
                        onValueChange = { quantity = it.filter { c -> c.isDigit() }; showError = false },
                        placeholder = { Text("Enter unit quantity") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.GridView,
                                contentDescription = null,
                                tint = ColorPrimary.copy(alpha = 0.55f)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = showError && quantity.toIntOrNull() == null,
                        colors = editorialTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Inventory check performed on sync.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ColorOnSurface.copy(alpha = 0.40f),
                            letterSpacing = 0.3.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ColorSurfaceContainerLow)
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "ESTIMATED TOTAL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ColorOnSurface.copy(alpha = 0.45f),
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$${"%,.2f".format(estimatedTotal)}",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = ColorOnSurface,
                        letterSpacing = (-0.5).sp,
                        fontSize = 40.sp
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "PRIORITY RANK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ColorOnSurface.copy(alpha = 0.40f),
                            letterSpacing = 1.sp
                        )
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(ColorSecondaryContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "★ Tier 1 Client",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ColorSecondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ColorPrimary, Color(0xFF1A237E)),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                    .clickable {
                        val qty2 = quantity.toIntOrNull()
                        if (selectedClient.isBlank() || selectedProduct.isBlank() || qty2 == null || qty2 <= 0) {
                            showError = true
                        } else {
                            viewModel.insertOrder(selectedClient, selectedProduct, qty2)
                            onNavigateBack()
                        }
                    }
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "WILL SYNC WHEN\nCONNECTION IS RESTORED",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                        lineHeight = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            color = ColorOnSurface.copy(alpha = 0.45f),
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun editorialTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = ColorSurfaceContainerHighest,
    unfocusedContainerColor = ColorSurfaceContainerHighest,
    disabledContainerColor = ColorSurfaceContainerHighest,
    focusedIndicatorColor = ColorPrimary,
    unfocusedIndicatorColor = ColorOnSurface.copy(alpha = 0.15f),
    focusedTextColor = ColorOnSurface,
    unfocusedTextColor = ColorOnSurface,
    focusedPlaceholderColor = ColorOnSurface.copy(alpha = 0.35f),
    unfocusedPlaceholderColor = ColorOnSurface.copy(alpha = 0.35f)
)
