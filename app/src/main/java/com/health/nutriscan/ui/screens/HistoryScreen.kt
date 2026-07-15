package com.health.nutriscan.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.health.nutriscan.data.model.HistoryIngredientResult
import com.health.nutriscan.data.model.HistoryItem
import com.health.nutriscan.ui.theme.*
import com.health.nutriscan.ui.viewmodel.HistoryState
import com.health.nutriscan.ui.viewmodel.NutriCheckViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: NutriCheckViewModel) {
    val historyState by viewModel.historyState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedHistoryItem by remember { mutableStateOf<HistoryItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchHistory()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search products...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Filter Pills
        val filters = listOf(
            "ALL" to "All",
            "HIGH" to "High Risk",
            "MEDIUM" to "Med Risk",
            "LOW" to "Low Risk"
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters) { (key, label) ->
                val isSelected = selectedFilter == key
                val containerColor = if (isSelected) {
                    when (key) {
                        "HIGH" -> RiskHighContainerLight
                        "MEDIUM" -> RiskMediumContainerLight
                        "LOW" -> RiskLowContainerLight
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    }
                } else {
                    MaterialTheme.colorScheme.surface
                }
                val contentColor = if (isSelected) {
                    when (key) {
                        "HIGH" -> RiskHigh
                        "MEDIUM" -> RiskMedium
                        "LOW" -> RiskLow
                        else -> MaterialTheme.colorScheme.primary
                    }
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                val border = if (isSelected) {
                    BorderStroke(1.dp, contentColor.copy(alpha = 0.4f))
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }

                Surface(
                    onClick = { selectedFilter = key },
                    color = containerColor,
                    shape = RoundedCornerShape(12.dp),
                    border = border,
                    modifier = Modifier.height(38.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main List Content
        when (historyState) {
            is HistoryState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is HistoryState.Error -> {
                val errorMessage = (historyState as HistoryState.Error).message
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ReportProblem,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Unable to Load History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.fetchHistory() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Retry Connection", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
            is HistoryState.Success -> {
                val history = (historyState as HistoryState.Success).history

                // Filtering logic
                val filteredHistory = history.filter { item ->
                    val matchesSearch = item.productName?.contains(searchQuery, ignoreCase = true) == true
                    val matchesRisk = when (selectedFilter) {
                        "HIGH" -> item.summary.overallRisk.uppercase() == "HIGH"
                        "MEDIUM" -> item.summary.overallRisk.uppercase() == "MEDIUM"
                        "LOW" -> item.summary.overallRisk.uppercase() == "LOW"
                        else -> true
                    }
                    matchesSearch && matchesRisk
                }

                if (filteredHistory.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Inbox,
                                contentDescription = "Empty",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No results match your search" else "No scan history yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Try typing something else." else "Your scanned products will appear here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(filteredHistory, key = { it.scanId }) { item ->
                            HistoryCard(item = item, onClick = { selectedHistoryItem = item })
                        }
                    }
                }
            }
        }
    }

    // Modal details dialog when clicking a history card
    if (selectedHistoryItem != null) {
        HistoryDetailsSheet(
            item = selectedHistoryItem!!,
            onDismiss = { selectedHistoryItem = null }
        )
    }
}

@Composable
fun HistoryCard(item: HistoryItem, onClick: () -> Unit) {
    val riskColor = when (item.summary.overallRisk.uppercase()) {
        "HIGH" -> RiskHigh
        "MEDIUM" -> RiskMedium
        else -> RiskLow
    }
    val riskBgColor = when (item.summary.overallRisk.uppercase()) {
        "HIGH" -> RiskHighContainerLight
        "MEDIUM" -> RiskMediumContainerLight
        else -> RiskLowContainerLight
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.productName ?: "Unknown Product",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    color = riskBgColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = item.summary.overallRisk,
                        color = riskColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatDateTime(item.scannedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                CompactSummaryBadge(label = "High", count = item.summary.highRiskCount, color = RiskHigh)
                CompactSummaryBadge(label = "Med", count = item.summary.mediumRiskCount, color = RiskMedium)
                CompactSummaryBadge(label = "Low", count = item.summary.lowRiskCount, color = RiskLow)
            }
        }
    }
}

@Composable
fun CompactSummaryBadge(label: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$count $label",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            softWrap = false
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailsSheet(
    item: HistoryItem,
    onDismiss: () -> Unit
) {
    val riskColor = when (item.summary.overallRisk.uppercase()) {
        "HIGH" -> RiskHigh
        "MEDIUM" -> RiskMedium
        else -> RiskLow
    }
    val riskBgColor = when (item.summary.overallRisk.uppercase()) {
        "HIGH" -> RiskHighContainerLight
        "MEDIUM" -> RiskMediumContainerLight
        else -> RiskLowContainerLight
    }

    // Estimate safety score based on risk ratios
    val computedScore = remember(item) {
        if (item.summary.totalIngredients > 0) {
            val base = (item.summary.lowRiskCount * 100f + item.summary.mediumRiskCount * 60f + item.summary.highRiskCount * 10f) / item.summary.totalIngredients
            base.coerceIn(0f, 100f).toInt()
        } else {
            100
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp)
                ) {
                    Text(
                        text = item.productName ?: "Scanned Product",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scanned on ${formatDateTime(item.scannedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = riskBgColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${item.summary.overallRisk} RISK",
                        color = riskColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Safety Gauge Visual
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                SafetyScoreGauge(score = computedScore)
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Ingredient Summary Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Badges breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SummaryStatCard(title = "High Risk", count = item.summary.highRiskCount, color = RiskHigh)
                }
                Box(modifier = Modifier.weight(1f)) {
                    SummaryStatCard(title = "Medium", count = item.summary.mediumRiskCount, color = RiskMedium)
                }
                Box(modifier = Modifier.weight(1f)) {
                    SummaryStatCard(title = "Low Risk", count = item.summary.lowRiskCount, color = RiskLow)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Ingredient Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            item.results.forEach { ingredient ->
                val ingRiskColor = when (ingredient.risk.uppercase()) {
                    "HIGH" -> RiskHigh
                    "MEDIUM" -> RiskMedium
                    else -> RiskLow
                }
                val ingRiskBg = when (ingredient.risk.uppercase()) {
                    "HIGH" -> RiskHighContainerLight
                    "MEDIUM" -> RiskMediumContainerLight
                    else -> RiskLowContainerLight
                }
                val icon = if (ingredient.risk.uppercase() == "LOW") Icons.Rounded.CheckCircle else Icons.Rounded.Warning

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(icon, contentDescription = null, tint = ingRiskColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(ingredient.ingredientName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Surface(
                                color = ingRiskBg,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = ingredient.risk.uppercase(),
                                    color = ingRiskColor,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = ingredient.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Render side effects if present in history
                        val sideEffectsList = remember(ingredient.sideEffects) {
                            ingredient.sideEffects?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                        }
                        if (sideEffectsList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Side Effects: ${sideEffectsList.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = RiskHigh.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun SummaryStatCard(title: String, count: Int, color: Color) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.06f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatDateTime(isoString: String): String {
    return try {
        // Safe string parsing for "YYYY-MM-DDTHH:MM:SS..."
        val datePart = isoString.substringBefore("T")
        val timePart = isoString.substringAfter("T").substringBeforeLast(".").take(5)
        "$datePart at $timePart"
    } catch (e: Exception) {
        isoString
    }
}