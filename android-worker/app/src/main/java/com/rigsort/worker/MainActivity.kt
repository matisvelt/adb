package com.rigsort.worker

import android.os.Bundle
import android.widget.TextView
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var server: WorkerServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val status = findViewById<TextView>(R.id.statusText)
        val detail = findViewById<TextView>(R.id.detailText)

        status.text = "RigSort Worker"
        try {
            startForegroundService(Intent(this, WorkerService::class.java))
            detail.text = "Worker service started (127.0.0.1:18080)"
        } catch (ex: Exception) {
            detail.text = "Failed to start service: ${ex.message}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
