package com.health.nutriscan.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.health.nutriscan.data.model.ProductCategory
import com.health.nutriscan.data.model.ProductType
import com.health.nutriscan.ui.viewmodel.NutriCheckViewModel
import com.health.nutriscan.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(viewModel: NutriCheckViewModel, onNavigateToResult: () -> Unit) {
    var name by remember { mutableStateOf("Unknown Product") }
    var rawText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ProductCategory.FOOD) }
    var selectedType by remember { mutableStateOf(ProductType.SOLID) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }

    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var isExtractingText by remember { mutableStateOf(false) }

    val scanState by viewModel.scanResultState.collectAsState()

    // Camera Launcher - Returns a Bitmap and processes it with ML Kit
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            capturedImage = bitmap
            isExtractingText = true

            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    rawText = visionText.text
                    isExtractingText = false
                }
                .addOnFailureListener {
                    rawText = "OCR Failed. Please type ingredients manually."
                    isExtractingText = false
                }
        }
    }

    // Auto-launch camera on first load
    LaunchedEffect(Unit) {
        cameraLauncher.launch(null)
    }

    LaunchedEffect(scanState) {
        if (scanState is UiState.Success) {
            onNavigateToResult()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Scan Ingredients", style = MaterialTheme.typography.headlineMedium)

        // Image Preview & Retake Button
        if (capturedImage != null) {
            Image(
                bitmap = capturedImage!!.asImageBitmap(),
                contentDescription = "Captured Label",
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )
        }
        Button(
            onClick = { cameraLauncher.launch(null) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Retake Photo")
        }

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth())

        ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
            OutlinedTextField(
                value = selectedCategory.name, onValueChange = {}, readOnly = true, label = { Text("Product Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                ProductCategory.values().forEach { cat ->
                    DropdownMenuItem(text = { Text(cat.name) }, onClick = { selectedCategory = cat; categoryExpanded = false })
                }
            }
        }

        OutlinedTextField(
            value = rawText,
            onValueChange = { rawText = it },
            label = { Text(if (isExtractingText) "Extracting text from image..." else "Extracted Ingredients") },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            maxLines = 6
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (scanState is UiState.Loading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Button(
                onClick = {
                    if (rawText.isNotBlank()) {
                        viewModel.analyzeIngredients(name, selectedCategory, selectedType, rawText)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Process AI Engine Scan", fontWeight = FontWeight.Bold)
            }
        }

        if (scanState is UiState.Error) {
            Text(text = (scanState as UiState.Error).message, color = MaterialTheme.colorScheme.error)
        }
    }
}