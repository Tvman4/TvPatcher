package com.tvmods.tvpatcher

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
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

    private var patchService: IPatchService? = null
    private var serviceBound = false

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->

            if (requestCode != SHIZUKU_REQUEST_CODE) return@OnRequestPermissionResultListener

            runOnUiThread {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    setStatus("✓ Patch access granted")
                    updateUi()
                    bindPatchService()
                } else {
                    setStatus("Patch access denied.")
                    updateUi()
                }
            }
        }

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(
            name: ComponentName?,
            service: IBinder?
        ) {
            patchService = IPatchService.Stub.asInterface(service)
            serviceBound = true

            runOnUiThread {
                setStatus("✓ TvPatcher service ready")
                updateUi()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            patchService = null
            serviceBound = false

            runOnUiThread {
                setStatus("Patch service disconnected.")
                updateUi()
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

        grantButton.setOnClickListener {
            requestPatchAccess()
        }

        copyObbButton.setOnClickListener {
            copyObb()
        }

        restoreCacheButton.setOnClickListener {
            restoreCache()
        }

        Shizuku.addRequestPermissionResultListener(permissionListener)

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
            updateUi()
            return
        }

        if (
            Shizuku.checkSelfPermission() ==
            PackageManager.PERMISSION_GRANTED
        ) {
            setStatus("✓ Patch access already granted")
            updateUi()
            bindPatchService()
            return
        }

        if (Shizuku.shouldShowRequestPermissionRationale()) {
            setStatus(
                "TvPatcher was denied access.\n" +
                    "Allow TvPatcher in Shizuku/Bytezuku."
            )
            updateUi()
            return
        }

        Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
    }

    private fun updatePermissionState() {

        val granted =
            Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() ==
                PackageManager.PERMISSION_GRANTED

        updateUi()

        if (granted) {
            if (!serviceBound) {
                bindPatchService()
            }

            if (!serviceBound) {
                setStatus("Patch access granted. Starting service...")
            }
        } else {
            setStatus("Waiting for Patch Access...")
        }
    }

    private fun bindPatchService() {

        if (!Shizuku.pingBinder()) {
            setStatus("Shizuku/Bytezuku is not running.")
            return
        }

        if (
            Shizuku.checkSelfPermission() !=
            PackageManager.PERMISSION_GRANTED
        ) {
            setStatus("Grant Patch Access first.")
            updateUi()
            return
        }

        if (serviceBound) return

        try {

            val args = Shizuku.UserServiceArgs(
                ComponentName(
                    this,
                    PatchService::class.java
                )
            )
                .daemon(false)
                .tag("TvPatcher")

            /*
             * Shizuku 13.1.x exposes processName as a
             * Java property rather than processName(...).
             */
            args.processName = "tvpatcher"

            Shizuku.bindUserService(
                args,
                serviceConnection
            )

            setStatus("Starting patch service...")

        } catch (e: Exception) {

            setStatus(
                "Failed to start patch service:\n" +
                    (e.message ?: e.javaClass.simpleName)
            )
        }
    }

    private fun checkService(): Boolean {

        if (!Shizuku.pingBinder()) {
            setStatus("Shizuku/Bytezuku is not running.")
            updateUi()
            return false
        }

        if (
            Shizuku.checkSelfPermission() !=
            PackageManager.PERMISSION_GRANTED
        ) {
            setStatus("Grant Patch Access first.")
            updateUi()
            return false
        }

        if (!serviceBound || patchService == null) {
            setStatus("Patch service is starting...")
            bindPatchService()
            return false
        }

        return true
    }

    private fun copyObb() {

        if (!checkService()) return

        copyObbButton.isEnabled = false
        restoreCacheButton.isEnabled = false
        setStatus("Copying OBB...")

        Thread {

            val result = try {
                patchService?.copyObb()
                    ?: "Patch service unavailable."
            } catch (e: Exception) {
                "OBB copy failed:\n${e.message ?: e.javaClass.simpleName}"
            }

            runOnUiThread {

                copyObbButton.isEnabled = serviceBound
                restoreCacheButton.isEnabled = serviceBound

                setStatus(result)
            }

        }.start()
    }

    private fun restoreCache() {

        if (!checkService()) return

        copyObbButton.isEnabled = false
        restoreCacheButton.isEnabled = false
        setStatus("Restoring cache...")

        Thread {

            val result = try {
                patchService?.restoreCache()
                    ?: "Patch service unavailable."
            } catch (e: Exception) {
                "Cache restore failed:\n${e.message ?: e.javaClass.simpleName}"
            }

            runOnUiThread {

                copyObbButton.isEnabled = serviceBound
                restoreCacheButton.isEnabled = serviceBound

                setStatus(result)
            }

        }.start()
    }

    private fun updateUi() {

        val permissionGranted =
            Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() ==
                PackageManager.PERMISSION_GRANTED

        grantButton.isEnabled = true

        copyObbButton.isEnabled =
            permissionGranted && serviceBound

        restoreCacheButton.isEnabled =
            permissionGranted && serviceBound
    }

    private fun setStatus(message: String) {
        statusText.text = message
    }

    override fun onDestroy() {

        Shizuku.removeRequestPermissionResultListener(
            permissionListener
        )

        if (serviceBound) {
            try {
                val args = Shizuku.UserServiceArgs(
                    ComponentName(
                        this,
                        PatchService::class.java
                    )
                )
                    .daemon(false)
                    .tag("TvPatcher")

                args.processName = "tvpatcher"

                Shizuku.unbindUserService(
                    args,
                    serviceConnection,
                    true
                )
            } catch (_: Exception) {
            }
        }

        serviceBound = false
        patchService = null

        super.onDestroy()
    }
}
