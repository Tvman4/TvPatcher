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

    private var patchAccessGranted = false
    private var serviceBound = false
    private var patchService: IPatchService? = null

    private fun createServiceArgs(): Shizuku.UserServiceArgs {

        return Shizuku.UserServiceArgs(
            ComponentName(
                this,
                PatchService::class.java
            )
        )
            .daemon(false)
            .tag("TvPatcher")
            .version(1)
            .processNameSuffix("patch")
    }

    private val serviceConnection =
        object : ServiceConnection {

            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?
            ) {

                patchService =
                    IPatchService.Stub.asInterface(
                        service
                    )

                serviceBound =
                    patchService != null

                runOnUiThread {

                    updateUi()

                    if (serviceBound) {
                        setStatus(
                            "✓ Patch service ready"
                        )
                    } else {
                        setStatus(
                            "Patch service failed."
                        )
                    }
                }
            }

            override fun onServiceDisconnected(
                name: ComponentName?
            ) {

                patchService = null
                serviceBound = false

                runOnUiThread {

                    updateUi()

                    setStatus(
                        "Patch service disconnected."
                    )
                }
            }
        }

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener {
                requestCode,
                grantResult ->

            if (
                requestCode !=
                SHIZUKU_REQUEST_CODE
            ) {
                return@OnRequestPermissionResultListener
            }

            runOnUiThread {

                if (
                    grantResult ==
                    PackageManager.PERMISSION_GRANTED
                ) {

                    patchAccessGranted = true

                    updateUi()

                    setStatus(
                        "✓ Patch access granted"
                    )

                    bindPatchService()

                } else {

                    patchAccessGranted = false

                    updateUi()

                    setStatus(
                        "Patch access denied."
                    )
                }
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_main
        )

        grantButton =
            findViewById(
                R.id.grantPatchAccess
            )

        copyObbButton =
            findViewById(
                R.id.copyObb
            )

        restoreCacheButton =
            findViewById(
                R.id.restoreCache
            )

        statusText =
            findViewById(
                R.id.statusText
            )

        grantButton.setOnClickListener {
            requestPatchAccess()
        }

        copyObbButton.setOnClickListener {
            runCopyObb()
        }

        restoreCacheButton.setOnClickListener {
            runRestoreCache()
        }

        Shizuku.addRequestPermissionResultListener(
            permissionListener
        )

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
                    "Start it first."
            )

            return
        }

        if (
            Shizuku.checkSelfPermission() ==
            PackageManager.PERMISSION_GRANTED
        ) {

            patchAccessGranted = true

            updateUi()

            setStatus(
                "✓ Patch access already granted"
            )

            bindPatchService()

            return
        }

        if (
            Shizuku.shouldShowRequestPermissionRationale()
        ) {

            setStatus(
                "Allow TvPatcher in Shizuku/Bytezuku."
            )

            return
        }

        Shizuku.requestPermission(
            SHIZUKU_REQUEST_CODE
        )
    }

    private fun updatePermissionState() {

        patchAccessGranted =
            Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() ==
                PackageManager.PERMISSION_GRANTED

        updateUi()

        if (patchAccessGranted) {

            bindPatchService()

        } else {

            setStatus(
                "Waiting for Patch Access..."
            )
        }
    }

    private fun bindPatchService() {

        if (!patchAccessGranted) {
            return
        }

        if (serviceBound) {
            return
        }

        try {

            Shizuku.bindUserService(
                createServiceArgs(),
                serviceConnection
            )

            setStatus(
                "Starting patch service..."
            )

        } catch (e: Throwable) {

            setStatus(
                "Service start failed:\n" +
                    (
                        e.message
                            ?: e.javaClass.simpleName
                    )
            )
        }
    }

    private fun runCopyObb() {

        if (!checkReady()) {
            return
        }

        val service =
            patchService
                ?: return

        copyObbButton.isEnabled = false
        restoreCacheButton.isEnabled = false

        setStatus(
            "Copying OBB..."
        )

        Thread {

            val result = try {

                service.copyObb()

            } catch (e: Throwable) {

                "OBB copy failed:\n" +
                    (
                        e.message
                            ?: e.javaClass.simpleName
                    )
            }

            runOnUiThread {

                updateUi()

                setStatus(result)
            }

        }.start()
    }

    private fun runRestoreCache() {

        if (!checkReady()) {
            return
        }

        val service =
            patchService
                ?: return

        copyObbButton.isEnabled = false
        restoreCacheButton.isEnabled = false

        setStatus(
            "Restoring cache..."
        )

        Thread {

            val result = try {

                service.restoreCache()

            } catch (e: Throwable) {

                "Cache restore failed:\n" +
                    (
                        e.message
                            ?: e.javaClass.simpleName
                    )
            }

            runOnUiThread {

                updateUi()

                setStatus(result)
            }

        }.start()
    }

    private fun checkReady(): Boolean {

        if (!Shizuku.pingBinder()) {

            setStatus(
                "Shizuku/Bytezuku is not running."
            )

            return false
        }

        if (!patchAccessGranted) {

            setStatus(
                "Grant Patch Access first."
            )

            return false
        }

        if (!serviceBound || patchService == null) {

            setStatus(
                "Patch service is starting..."
            )

            bindPatchService()

            return false
        }

        return true
    }

    private fun updateUi() {

        grantButton.isEnabled = true

        copyObbButton.isEnabled =
            patchAccessGranted &&
                serviceBound &&
                patchService != null

        restoreCacheButton.isEnabled =
            patchAccessGranted &&
                serviceBound &&
                patchService != null
    }

    private fun setStatus(
        message: String
    ) {

        statusText.text = message
    }

    override fun onDestroy() {

        Shizuku.removeRequestPermissionResultListener(
            permissionListener
        )

        if (serviceBound) {

            try {

                Shizuku.unbindUserService(
                    createServiceArgs(),
                    serviceConnection,
                    true
                )

            } catch (_: Throwable) {
            }
        }

        patchService = null
        serviceBound = false

        super.onDestroy()
    }
}
