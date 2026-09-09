package com.tvmods.tvpatcher

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE = 100
    }

    private lateinit var grantButton: Button
    private lateinit var copyObbButton: Button
    private lateinit var restoreCacheButton: Button
    private lateinit var statusText: TextView

    private var patchService: IPatchService? = null

    private var serviceConnection: ServiceConnection? = null

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener {
                requestCode,
                grantResult ->

            if (requestCode != REQUEST_CODE) {
                return@OnRequestPermissionResultListener
            }

            runOnUiThread {

                if (
                    grantResult ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    setStatus(
                        "✓ Patch access granted."
                    )

                    updateButtons()

                    connectPatchService()

                } else {

                    setStatus(
                        "Patch access denied."
                    )

                    updateButtons()
                }
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_main
        )

        grantButton =
            findViewById(R.id.grantPatchAccess)

        copyObbButton =
            findViewById(R.id.copyObb)

        restoreCacheButton =
            findViewById(R.id.restoreCache)

        statusText =
            findViewById(R.id.statusText)

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

        updateButtons()
    }

    override fun onResume() {
        super.onResume()

        updatePermissionState()
    }

    private fun requestPatchAccess() {

        if (!Shizuku.pingBinder()) {

            setStatus(
                "Shizuku / Bytezuku is not running."
            )

            return
        }

        if (
            Shizuku.checkSelfPermission() ==
            PackageManager.PERMISSION_GRANTED
        ) {

            setStatus(
                "✓ Patch access already granted."
            )

            connectPatchService()

            return
        }

        if (
            Shizuku.shouldShowRequestPermissionRationale()
        ) {

            setStatus(
                "Permission was denied.\n" +
                "Allow TvPatcher in Shizuku / Bytezuku."
            )

            return
        }

        Shizuku.requestPermission(
            REQUEST_CODE
        )
    }

    private fun updatePermissionState() {

        if (!Shizuku.pingBinder()) {

            patchService = null

            setStatus(
                "Waiting for Shizuku / Bytezuku..."
            )

            updateButtons()

            return
        }

        if (
            Shizuku.checkSelfPermission() ==
            PackageManager.PERMISSION_GRANTED
        ) {

            setStatus(
                "✓ Patch access granted."
            )

            connectPatchService()

        } else {

            patchService = null

            setStatus(
                "Waiting for patch access..."
            )
        }

        updateButtons()
    }

    private fun connectPatchService() {

        if (patchService != null) {
            updateButtons()
            return
        }

        val args =
            Shizuku.UserServiceArgs(
                ComponentName(
                    this,
                    PatchService::class.java
                )
            )
                .daemon(false)
                .processName("patch")
                .debuggable(BuildConfig.DEBUG)
                .version(1)

        serviceConnection =
            object : ServiceConnection {

                override fun onServiceConnected(
                    name: ComponentName?,
                    binder: IBinder?
                ) {

                    patchService =
                        IPatchService.Stub.asInterface(
                            binder
                        )

                    runOnUiThread {
                        setStatus(
                            "✓ Patch service ready."
                        )

                        updateButtons()
                    }
                }

                override fun onServiceDisconnected(
                    name: ComponentName?
                ) {

                    patchService = null

                    runOnUiThread {
                        setStatus(
                            "Patch service disconnected."
                        )

                        updateButtons()
                    }
                }
            }

        try {

            Shizuku.bindUserService(
                args,
                serviceConnection!!
            )

            setStatus(
                "Starting patch service..."
            )

        } catch (e: Exception) {

            patchService = null

            setStatus(
                "Could not start patch service:\n" +
                "${e.message ?: "Unknown error"}"
            )
        }
    }

    private fun runCopyObb() {

        val service = patchService

        if (service == null) {
            setStatus(
                "Grant Patch Access first."
            )
            return
        }

        setWorking(true)

        Thread {

            val result =
                try {
                    service.copyObb()
                } catch (e: Exception) {
                    "ERROR: ${e.message ?: "Copy failed."}"
                }

            runOnUiThread {

                setWorking(false)
                setStatus(result)
            }

        }.start()
    }

    private fun runRestoreCache() {

        val service = patchService

        if (service == null) {
            setStatus(
                "Grant Patch Access first."
            )
            return
        }

        setWorking(true)

        Thread {

            val result =
                try {
                    service.restoreCache()
                } catch (e: Exception) {
                    "ERROR: ${e.message ?: "Restore failed."}"
                }

            runOnUiThread {

                setWorking(false)
                setStatus(result)
            }

        }.start()
    }

    private fun setWorking(
        working: Boolean
    ) {
        grantButton.isEnabled = !working
        copyObbButton.isEnabled = !working &&
                patchService != null
        restoreCacheButton.isEnabled = !working &&
                patchService != null
    }

    private fun updateButtons() {

        val ready =
            patchService != null

        copyObbButton.isEnabled =
            ready

        restoreCacheButton.isEnabled =
            ready

        grantButton.isEnabled =
            true
    }

    private fun setStatus(
        text: String
    ) {
        statusText.text = text
    }

    override fun onDestroy() {

        Shizuku.removeRequestPermissionResultListener(
            permissionListener
        )

        val connection =
            serviceConnection

        if (connection != null) {

            try {

                val args =
                    Shizuku.UserServiceArgs(
                        ComponentName(
                            this,
                            PatchService::class.java
                        )
                    )
                        .daemon(false)
                        .processName("patch")
                        .version(1)

                Shizuku.unbindUserService(
                    args,
                    connection,
                    true
                )

            } catch (_: Exception) {
            }
        }

        patchService = null

        super.onDestroy()
    }
}
