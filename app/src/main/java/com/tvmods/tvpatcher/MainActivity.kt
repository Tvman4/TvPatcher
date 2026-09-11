package com.tvmods.tvpatcher

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.content.ServiceConnection
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import rikka.shizuku.Shizuku
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class MainActivity : AppCompatActivity() {
    companion object {
        private const val SHIZUKU_REQUEST_CODE = 1401
        private const val BYTEZUKU_PACKAGE = "com.byteus.bytezuku"
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        // Set this to the HTTPS address of the companion bot/API service.
        private const val API_BASE_URL = "https://tvbot-pvr5.onrender.com/"
        private const val META_LOGIN_URL = "https://auth.meta.com/?waterfall_id=1993c91a-485b-4c6d-bd41-bfc56f7da725&redirect_uri=https%3A%2F%2Fauth.meta.com%2Foidc%2F%3Fapp_id%3D800455417652954%26nonce%3DAdSAgq5Gh_Dp33ZBlbTC-n4Aopo%26redirect_uri%3Dhttps%253A%252F%252Fwww.meta.com%252Foidc%252Fcallback%252F%26response_type%3Dcode%26scope%3Dopenid%26state%3DATpyaIjyxxG0eW3TFA5F7o9XQo5w6mdVSvglhCzV7IzyI6lCmFjVeIWUR-TmcuBJL67prPDu-LP4Cq20XShReIm2PJ6PuPNknLXAr5tduLsXwtHQrJI36x2zcVC8X4P92rAwpimuQr4pVTA2alszzj82i-Yy__VF2cX8Jn69jAnuExQUGRMJ6r_R2ZnVZRESJ4da0u4EHJfy4OUEbFobr7qvy8d59lkQXGeG_FBgvzFrwUXbUOOIAB4GZEoax-wiVonkJA86VB4_TZnyvWlRBZNHtIugtDvm4N6OMb_pvhaXK49FeEy0yYUc6aF_PvnZsn52V4FWBnet8Ngrq2aJXmippW5ecx%26waterfall_id%3D1993c91a-485b-4c6d-bd41-bfc56f7da725&source_app_id=800455417652954&utm_source=meta.com&force_reauth=0"
    }

    private lateinit var tabs: TabLayout
    private lateinit var statusText: TextView
    private lateinit var otpCodeText: TextView
    private lateinit var otpChallengeText: TextView
    private lateinit var requestOtpButton: Button
    private lateinit var metaLoginButton: Button
    private lateinit var metaDoneButton: Button
    private lateinit var grantButton: Button
    private lateinit var copyObbButton: Button
    private lateinit var restoreCacheButton: Button
    private lateinit var checkAuthButton: Button
    private lateinit var redeemText: EditText
    private lateinit var patchLockedText: TextView

    private var patchAccessGranted = false
    private var serviceBound = false
    private var patchService: IPatchService? = null
    private var otpVerified = false
    private var metaLoginAcknowledged = false
    private var obbCopied = false

    private fun createServiceArgs(): Shizuku.UserServiceArgs =
        Shizuku.UserServiceArgs(ComponentName(this, PatchService::class.java))
            .daemon(false).tag("TvPatcher").version(2).processNameSuffix("patch")

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            patchService = IPatchService.Stub.asInterface(service)
            serviceBound = patchService != null
            runOnUiThread { updateUi(); setStatus(if (serviceBound) "✓ ADB patch service ready" else "Patch service failed.") }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            patchService = null; serviceBound = false
            runOnUiThread { updateUi(); setStatus("Patch service disconnected.") }
        }
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode != SHIZUKU_REQUEST_CODE) return@OnRequestPermissionResultListener
        runOnUiThread {
            patchAccessGranted = grantResult == PackageManager.PERMISSION_GRANTED
            updateUi()
            if (patchAccessGranted) { setStatus("✓ Patch access granted"); bindPatchService() }
            else setStatus("Patch access denied.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tabs = findViewById(R.id.tabs)
        statusText = findViewById(R.id.statusText)
        otpCodeText = findViewById(R.id.otpCode)
        otpChallengeText = findViewById(R.id.otpChallenge)
        requestOtpButton = findViewById(R.id.requestOtp)
        metaLoginButton = findViewById(R.id.metaLogin)
        metaDoneButton = findViewById(R.id.metaDone)
        grantButton = findViewById(R.id.grantPatchAccess)
        copyObbButton = findViewById(R.id.copyObb)
        restoreCacheButton = findViewById(R.id.restoreCache)
        checkAuthButton = findViewById(R.id.checkAuth)
        redeemText = findViewById(R.id.redeemText)
        patchLockedText = findViewById(R.id.patchLocked)

        tabs.addTab(tabs.newTab().setText("OTP"))
        tabs.addTab(tabs.newTab().setText("PATCHING"))
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) { updateUi() }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        requestOtpButton.setOnClickListener { requestOtp() }
        metaLoginButton.setOnClickListener { openMetaLogin() }
        metaDoneButton.setOnClickListener { metaLoginAcknowledged = true; setStatus("Meta login marked complete. Credentials were never read by TvPatcher."); updateUi() }
        grantButton.setOnClickListener { requestPatchAccess() }
        copyObbButton.setOnClickListener { runCopyObb() }
        restoreCacheButton.setOnClickListener { runRestoreCache() }
        checkAuthButton.setOnClickListener { runAuthCheck() }

        Shizuku.addRequestPermissionResultListener(permissionListener)
        updatePermissionState()
        updateUi()
    }

    override fun onResume() { super.onResume(); updatePermissionState(); updateUi() }

    private fun requestOtp() {
        if (API_BASE_URL.contains("YOUR-BOT-HOST")) {
            setStatus("Set API_BASE_URL in MainActivity.kt to your companion bot URL first.")
            return
        }
        val challenge = UUID.randomUUID().toString().replace("-", "").take(10).uppercase()
        otpChallengeText.text = "Challenge: $challenge"
        otpCodeText.text = "OTP: waiting…"
        requestOtpButton.isEnabled = false
        Thread {
            try {
                val conn = (URL("$API_BASE_URL/api/request").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; connectTimeout = 10000; readTimeout = 10000
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }
                conn.outputStream.use { it.write(JSONObject().put("challenge", challenge).toString().toByteArray()) }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val returned = json.optString("challenge", challenge)
                runOnUiThread { otpChallengeText.text = "Challenge: $returned"; pollOtp(returned); setStatus("✓ Request sent. Use /verify in the Discord bot.") }
            } catch (e: Exception) {
                runOnUiThread { requestOtpButton.isEnabled = true; setStatus("OTP request failed: ${e.message ?: "network error"}") }
            }
        }.start()
    }

    private fun pollOtp(challenge: String) {
        Thread {
            repeat(120) {
                try {
                    val conn = (URL("$API_BASE_URL/api/status?challenge=$challenge").openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"; connectTimeout = 7000; readTimeout = 7000
                    }
                    val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    if (json.optBoolean("verified", false)) {
                        val otp = json.optString("otp", "")
                        runOnUiThread {
                            otpCodeText.text = "OTP: $otp"
                            otpVerified = true
                            requestOtpButton.isEnabled = false
                            setStatus("✓ Purchase/headset verification accepted. PATCHING unlocked.")
                            updateUi()
                        }
                        return@Thread
                    }
                } catch (_: Exception) { }
                Thread.sleep(2500)
            }
            runOnUiThread { requestOtpButton.isEnabled = true; setStatus("OTP request timed out. Request another code.") }
        }.start()
    }

    private fun openMetaLogin() {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(META_LOGIN_URL))) }
        catch (e: Exception) { setStatus("Could not open Meta login: ${e.message}") }
    }

    private fun requestPatchAccess() {
        if (Shizuku.pingBinder()) { requestShizukuPermission(); return }
        if (isPackageInstalled(BYTEZUKU_PACKAGE)) {
            setStatus("Opening Bytezuku. If TvPatcher is already authorized, return here.")
            openPackage(BYTEZUKU_PACKAGE); return
        }
        if (isPackageInstalled(SHIZUKU_PACKAGE)) {
            setStatus("Opening Shizuku. Start its ADB service, then return here.")
            openPackage(SHIZUKU_PACKAGE); return
        }
        setStatus("No Bytezuku/Shizuku service found.")
    }

    private fun requestShizukuPermission() {
        if (!Shizuku.pingBinder()) { setStatus("ADB service is not running."); return }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            patchAccessGranted = true; updateUi(); bindPatchService(); setStatus("✓ Patch access already granted"); return
        }
        setStatus("Requesting TvPatcher ADB authorization…")
        Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
    }

    private fun updatePermissionState() {
        if (!Shizuku.pingBinder()) { patchAccessGranted = false; serviceBound = false; patchService = null; return }
        patchAccessGranted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        if (patchAccessGranted) bindPatchService()
    }

    private fun bindPatchService() {
        if (!Shizuku.pingBinder() || !patchAccessGranted || serviceBound) return
        try { Shizuku.bindUserService(createServiceArgs(), serviceConnection) }
        catch (e: Throwable) { setStatus("ADB service start failed: ${e.message ?: e.javaClass.simpleName}") }
    }

    private fun runCopyObb() {
        if (!checkReady()) return
        copyObbButton.isEnabled = false; restoreCacheButton.isEnabled = false
        setStatus("Running ADB OBB copy…")
        Thread {
            val result = try { patchService!!.copyObb() } catch (e: Throwable) { "OBB copy failed: ${e.message}" }
            runOnUiThread { obbCopied = result.startsWith("✓"); updateUi(); setStatus(result) }
        }.start()
    }

    private fun runRestoreCache() {
        if (!checkReady()) return
        copyObbButton.isEnabled = false; restoreCacheButton.isEnabled = false
        setStatus("Running ADB cache copy…")
        Thread {
            val result = try { patchService!!.restoreCache() } catch (e: Throwable) { "Cache copy failed: ${e.message}" }
            runOnUiThread { updateUi(); setStatus(result) }
        }.start()
    }

    private fun runAuthCheck() {
        if (!checkReady()) return
        if (!metaLoginAcknowledged) { setStatus("Finish the external Meta login first."); return }
        if (!obbCopied) { setStatus("Copy the OBB first."); return }
        val marker = redeemText.text.toString().trim()
        if (marker.length < 2) { setStatus("Enter the exact text you used in the real game's redeem field."); return }
        checkAuthButton.isEnabled = false
        setStatus("Checking ADB/log output for the redeem marker…")
        Thread {
            val result = try { patchService!!.checkAuthMarker(marker) } catch (e: Throwable) { "Auth check failed: ${e.message}" }
            runOnUiThread { checkAuthButton.isEnabled = true; setStatus(result) }
        }.start()
    }

    private fun checkReady(): Boolean {
        if (!Shizuku.pingBinder()) { setStatus("Start Bytezuku/Shizuku's ADB service first."); return false }
        if (!patchAccessGranted) { setStatus("Grant Patch Access first."); return false }
        if (!serviceBound || patchService == null) { bindPatchService(); setStatus("ADB patch service is starting…"); return false }
        if (!otpVerified) { setStatus("Verify the purchase/headset in OTP first."); return false }
        return true
    }

    private fun updateUi() {
        val unlocked = otpVerified
        patchLockedText.visibility = if (unlocked) TextView.GONE else TextView.VISIBLE
        val patchingTabSelected = tabs.selectedTabPosition == 1
        if (patchingTabSelected && !unlocked) tabs.getTabAt(0)?.select()
        grantButton.isEnabled = unlocked
        metaLoginButton.isEnabled = unlocked
        metaDoneButton.isEnabled = unlocked
        copyObbButton.isEnabled = unlocked && patchAccessGranted && serviceBound && metaLoginAcknowledged
        restoreCacheButton.isEnabled = unlocked && patchAccessGranted && serviceBound && metaLoginAcknowledged
        checkAuthButton.isEnabled = unlocked && patchAccessGranted && serviceBound && metaLoginAcknowledged && obbCopied
    }

    private fun setStatus(text: String) { runOnUiThread { statusText.text = text } }

    private fun isPackageInstalled(packageName: String): Boolean = try { packageManager.getPackageInfo(packageName, 0); true } catch (_: PackageManager.NameNotFoundException) { false }

    private fun openPackage(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) { setStatus("Could not open $packageName."); return }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(intent)
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        if (serviceBound) { try { Shizuku.unbindUserService(createServiceArgs(), serviceConnection, true) } catch (_: Throwable) {} }
        super.onDestroy()
    }
}
