package com.tvmods.tvpatcher

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    companion object {
        private const val AUTH_PACKAGE =
            "com.AnotherAxiom.GorillaTag"

        private const val MOD_PACKAGE =
            "com.TvMods.GorillaTag"

        private const val MIN_LOBBY_TIME = 15_000L
        private const val MAX_LOBBY_TIME = 30_000L

        private const val UPDATE_INTERVAL = 100L
    }

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var statusText: TextView
    private lateinit var countdownText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var retryButton: Button

    private var launchStarted = false
    private var lobbyStartTime = 0L
    private var lobbyDuration = 0L

    private val lobbyTicker = object : Runnable {

        override fun run() {

            if (isFinishing || isDestroyed()) {
                return
            }

            val elapsed =
                System.currentTimeMillis() - lobbyStartTime

            val remaining =
                (lobbyDuration - elapsed).coerceAtLeast(0L)

            val progress =
                ((elapsed.toDouble() / lobbyDuration) * 100.0)
                    .toInt()
                    .coerceIn(0, 100)

            progressBar.progress = progress

            val seconds =
                ((remaining + 999L) / 1000L)

            countdownText.text =
                "${seconds}s"

            statusText.text =
                if (remaining > 0) {
                    "Waiting for lobby..."
                } else {
                    "Launching TvMods..."
                }

            if (elapsed >= lobbyDuration) {

                launchModdedGame()

            } else {

                handler.postDelayed(
                    this,
                    UPDATE_INTERVAL
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        countdownText = findViewById(R.id.countdownText)
        progressBar = findViewById(R.id.progressBar)
        retryButton = findViewById(R.id.retryButton)

        retryButton.setOnClickListener {
            startAuthenticationFlow()
        }

        retryButton.isEnabled = false

        startAuthenticationFlow()
    }

    private fun startAuthenticationFlow() {

        if (launchStarted) {
            return
        }

        launchStarted = true

        retryButton.isEnabled = false

        progressBar.progress = 0

        statusText.text =
            "Opening Gorilla Tag..."

        countdownText.text =
            "--"

        val authIntent =
            packageManager.getLaunchIntentForPackage(
                AUTH_PACKAGE
            )

        if (authIntent == null) {

            showFailure(
                "Normal Gorilla Tag was not found."
            )

            return
        }

        authIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
        )

        try {

            /*
             * This launches the real Gorilla Tag client.
             *
             * TvPatcher does not create or modify authentication
             * credentials. The normal client performs its own
             * authentication.
             */
            startActivity(authIntent)

            beginLobbyTimer()

        } catch (error: ActivityNotFoundException) {

            showFailure(
                "Could not open Gorilla Tag."
            )

        } catch (error: SecurityException) {

            showFailure(
                "Android blocked the Gorilla Tag launch."
            )
        }
    }

    private fun beginLobbyTimer() {

        handler.removeCallbacks(lobbyTicker)

        lobbyDuration =
            Random.nextLong(
                MIN_LOBBY_TIME,
                MAX_LOBBY_TIME + 1
            )

        lobbyStartTime =
            System.currentTimeMillis()

        statusText.text =
            "Gorilla Tag lobby active..."

        countdownText.text =
            "${lobbyDuration / 1000}s"

        progressBar.progress = 0

        handler.post(lobbyTicker)
    }

    private fun launchModdedGame() {

        handler.removeCallbacks(lobbyTicker)

        statusText.text =
            "Opening TvMods..."

        countdownText.text =
            "GO"

        progressBar.progress = 100

        val modIntent =
            packageManager.getLaunchIntentForPackage(
                MOD_PACKAGE
            )

        if (modIntent == null) {

            showFailure(
                "TvMods Gorilla Tag was not found."
            )

            return
        }

        modIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        )

        try {

            startActivity(modIntent)

            /*
             * TvPatcher has handed control over to the
             * modded client.
             */
            handler.postDelayed(
                {
                    if (!isFinishing) {
                        finish()
                    }
                },
                500L
            )

        } catch (error: ActivityNotFoundException) {

            showFailure(
                "Could not open TvMods Gorilla Tag."
            )

        } catch (error: SecurityException) {

            showFailure(
                "Android blocked the TvMods launch."
            )
        }
    }

    private fun showFailure(message: String) {

        handler.removeCallbacks(lobbyTicker)

        launchStarted = false

        statusText.text =
            message

        countdownText.text =
            "!"

        progressBar.progress = 0

        retryButton.isEnabled = true
    }

    override fun onDestroy() {

        handler.removeCallbacks(lobbyTicker)

        super.onDestroy()
    }
}
