package com.health.nutriscan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.health.nutriscan.data.model.ScanSummary
import com.health.nutriscan.ui.viewmodel.NutriCheckViewModel
import com.health.nutriscan.ui.viewmodel.UiState

@Composable
fun DashboardScreen(viewModel: NutriCheckViewModel, onNavigateToScan: () -> Unit) {
    // Explicitly declaring the generic type fixes "Cannot infer type parameter 'T'"
    val historyState: UiState<List<ScanSummary>> by viewModel.historyState.collectAsState()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToScan,
                icon = { Icon(Icons.Default.Add, contentDescription = "Scan") },
                text = { Text("New Scan") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            Text(text = "Welcome 👋", style = MaterialTheme.typography.headlineMedium)
            Text(text = "Your Recent Product Diagnostics", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

            Spacer(modifier = Modifier.height(24.dp))

            when (val state = historyState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is UiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                is UiState.Success<*> -> {
                    // Safely cast the list to eliminate generic type mismatch errors
                    @Suppress("UNCHECKED_CAST")
                    val scanList = state.data as List<ScanSummary>

                    if (scanList.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No items scanned yet. Perform your first scan!", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(scanList) { item -> ScanSummaryCard(item) }
                        }
                    }
                }
                else -> LaunchedEffect(Unit) { viewModel.loadScanHistory() }
            }
        }
    }
}

@Composable
fun ScanSummaryCard(summary: ScanSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = summary.productName, style = MaterialTheme.typography.titleLarge)
                Text(text = summary.category.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            Box(contentAlignment = Alignment.Center) {
                val scoreColor = when {
                    summary.overallScore >= 75 -> MaterialTheme.colorScheme.primary
                    summary.overallScore >= 50 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.error
                }
                Text(
                    text = "${summary.overallScore}/100",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = scoreColor
                )
            }
        }
    }
}