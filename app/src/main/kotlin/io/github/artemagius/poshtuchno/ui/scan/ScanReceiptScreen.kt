package io.github.artemagius.poshtuchno.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.artemagius.poshtuchno.data.receipt.ReceiptQr
import java.util.concurrent.Executors

/**
 * Сканер QR фискального чека.
 *
 * Разрешение на камеру спрашивается здесь, а не при запуске приложения:
 * камера нужна только для этой функции, и просить её заранее незачем.
 */
@Composable
fun ScanReceiptScreen(
    onResult: (ReceiptQr.Receipt) -> Unit,
    onManualInstead: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var unrecognized by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            CameraScanner(
                onQrDetected = { raw ->
                    val receipt = ReceiptQr.parse(raw)
                    if (receipt != null) onResult(receipt) else unrecognized = true
                },
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = when {
                    !hasPermission -> "Нужен доступ к камере"
                    unrecognized -> "Это не похоже на QR чека"
                    else -> "Наведи камеру на QR-код чека"
                },
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "В QR-коде есть дата, сумма и реквизиты. " +
                    "Товары в него не записывают — их добавишь сам, приложение подскажет названия.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (!hasPermission) {
                Button(
                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Разрешить камеру")
                }
            }

            TextButton(onClick = onManualInstead, modifier = Modifier.fillMaxWidth()) {
                Text("Ввести вручную")
            }
            TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Отмена")
            }
        }
    }
}

@Composable
private fun CameraScanner(onQrDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember { QrAnalyzer(onQrDetected) }

    DisposableEffect(Unit) {
        onDispose {
            analyzer.close()
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = CameraPreview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    // 1280x720 хватает для QR и заметно дешевле полного разрешения.
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(executor, analyzer) }

                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}
