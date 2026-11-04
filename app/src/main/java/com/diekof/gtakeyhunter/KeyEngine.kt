package com.diekof.gtakeyhunter
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
object KeyEngine {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val pattern = Regex("(?i)(?<![A-Z0-9])[A-Z0-9]{4}[-\\s]?[A-Z0-9]{4}[-\\s]?[A-Z0-9]{4}(?![A-Z0-9])")
    fun extract(text: String): List<String> = pattern.findAll(text.uppercase()).map { it.value.replace(Regex("[^A-Z0-9]"), "") }.distinct().toList()
    suspend fun recognize(bitmap: Bitmap): List<String> = suspendCancellableCoroutine { cont ->
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { if (cont.isActive) cont.resume(extract(it.text)) }
            .addOnFailureListener { if (cont.isActive) cont.resume(emptyList()) }
    }

    suspend fun recognizeGameKey(bitmap: Bitmap): List<String> = suspendCancellableCoroutine { cont ->
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { result ->
                if (cont.isActive) {
                    val blocks = result.textBlocks
                    val hasGame = blocks.any { block ->
                        GAME_KEYWORDS.any { keyword -> block.text.contains(keyword, ignoreCase = true) }
                    }
                    cont.resume(if (hasGame) extract(result.text) else emptyList())
                }
            }
            .addOnFailureListener { if (cont.isActive) cont.resume(emptyList()) }
    }

    private val GAME_KEYWORDS = listOf("GTA", "ZELDA")
    fun formatted(code: String) = code.chunked(4).joinToString("-")
}
