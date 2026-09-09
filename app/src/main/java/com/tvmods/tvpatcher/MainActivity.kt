package com.tvmods.tvpatcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var status: TextView

    private val normalPackage = "com.AnotherAxiom.GorillaTag"
    private val modPackage = "com.TvMods.GorillaTag"
    private val bytezukuPackage = "com.byteus.bytezuku"

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
            text = buildStatus()
            textSize = 16f
            setPadding(0, 24, 0, 24)
        }

        val grant = Button(this).apply {
            text = "Grant patch access"
            setOnClickListener {
                // Bytezuku integration is intentionally kept behind an adapter.
                // The exact Bytezuku API/version must be supplied before wiring
                // privileged operations to avoid inventing an incompatible API.
                status.text = "Patch access: waiting for Bytezuku integration"
            }
        }

        val backup = Button(this).apply {
            text = "Backup cache"
            setOnClickListener {
                status.text = "Cache backup: adapter ready; elevated file service required"
            }
        }

        val copyObb = Button(this).apply {
            text = "Copy OBB"
            setOnClickListener {
                status.text = "OBB copy: adapter ready; elevated file service required"
            }
        }

        val openNormal = Button(this).apply {
            text = "Open normal Gorilla Tag"
            setOnClickListener { launch(normalPackage) }
        }

        val openMod = Button(this).apply {
            text = "Open modded Gorilla Tag"
            setOnClickListener { launch(modPackage) }
        }

        val auth = TextView(this).apply {
            text = "Auth: Not caught yet"
            textSize = 16f
            setPadding(0, 28, 0, 8)
        }

        root.addView(title)
        root.addView(status)
        root.addView(grant)
        root.addView(backup)
        root.addView(copyObb)
        root.addView(openNormal)
        root.addView(openMod)
        root.addView(auth)

        setContentView(root)
    }

    private fun buildStatus(): String {
        val bytezuku = installed(bytezukuPackage)
        val normal = installed(normalPackage)
        val mod = installed(modPackage)
        return "Bytezuku: ${if (bytezuku) "found" else "not found"}\n" +
               "Normal GTAG: ${if (normal) "found" else "not found"}\n" +
               "Modded GTAG: ${if (mod) "found" else "not found"}"
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
        if (intent != null) {
            startActivity(intent)
        } else {
            status.text = "Unable to launch $pkg"
        }
    }
}
