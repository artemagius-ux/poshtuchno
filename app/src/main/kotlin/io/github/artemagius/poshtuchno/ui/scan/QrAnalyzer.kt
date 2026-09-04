package io.github.artemagius.poshtuchno.ui.scan

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Распознаёт QR в кадре камеры.
 *
 * Ищем только QR_CODE: фискальные чеки используют его, а ограничение формата
 * ускоряет распознавание. Первый успешный результат останавливает анализ —
 * иначе колбэк дёргался бы десятки раз в секунду на одном и том же коде.
 */
class QrAnalyzer(private val onDetected: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build(),
    )

    @Volatile
    private var done = false

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (done) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val value = barcodes.firstNotNullOfOrNull { it.rawValue }
                if (value != null && !done) {
                    done = true
                    onDetected(value)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    fun close() {
        done = true
        scanner.close()
    }
}
