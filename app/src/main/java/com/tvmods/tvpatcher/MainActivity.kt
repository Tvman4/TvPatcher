package com.tvmods.tvpatcher

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    companion object {
        private const val SHIZUKU_REQUEST_CODE = 100
    }

    private lateinit var grantButton: Button
    private lateinit var copyObbButton: Button
    private lateinit var restoreCacheButton: Button
    private lateinit var statusText: TextView

    private var patchAccessGranted = false

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode != SHIZUKU_REQUEST_CODE) return@OnRequestPermissionResultListener

            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                patchAccessGranted = true

                runOnUiThread {
                    updateUi()
                    setStatus("✓ Patch access granted")
                }
            } else {
                patchAccessGranted = false

                runOnUiThread {
                    updateUi()
                    setStatus("Patch access was denied.")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        grantButton = findViewById(R.id.grantPatchAccess)
        copyObbButton = findViewById(R.id.copyObb)
        restoreCacheButton = findViewById(R.id.restoreCache)
        statusText = findViewById(R.id.statusText)

        Shizuku.addRequestPermissionResultListener(permissionListener)

        grantButton.setOnClickListener {
            requestPatchAccess()
        }

        copyObbButton.setOnClickListener {
            if (!checkPatchAccess()) return@setOnClickListener

            setStatus("Copying OBB...")
            copyObbButton.isEnabled = false

            Thread {
                val result = PatchOperations.copyObb()

                runOnUiThread {
                    copyObbButton.isEnabled = patchAccessGranted
                    setStatus(result)
                }
            }.start()
        }

        restoreCacheButton.setOnClickListener {
            if (!checkPatchAccess()) return@setOnClickListener

            setStatus("Restoring cache...")
            restoreCacheButton.isEnabled = false

            Thread {
                val result = PatchOperations.restoreCache()

                runOnUiThread {
                    restoreCacheButton.isEnabled = patchAccessGranted
                    setStatus(result)
                }
            }.start()
        }

        updatePermissionState()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionState()
    }

    private fun requestPatchAccess() {
        if (!Shizuku.pingBinder()) {
            setStatus(
                "Shizuku/Bytezuku is not running.\n" +
                "Start it first, then try again."
            )
            return
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            patchAccessGranted = true
            updateUi()
            setStatus("✓ Patch access already granted")
            return
        }

        if (Shizuku.shouldShowRequestPermissionRationale()) {
            setStatus(
                "Patch access was previously denied.\n" +
                "Open your Shizuku/Bytezuku manager and allow TvPatcher."
            )
            return
        }

        Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
    }

    private fun updatePermissionState() {
        patchAccessGranted =
            Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

        updateUi()

        if (!patchAccessGranted) {
            setStatus("Waiting for patch access...")
        }
    }

    private fun checkPatchAccess(): Boolean {
        if (!Shizuku.pingBinder()) {
            setStatus("Shizuku/Bytezuku is not running.")
            return false
        }

        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            patchAccessGranted = false
            updateUi()
            setStatus("Grant Patch Access first.")
            return false
        }

        patchAccessGranted = true
        return true
    }

    private fun updateUi() {
        grantButton.isEnabled = true
        copyObbButton.isEnabled = patchAccessGranted
        restoreCacheButton.isEnabled = patchAccessGranted
    }

    private fun setStatus(message: String) {
        statusText.text = message
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        super.onDestroy()
    }
}
