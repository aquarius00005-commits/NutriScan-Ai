package com.example.ui.screens

import android.Manifest
import androidx.compose.ui.graphics.asImageBitmap
import com.example.R
import com.example.ui.theme.*
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.api.NutritionAnalysisResult
import com.example.ui.theme.NutriGreen
import com.example.ui.viewmodel.NutriScanViewModel
import com.example.ui.viewmodel.ScannerUiState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    viewModel: NutriScanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val uiState by viewModel.scannerUiState.collectAsState()

    // Configuration / Simulation States
    var simulationMode by remember { mutableStateOf(false) }
    var captureExecutor = remember { Executors.newSingleThreadExecutor() }

    // CameraX instance holds
    val imageCapture = remember { ImageCapture.Builder().build() }

    // Launcher to Pick Photos from System Storage Gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = loadUriAsBitmap(context, uri)
            if (bitmap != null) {
                viewModel.scanStateImg(bitmap, simulationMode)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            captureExecutor.shutdown()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val state = uiState) {
            is ScannerUiState.Idle -> {
                if (cameraPermissionState.status.isGranted) {
                    // Camera layout is permitted, display real viewfinder lens
                    CameraPreviewView(
                        imageCapture = imageCapture,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallbback instructions graphic
                    PermissionDeniedLayout(
                        onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                    )
                }

                // --- Fancy Scanning Overlay HUD Graphics ---
                ScanningHudOverlay()

                // --- Top Controller Bar ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateTo("dashboard") },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    // Simulation Mode Switch Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { simulationMode = !simulationMode }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (simulationMode) NutriGreen else Color.Gray)
                        )
                        Text(
                            text = if (simulationMode) "Demo Simulation Mode" else "Live OpenAI/Gemini Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // --- Bottom Capture Bar Controllers ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp, start = 24.dp, end = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Choose image from gallery action button
                        IconButton(
                            onClick = {
                                galleryLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Pick gallery photo", tint = Color.White)
                        }

                        // Core Capture Action Trigger button
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .border(4.dp, Color.White, CircleShape)
                                .padding(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    if (cameraPermissionState.status.isGranted) {
                                        takePhoto(
                                            context = context,
                                            imageCapture = imageCapture,
                                            executor = captureExecutor
                                        ) { bitmap ->
                                            viewModel.scanStateImg(bitmap, simulationMode)
                                        }
                                    } else {
                                        // Auto trigger simulation bitmap if camera feature unpermitted/emulator
                                        val testAsset = createSimulationBitmap(context)
                                        viewModel.scanStateImg(testAsset, isSimulation = true)
                                    }
                                }
                                .testTag("capture_photo_button")
                        )

                        // Flash or close scanner launcher
                        IconButton(
                            onClick = { viewModel.navigateTo("dashboard") },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Scanner", tint = Color.White)
                        }
                    }
                    Text(
                        text = "Center your food items in the frame and scan",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            is ScannerUiState.Scanning -> {
                ScanningSkeletonLoader()
            }

            is ScannerUiState.Success -> {
                ResultReviewScreen(
                    result = state.result,
                    bitmap = state.bitmap,
                    onBack = { viewModel.resetScanner() },
                    onConfirm = { editedResult ->
                        // Save image and log
                        val path = saveBitmapToLocalCache(context, state.bitmap)
                        viewModel.logMealToDb(editedResult, path)
                    }
                )
            }

            is ScannerUiState.LowConfidence -> {
                LowConfidenceWarningLayout(
                    result = state.result,
                    onClarify = { viewModel.forceAcceptScannerLowConfidence(state.result, state.bitmap) },
                    onCancel = { viewModel.resetScanner() }
                )
            }

            is ScannerUiState.Error -> {
                ScanFailedErrorLayout(
                    message = state.message,
                    onRetry = { viewModel.resetScanner() }
                )
            }
        }
    }
}

// --- PreviewView setup from CameraX ---
@Composable
fun CameraPreviewView(
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = androidx.camera.core.Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (exc: Exception) {
                    Log.e("ScannerCameraX", "Use case binding failed", exc)
                }

            }, ContextCompat.getMainExecutor(context))

            previewView
        },
        modifier = modifier
    )
}

// --- Animated scanning overlays HUD line ---
@Composable
fun ScanningHudOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_scan")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanner_laser_animation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()

                // Draw centered framing target coordinates
                val padX = size.width * 0.1f
                val padY = size.height * 0.22f
                val focusRectWidth = size.width - (padX * 2)
                val focusRectHeight = size.height * 0.45f

                // Draw a beautiful tech scanner overlay border path
                drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    size = size
                )

                // Clear focus center rectangle
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = androidx.compose.ui.geometry.Offset(padX, padY),
                    size = androidx.compose.ui.geometry.Size(focusRectWidth, focusRectHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                    blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                )

                // High tech neon green tracking lines
                drawRoundRect(
                    color = NutriGreen,
                    topLeft = androidx.compose.ui.geometry.Offset(padX, padY),
                    size = androidx.compose.ui.geometry.Size(focusRectWidth, focusRectHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                    style = Stroke(
                        width = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(50f, 30f), 0f)
                    )
                )

                // Floating green scanner laser indicator
                val lineY = padY + (focusRectHeight * scanProgress)
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, NutriGreen, NutriGreen, Color.Transparent)
                    ),
                    start = androidx.compose.ui.geometry.Offset(padX + 16.dp.toPx(), lineY),
                    end = androidx.compose.ui.geometry.Offset(padX + focusRectWidth - 16.dp.toPx(), lineY),
                    strokeWidth = 4.dp.toPx()
                )
            }
    )
}

// --- Loading skeleton layout during scanning processing ---
@Composable
fun ScanningSkeletonLoader() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_skeleton")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(ForestGreenDeep, CosmicDarkBg)))
            .padding(24.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // High fidelity loading ring
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(100.dp),
                color = NutriGreen,
                strokeWidth = 6.dp
            )
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Analysis",
                tint = NutriGreen,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "NutriScan AI Vision Analyzing...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Detecting food items, estimating dimensions, and calculating USDA nutrition values in real time.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Visual loading cards skeletons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = pulseAlpha * 0.1f))
                )
            }
        }
    }
}

// --- Custom detailed layout reviewed success scan ---
@Composable
fun ResultReviewScreen(
    result: NutritionAnalysisResult,
    bitmap: Bitmap,
    onBack: () -> Unit,
    onConfirm: (NutritionAnalysisResult) -> Unit
) {
    // Tweak variables so users can adjust estimated values directly before commitment
    var editableFoodName by remember { mutableStateOf(result.foodName) }
    var editableCals by remember { mutableStateOf(result.calories.toString()) }
    var editableProtein by remember { mutableStateOf(result.protein.toString()) }
    var editableCarbs by remember { mutableStateOf(result.carbs.toString()) }
    var editableFat by remember { mutableStateOf(result.fat.toString()) }
    var editableWeight by remember { mutableStateOf(result.estimatedWeight.toString()) }

    var isTweakMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // --- Photo Banner Header ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Scanned food image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Overlays visual gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                            startY = 400f
                        )
                    )
            )

            // Header close actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Close", tint = Color.White)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NutriGreen)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Confidence: ${result.confidence}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // --- Core Content Body Section ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Food Name & Portion Specs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (isTweakMode) {
                        OutlinedTextField(
                            value = editableFoodName,
                            onValueChange = { editableFoodName = it },
                            label = { Text("Edit Food Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = editableFoodName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Scale, contentDescription = "Portion", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        if (isTweakMode) {
                            OutlinedTextField(
                                value = editableWeight,
                                onValueChange = { editableWeight = it },
                                label = { Text("Grams") },
                                modifier = Modifier.width(100.dp)
                            )
                        } else {
                            Text(
                                text = "Estimated Serving Size: ${editableWeight.toDoubleOrNull()?.toInt() ?: result.estimatedWeight.toInt()}g",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { isTweakMode = !isTweakMode },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = if (isTweakMode) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = "Toggle edit fields",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Health Score & Grade Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Health Score Card (0-100)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = result.healthScore.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text("Health Score", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (result.healthScore >= 70) "Very Healthy" else if (result.healthScore >= 50) "Moderate" else "Unhealthy Selection",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (result.healthScore >= 70) NutriGreen else ActiveAmber
                            )
                        }
                    }
                }

                // Grade indicator card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    when (result.grade.uppercase()) {
                                        "A", "B" -> NutriGreen.copy(alpha = 0.15f)
                                        else -> ActiveRed.copy(alpha = 0.15f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = result.grade,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = when (result.grade.uppercase()) {
                                    "A", "B" -> NutriGreen
                                    else -> ActiveRed
                                }
                            )
                        }
                        Column {
                            Text("AI Grade", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "Grade Level",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Diet Compatibility badges horizontal list
            Text(
                text = "Diet Compatibility",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(result.dietCompatibility) { diet ->
                    SuggestionBadge(text = diet)
                }
            }

            // Nutritional Values Panel
            Text(
                text = "Detailed Nutritional Values",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Calories header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Calories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (isTweakMode) {
                            OutlinedTextField(
                                value = editableCals,
                                onValueChange = { editableCals = it },
                                modifier = Modifier.width(100.dp)
                            )
                        } else {
                            Text(
                                text = "${editableCals.toDoubleOrNull()?.toInt() ?: result.calories.toInt()} kcal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    // Core Macornutrients values grid layout
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        NutritionMiniSpec(label = "Protein", value = "${editableProtein}g", color = MaterialTheme.colorScheme.primary)
                        NutritionMiniSpec(label = "Carbs", value = "${editableCarbs}g", color = MaterialTheme.colorScheme.secondary)
                        NutritionMiniSpec(label = "Fats", value = "${editableFat}g", color = MaterialTheme.colorScheme.error)
                        NutritionMiniSpec(label = "Fiber", value = "${result.fiber}g", color = Color(0xFFC084FC))
                    }

                    if (isTweakMode) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editableProtein,
                                onValueChange = { editableProtein = it },
                                label = { Text("Protein (g)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = editableCarbs,
                                onValueChange = { editableCarbs = it },
                                label = { Text("Carbs (g)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = editableFat,
                                onValueChange = { editableFat = it },
                                label = { Text("Fat (g)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Secondary stats parameters
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sugar content:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${result.sugar} g", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sodium component:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${result.sodium.toInt()} mg", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Potassium level:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${result.potassium.toInt()} mg", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Ingredients
            Text(
                text = "Detected Ingredients",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = result.ingredients.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // AI Recommendations Swaps Advice section
            Text(
                text = "Premium AI Swaps & Suggestions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    result.recommendations.forEach { recommendation ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Swap suggestion",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = recommendation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action button to post and log meal
            Button(
                onClick = {
                    val finalResult = result.copy(
                        foodName = editableFoodName,
                        estimatedWeight = editableWeight.toDoubleOrNull() ?: result.estimatedWeight,
                        calories = editableCals.toDoubleOrNull() ?: result.calories,
                        protein = editableProtein.toDoubleOrNull() ?: result.protein,
                        carbs = editableCarbs.toDoubleOrNull() ?: result.carbs,
                        fat = editableFat.toDoubleOrNull() ?: result.fat
                    )
                    onConfirm(finalResult)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("log_scanned_meal_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.BookmarkBorder, contentDescription = "Log")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm & Log to Diary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun NutritionMiniSpec(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SuggestionBadge(text: String) {
    Box(
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun PermissionDeniedLayout(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoCamera,
            contentDescription = "No camera",
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Camera Access Request",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "To analyze meals in real time, NutriScan AI needs camera permissions. You can also pick directly from the local gallery.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Grant Camera Permission")
        }
    }
}

@Composable
fun LowConfidenceWarningLayout(
    result: NutritionAnalysisResult,
    onClarify: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(ForestGreenDeep, CosmicDarkBg)))
            .padding(24.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Warning Low Confidence",
            tint = ActiveAmber,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Low Analysis Confidence",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "NutriScan AI is only ${result.confidence}% sure of this scan. The visual context might be too blurry or obscure. Would you like to review and clarify the detected food item manually?",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onClarify,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = NutriGreen)
        ) {
            Text("Review and Adjust Manually")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Discard and Retake Scan", color = Color.White.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun ScanFailedErrorLayout(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(ForestGreenDeep, CosmicDarkBg)))
            .padding(24.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Analysis Failure",
            tint = ActiveRed,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Vision Analysis Interrupted",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Retry Scan")
        }
    }
}

// --- Helper Functions to Process Photos ---
fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: ExecutorService,
    onCaptured: (Bitmap) -> Unit
) {
    val photoFile = File(context.cacheDir, "temp_scan.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                if (bitmap != null) {
                    onCaptured(bitmap)
                }
            }

            override fun onError(exc: ImageCaptureException) {
                Log.e("ScannerCameraX", "Photo capture failed: ${exc.message}", exc)
            }
        }
    )
}

fun saveBitmapToLocalCache(context: Context, bitmap: Bitmap): String {
    val file = File(context.filesDir, "meal_scans_${System.currentTimeMillis()}.jpg")
    val out = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
    out.flush()
    out.close()
    return file.absolutePath
}

fun loadUriAsBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        null
    }
}

fun createSimulationBitmap(context: Context): Bitmap {
    // Return sample launcher icon bitmap if hardware camera is completely missing (like sandbox runner emulator)
    return BitmapFactory.decodeResource(context.resources, com.example.R.drawable.ic_launcher_fg) ?: Bitmap.createBitmap(150, 150, Bitmap.Config.ARGB_8888)
}
