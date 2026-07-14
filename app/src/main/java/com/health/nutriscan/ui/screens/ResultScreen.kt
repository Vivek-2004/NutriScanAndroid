package com.health.nutriscan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.health.nutriscan.data.model.IngredientAnalysis
import com.health.nutriscan.ui.viewmodel.NutriCheckViewModel
import com.health.nutriscan.ui.viewmodel.UiState

@Composable
fun ResultScreen(viewModel: NutriCheckViewModel, onNavigateBack: () -> Unit) {
    val scanState by viewModel.scanResultState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "AI Structural Report", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (scanState is UiState.Success) {
            val report = (scanState as UiState.Success).data

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Health Safety Metric Score", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "${report.overallScore} / 100", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Recommendations: ${report.recommendations}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                item {
                    Text(text = "Detected Compounds Breakdown", style = MaterialTheme.typography.titleLarge)
                }

                items(report.ingredientsAnalysis) { ingredient ->
                    IngredientAnalysisRow(ingredient)
                }
            }

            Button(
                onClick = { viewModel.clearScanResult(); onNavigateBack() },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Dismiss Report")
            }
        }
    }
}

@Composable
fun IngredientAnalysisRow(ingredient: IngredientAnalysis) {
    val badgeColor = when (ingredient.safetyRating.uppercase()) {
        "SAFE" -> Color(0xFFE8F5E9)
        "MODERATE" -> Color(0xFFFFF3E0)
        else -> Color(0xFFFFEBEE)
    }
    val textColor = when (ingredient.safetyRating.uppercase()) {
        "SAFE" -> Color(0xFF2E7D32)
        "MODERATE" -> Color(0xFFEF6C00)
        else -> Color(0xFFC62828)
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = ingredient.name, style = MaterialTheme.typography.titleLarge)
                Box(modifier = Modifier.background(badgeColor, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(text = ingredient.safetyRating, color = textColor, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
            if (ingredient.risks.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Risks: ${ingredient.risks}", color = Color.Red, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = ingredient.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}