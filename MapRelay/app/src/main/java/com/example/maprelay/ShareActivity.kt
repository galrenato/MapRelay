package com.example.maprelay

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class ShareActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        forwardShare(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        forwardShare(intent)
    }

    private fun forwardShare(incoming: Intent?) {
        if (incoming?.action != Intent.ACTION_SEND) {
            finish()
            return
        }

        val text = incoming.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            ?: incoming.data?.toString()

        if (text.isNullOrBlank()) {
            Toast.makeText(this, "No destination text received", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val subject = incoming.getStringExtra(Intent.EXTRA_SUBJECT)
        val htmlText = incoming.getStringExtra(Intent.EXTRA_HTML_TEXT)

        // First send the destination to Tesla.
        val teslaSent = launchShareTarget(
            TESLA_PACKAGE, text, subject, htmlText
        )

        // Then open Waze after a short delay.
        Handler(Looper.getMainLooper()).postDelayed({
            val wazeSent = launchShareTarget(
                WAZE_PACKAGE, text, subject, htmlText
            )

            if (!wazeSent) {
                Toast.makeText(
                    this,
                    "Sent to Tesla, but Waze could not be opened",
                    Toast.LENGTH_LONG
                ).show()
            } else if (!teslaSent) {
                Toast.makeText(
                    this,
                    "Waze opened; Tesla could not receive the destination",
                    Toast.LENGTH_LONG
                ).show()
            }

            finish()
        }, WAZE_LAUNCH_DELAY_MS)
    }

    private fun launchShareTarget(
        packageName: String,
        text: String,
        subject: String?,
        htmlText: String?
    ): Boolean {
        val outgoing = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage(packageName)
            putExtra(Intent.EXTRA_TEXT, text)

            if (!subject.isNullOrBlank()) {
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }

            if (!htmlText.isNullOrBlank()) {
                putExtra(Intent.EXTRA_HTML_TEXT, htmlText)
            }
        }

        return try {
            startActivity(outgoing)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    companion object {
        private const val TESLA_PACKAGE = "com.teslamotors.tesla"
        private const val WAZE_PACKAGE = "com.waze"
        private const val WAZE_LAUNCH_DELAY_MS = 1_500L
    }
}
