package com.health.nutriscan.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.health.nutriscan.data.model.IngredientResult
import com.health.nutriscan.ui.theme.*
import com.health.nutriscan.ui.viewmodel.NutriCheckViewModel
import com.health.nutriscan.ui.viewmodel.ScanState
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(viewModel: NutriCheckViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanState by viewModel.scanState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Photo picker launcher for selecting from Gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        viewModel.processIngredients(visionText.text)
                    }
                    .addOnFailureListener { e ->
                        viewModel.processIngredients("")
                        Log.e("MLKit", "Gallery text recognition failed", e)
                    }
            } catch (e: Exception) {
                Log.e("Gallery", "Error importing gallery image", e)
                viewModel.processIngredients("")
            }
        }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Camera,
                        contentDescription = "Camera",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "NutriScan requires camera access to scan product ingredient labels in real-time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Grant Camera Access", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        return
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (scanState) {
            is ScanState.Idle, is ScanState.Error -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraPreview(context = context, lifecycleOwner = lifecycleOwner, imageCapture = imageCapture)

                    // Custom-drawn view finder dimming overlay and scanning laser line
                    val infiniteTransition = rememberInfiniteTransition(label = "viewfinder")
                    val laserPosition by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 2500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "laserLine"
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        // Calculate centered viewfinder box dimensions
                        val padX = 36.dp.toPx()
                        val boxWidth = width - (padX * 2)
                        val boxHeight = boxWidth / 0.75f
                        val startX = padX
                        val startY = (height - boxHeight) / 2.3f // shift slightly up to offset buttons
                        val endX = startX + boxWidth
                        val endY = startY + boxHeight

                        // 1. Draw outer dimmed mask
                        drawRect(color = Color.Black.copy(alpha = 0.6f), size = Size(width, startY))
                        drawRect(
                            color = Color.Black.copy(alpha = 0.6f),
                            topLeft = Offset(0f, endY),
                            size = Size(width, height - endY)
                        )
                        drawRect(
                            color = Color.Black.copy(alpha = 0.6f),
                            topLeft = Offset(0f, startY),
                            size = Size(startX, boxHeight)
                        )
                        drawRect(
                            color = Color.Black.copy(alpha = 0.6f),
                            topLeft = Offset(endX, startY),
                            size = Size(width - endX, boxHeight)
                        )

                        // 2. Draw glowing neon corners
                        val strokeW = 4.dp.toPx()
                        val cornerLen = 28.dp.toPx()
                        val neonColor = Color(0xFF10B981) // emerald

                        // Top-Left Corner
                        drawLine(neonColor, Offset(startX, startY), Offset(startX + cornerLen, startY), strokeW)
                        drawLine(neonColor, Offset(startX, startY), Offset(startX, startY + cornerLen), strokeW)

                        // Top-Right Corner
                        drawLine(neonColor, Offset(endX, startY), Offset(endX - cornerLen, startY), strokeW)
                        drawLine(neonColor, Offset(endX, startY), Offset(endX, startY + cornerLen), strokeW)

                        // Bottom-Left Corner
                        drawLine(neonColor, Offset(startX, endY), Offset(startX + cornerLen, endY), strokeW)
                        drawLine(neonColor, Offset(startX, endY), Offset(startX, endY - cornerLen), strokeW)

                        // Bottom-Right Corner
                        drawLine(neonColor, Offset(endX, endY), Offset(endX - cornerLen, endY), strokeW)
                        drawLine(neonColor, Offset(endX, endY), Offset(endX, endY - cornerLen), strokeW)

                        // 3. Draw horizontal laser line
                        val laserY = startY + (boxHeight * laserPosition)
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF10B981), Color.Transparent)
                            ),
                            start = Offset(startX + 12.dp.toPx(), laserY),
                            end = Offset(endX - 12.dp.toPx(), laserY),
                            strokeWidth = 3.dp.toPx()
                        )
                    }

                    // Scan Header Text overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Scan Ingredient Label",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Align text inside the frame or select from gallery",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    if (scanState is ScanState.Error) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 90.dp, start = 24.dp, end = 24.dp)
                                .fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = (scanState as ScanState.Error).message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Bottom Camera Actions Panel
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gallery Button
                        IconButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoLibrary,
                                contentDescription = "Gallery",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Capture Button
                        FloatingActionButton(
                            onClick = { takePhoto(imageCapture, cameraExecutor, viewModel) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(76.dp)
                                .border(4.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCodeScanner,
                                contentDescription = "Capture",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Info Button
                        var showScanHelp by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showScanHelp = true },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HelpOutline,
                                contentDescription = "Info",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        if (showScanHelp) {
                            AlertDialog(
                                onDismissRequest = { showScanHelp = false },
                                title = { Text("How to Scan") },
                                text = {
                                    Text(
                                        "1. Center the product's ingredient list in the viewfinder box.\n" +
                                        "2. Ensure good lighting and that text is in focus.\n" +
                                        "3. Tap the scanner button to capture and automatically detect details, or use the gallery icon on the left to import a saved photo."
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = { showScanHelp = false }) {
                                        Text("Got it")
                                    }
                                }
                            )
                        }
                    }
                }
            }
            is ScanState.Loading -> {
                val loadingTips = remember {
                    listOf(
                        "Reading ingredient lists helps identify hidden chemicals and artificial additives.",
                        "High-risk items are often artificial preservatives linked to health issues.",
                        "Looking for clean label items with recognizable ingredients is a great health habit.",
                        "Did you know? Ingredients are listed in order of abundance in the product.",
                        "Safety score ranges: 0-39 is High Risk, 40-69 is Medium Risk, 70-100 is Low Risk."
                    )
                }
                var tipIndex by remember { mutableStateOf(0) }

                LaunchedEffect(Unit) {
                    while (true) {
                        delay(4000)
                        tipIndex = (tipIndex + 1) % loadingTips.size
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 0.9f,
                            targetValue = 1.1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(120.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(90.dp * pulseScale),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 5.dp
                            )
                            Icon(
                                imageVector = Icons.Rounded.Spa,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "Analyzing Ingredients",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Running local text extraction and AI analysis...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        // Glassmorphic Quote Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.TipsAndUpdates,
                                        contentDescription = "Tips",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "INGREDIENT NOTE",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = loadingTips[tipIndex],
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontStyle = FontStyle.Italic,
                                        lineHeight = 20.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            is ScanState.Success -> {
                val response = (scanState as ScanState.Success).response
                val scrollState = rememberScrollState()
                var selectedIngredient by remember { mutableStateOf<IngredientResult?>(null) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Brand header line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = response.productName ?: "Scanned Product",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.resetState() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Premium animated circular gauge score card
                    SafetyScoreGauge(score = response.safetyScore)

                    Spacer(modifier = Modifier.height(28.dp))

                    // Overall Assessment Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Grading,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Overall Assessment",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = response.overallAssessment,
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Warnings Box (if any)
                    if (response.warningsFor.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = RiskHighContainerLight.copy(alpha = 0.9f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, RiskHigh.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.ReportProblem,
                                        contentDescription = "Warning",
                                        tint = RiskHigh,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Allergy & Health Warnings",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = RiskHigh,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                response.warningsFor.forEach { warning ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "•",
                                            color = RiskHigh,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            text = warning,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF7F1D1D),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Ingredient Breakdown Section Title
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.List,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ingredients Detected (${response.results.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Ingredients cards list
                    response.results.forEach { ingredient ->
                        val riskColor = when (ingredient.risk.uppercase()) {
                            "HIGH" -> RiskHigh
                            "MEDIUM" -> RiskMedium
                            else -> RiskLow
                        }
                        val riskContainerColor = when (ingredient.risk.uppercase()) {
                            "HIGH" -> RiskHighContainerLight
                            "MEDIUM" -> RiskMediumContainerLight
                            else -> RiskLowContainerLight
                        }
                        val icon = if (ingredient.risk.uppercase() == "LOW") {
                            Icons.Rounded.CheckCircle
                        } else {
                            Icons.Rounded.Warning
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { selectedIngredient = ingredient },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = riskColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = ingredient.ingredientName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Surface(
                                        color = riskContainerColor,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = ingredient.risk.uppercase(),
                                            color = riskColor,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = ingredient.explanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    modifier = Modifier.padding(start = 32.dp)
                                )

                                val sideEffectsList = remember(ingredient.sideEffects) {
                                    ingredient.sideEffects?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                                }

                                if (sideEffectsList.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.padding(start = 32.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Report,
                                            contentDescription = null,
                                            tint = RiskHigh.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${sideEffectsList.size} side effects listed",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontStyle = FontStyle.Italic,
                                            color = RiskHigh.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Back to Camera Button
                    Button(
                        onClick = { viewModel.resetState() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.QrCodeScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan Another Product", style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }

                // Modal bottom sheet displaying detailed ingredient overview
                if (selectedIngredient != null) {
                    IngredientDetailsSheet(
                        ingredient = selectedIngredient!!,
                        onDismiss = { selectedIngredient = null }
                    )
                }
            }
        }
    }
}

@Composable
fun SafetyScoreGauge(score: Int, modifier: Modifier = Modifier) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) score / 100f else 0f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "scoreProgress"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    val riskColor = when {
        score < 40 -> RiskHigh
        score < 70 -> RiskMedium
        else -> RiskLow
    }

    val gradient = when {
        score < 40 -> HighRiskGradient
        score < 70 -> MediumRiskGradient
        else -> LowRiskGradient
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(170.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = 12.dp.toPx()
            val diameter = size.minDimension - strokeW
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val rectSize = Size(diameter, diameter)

            // Draw track background arc
            drawArc(
                color = Color.LightGray.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = rectSize,
                style = Stroke(width = strokeW)
            )

            // Draw animated progress arc
            drawArc(
                brush = gradient,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = rectSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }

        // Inside layout content
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 48.sp
                ),
                color = riskColor
            )
            Text(
                text = "Safety Score",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientDetailsSheet(
    ingredient: IngredientResult,
    onDismiss: () -> Unit
) {
    val riskColor = when (ingredient.risk.uppercase()) {
        "HIGH" -> RiskHigh
        "MEDIUM" -> RiskMedium
        else -> RiskLow
    }
    val riskContainerColor = when (ingredient.risk.uppercase()) {
        "HIGH" -> RiskHighContainerLight
        "MEDIUM" -> RiskMediumContainerLight
        else -> RiskLowContainerLight
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
            // Header Title
            Text(
                text = ingredient.ingredientName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Badges Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = riskContainerColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (ingredient.risk.uppercase() == "LOW") Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = riskColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${ingredient.risk.uppercase()} RISK",
                            color = riskColor,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = ingredient.category.uppercase(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(20.dp))

            // Description Section
            Text(
                text = "Description",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = ingredient.description.ifBlank { "No detailed description available." },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Explanation / Concerns
            Text(
                text = "Health Impact Assessment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = ingredient.explanation,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Side Effects
            val sideEffectsList = remember(ingredient.sideEffects) {
                ingredient.sideEffects?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
            }
            if (sideEffectsList.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = RiskHighContainerLight.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RiskHigh.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.MedicalServices,
                                contentDescription = null,
                                tint = RiskHigh,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Documented Side Effects",
                                style = MaterialTheme.typography.titleSmall,
                                color = RiskHigh,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        sideEffectsList.forEach { sideEffect ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "•",
                                    color = RiskHigh,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = sideEffect,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF7F1D1D)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun CameraPreview(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    imageCapture: ImageCapture
) {
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalGetImage::class)
private fun takePhoto(
    imageCapture: ImageCapture,
    executor: ExecutorService,
    viewModel: NutriCheckViewModel
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val mediaImage = image.image
                if (mediaImage != null) {
                    val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                    recognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            val extractedText = visionText.text.replace("\n", ", ").trim()
                            viewModel.processIngredients(extractedText)
                        }
                        .addOnFailureListener { e ->
                            viewModel.processIngredients("")
                            Log.e("MLKit", "Text recognition failed", e)
                        }
                        .addOnCompleteListener {
                            image.close()
                        }
                } else {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("Camera", "Photo capture failed: ${exception.message}", exception)
                viewModel.processIngredients("")
            }
        }
    )
}