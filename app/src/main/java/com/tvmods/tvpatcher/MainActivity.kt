package com.tvmods.tvpatcher

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private lateinit var status: TextView
    private lateinit var auth: TextView

    private val shizukuPackage = "moe.shizuku.privileged.api"
    private val bytezukuPackage = "com.byteus.bytezuku"
    private val normalPackage = "com.AnotherAxiom.GorillaTag"
    private val modPackage = "com.TvMods.GorillaTag"

    private val requestCode = 1001

    private val binderReceived = Shizuku.OnBinderReceivedListener { refresh() }
    private val binderDead = Shizuku.OnBinderDeadListener { refresh() }
    private val permResult = Shizuku.OnRequestPermissionResultListener { code, result ->
        if (code == requestCode) refresh()
        if (code == requestCode && result == PackageManager.PERMISSION_GRANTED) {
            auth.text = "Auth: granted"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 48, 40, 40)
        }

        val title = TextView(this).apply {
            text = "TvPatcher"
            textSize = 28f
        }

        status = TextView(this).apply {
            textSize = 16f
            setPadding(0, 24, 0, 24)
        }

        auth = TextView(this).apply {
            textSize = 16f
            setPadding(0, 8, 0, 24)
        }

        val grant = Button(this).apply {
            text = "Authorize (Shizuku / ByteZuku)"
            setOnClickListener { requestAuth() }
        }

        val openHelper = Button(this).apply {
            text = "Open Shizuku / ByteZuku"
            setOnClickListener { openHelperApp() }
        }

        val openNormal = Button(this).apply {
            text = "Open normal Gorilla Tag"
            setOnClickListener { launch(normalPackage) }
        }

        val openMod = Button(this).apply {
            text = "Open modded Gorilla Tag"
            setOnClickListener { launch(modPackage) }
        }

        root.addView(title)
        root.addView(status)
        root.addView(auth)
        root.addView(grant)
        root.addView(openHelper)
        root.addView(openNormal)
        root.addView(openMod)
        setContentView(root)

        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permResult)
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
        Shizuku.removeRequestPermissionResultListener(permResult)
        super.onDestroy()
    }

    private fun requestAuth() {
        if (!installed(shizukuPackage) && !installed(bytezukuPackage)) {
            auth.text = "Auth: install Shizuku or ByteZuku first"
            return
        }
        if (!Shizuku.pingBinder()) {
            auth.text = "Auth: helper installed but service is not running.\nOpen Shizuku/ByteZuku and start it (wireless debugging / ADB), then tap Authorize again."
            openHelperApp()
            return
        }
        if (Shizuku.isPreV11()) {
            auth.text = "Auth: helper version is too old (need v11+)"
            return
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            auth.text = "Auth: already granted (uid ${safeUid()})"
            return
        }
        Shizuku.requestPermission(requestCode)
        auth.text = "Auth: waiting for the allow prompt…"
    }

    private fun refresh() {
        status.text = buildStatus()
        auth.text = "Auth: ${authState()}"
    }

    private fun authState(): String {
        val helpers = buildString {
            append(if (installed(shizukuPackage)) "Shizuku installed" else "Shizuku missing")
            append(" / ")
            append(if (installed(bytezukuPackage)) "ByteZuku installed" else "ByteZuku missing")
        }
        if (!Shizuku.pingBinder()) return "$helpers — service not running"
        if (Shizuku.isPreV11()) return "$helpers — too old"
        return if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            "$helpers — GRANTED (uid ${safeUid()})"
        } else {
            "$helpers — service up, not granted yet"
        }
    }

    private fun safeUid(): String = try {
        Shizuku.getUid().toString()
    } catch (_: Throwable) {
        "?"
    }

    private fun buildStatus(): String {
        return "Shizuku pkg: ${if (installed(shizukuPackage)) "found" else "not found"}\n" +
            "ByteZuku pkg: ${if (installed(bytezukuPackage)) "found" else "not found"}\n" +
            "Normal GTAG: ${if (installed(normalPackage)) "found" else "not found"}\n" +
            "Modded GTAG: ${if (installed(modPackage)) "found" else "not found"}"
    }

    private fun openHelperApp() {
        when {
            installed(bytezukuPackage) -> launch(bytezukuPackage)
            installed(shizukuPackage) -> launch(shizukuPackage)
            else -> auth.text = "Auth: neither helper is installed"
        }
    }

    private fun installed(pkg: String): Boolean =
        try {
            packageManager.getPackageInfo(pkg, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    private fun launch(pkg: String) {
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) startActivity(intent)
        else status.text = "Unable to launch $pkg"
    }
}
