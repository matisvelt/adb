package com.rigsort.worker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class WorkerServer(port: Int) : NanoHTTPD("127.0.0.1", port) {
    private val startTime = System.currentTimeMillis()
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .enableTracking()
            .build()
    )
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override fun serve(session: IHTTPSession): Response {
        return when (session.uri) {
            "/health" -> okJson(healthJson())
            "/classifyBatch" -> handleClassify(session)
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")
        }
    }

    private fun handleClassify(session: IHTTPSession): Response {
        return try {
            val files = HashMap<String, String>()
            session.parseBody(files)
            val body = files["postData"] ?: "{}"
            val json = JSONObject(body)
            val items = json.optJSONArray("items") ?: JSONArray()
            val results = JSONArray()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val fileId = item.getString("fileId")
                val base64 = item.getString("previewBase64")
                val bitmap = decodeBitmap(base64)
                val result = analyze(fileId, bitmap)
                results.put(result)
            }
            val response = JSONObject()
            response.put("results", results)
            okJson(response.toString())
        } catch (ex: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", ex.message ?: "error")
        }
    }

    private fun decodeBitmap(base64: String): Bitmap {
        val bytes = Base64.getDecoder().decode(base64)
        return BitmapFactory.decodeStream(ByteArrayInputStream(bytes))
    }

    private fun analyze(fileId: String, bitmap: Bitmap): JSONObject {
        val image = InputImage.fromBitmap(bitmap, 0)
        val faces = Tasks.await(faceDetector.process(image), 4, TimeUnit.SECONDS)
        val text = Tasks.await(textRecognizer.process(image), 4, TimeUnit.SECONDS)

        val facesCount = faces.size
        val textLength = text.text?.length ?: 0
        val hasText = min(1.0, textLength / 200.0)
        val isDoc = if (hasText > 0.5 && facesCount == 0) 0.8 else 0.2

        val aspect = if (bitmap.height == 0) 0.0 else bitmap.width.toDouble() / bitmap.height.toDouble()
        val screenshot = if (aspect > 1.5 && aspect < 2.2 && hasText > 0.3) 0.7 else 0.2

        val invoice = if (isInvoice(text.text ?: "")) 0.9 else 0.0

        var label = "UNKNOWN"
        var confidence = max(max(hasText, isDoc), screenshot)
        if (isDoc > 0.65) {
            label = if (invoice > 0.5) "DOCUMENT_INVOICE" else "DOCUMENT_OTHER"
            confidence = max(confidence, isDoc)
        } else if (screenshot > 0.7) {
            label = "SCREENSHOT"
            confidence = max(confidence, screenshot)
        } else if (facesCount > 0) {
            label = "PHOTO_PEOPLE"
            confidence = max(confidence, 0.75)
        } else {
            label = "PHOTO_NO_PEOPLE"
            confidence = max(confidence, 0.6)
        }

        val features = JSONObject()
        features.put("isDocumentLikelihood", isDoc)
        features.put("hasTextLikelihood", hasText)
        features.put("facesCount", facesCount)
        features.put("screenshotLikelihood", screenshot)
        features.put("notes", "textLen=$textLength")

        val result = JSONObject()
        result.put("fileId", fileId)
        result.put("topLabel", label)
        result.put("confidence", min(1.0, confidence))
        result.put("features", features)
        return result
    }

    private fun isInvoice(text: String): Boolean {
        val lower = text.lowercase()
        val keywords = listOf("invoice", "amount due", "total", "bill to", "due date")
        return keywords.any { lower.contains(it) }
    }

    private fun healthJson(): String {
        val uptime = (System.currentTimeMillis() - startTime) / 1000
        val obj = JSONObject()
        obj.put("ok", true)
        obj.put("version", "0.2.0")
        obj.put("modelVersion", "local-mlkit")
        obj.put("queueDepth", 0)
        obj.put("uptimeSeconds", uptime)
        obj.put("memoryMb", Runtime.getRuntime().freeMemory() / (1024 * 1024))
        return obj.toString()
    }

    private fun okJson(body: String): Response {
        val response = newFixedLengthResponse(Response.Status.OK, "application/json", body)
        response.addHeader("Content-Type", "application/json")
        return response
    }
}
