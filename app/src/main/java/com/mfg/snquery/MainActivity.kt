package com.mfg.snquery

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.http.SslError
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * ============================================================
 *  設定區
 * ============================================================
 *  伺服器 IP 現在存在手機本機（SharedPreferences），
 *  不用改程式碼重新編譯 —— 開 App 點右上角 ⚙ 就能改。
 *  這裡的值只在「手機上還沒設定過」時，當作第一次的預設值。
 */
object AppConfig {
    const val DEFAULT_IP = "10.41.40.38"
    const val HTTPS_PORT = "8091"
    const val PAGE_PATH = "/sn_query"

    private const val PREFS_NAME = "sn_query_prefs"
    private const val KEY_SERVER_IP = "server_ip"

    fun getServerIp(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SERVER_IP, DEFAULT_IP) ?: DEFAULT_IP
    }

    fun setServerIp(context: Context, ip: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SERVER_IP, ip.trim()).apply()
    }

    fun buildUrl(context: Context): String =
        "https://${getServerIp(context)}:$HTTPS_PORT$PAGE_PATH"
}

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private val CAMERA_PERMISSION_REQUEST_CODE = 1001

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)

        val root = FrameLayout(this)
        root.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val settingsBtn = TextView(this).apply {
            text = "⚙"
            textSize = 20f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#66000000"))
            setPadding(24, 12, 24, 12)
            setOnClickListener { showServerSettingDialog() }
        }
        val btnParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        btnParams.gravity = Gravity.TOP or Gravity.END
        btnParams.rightMargin = 20
        root.addView(settingsBtn, btnParams)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val params = settingsBtn.layoutParams as FrameLayout.LayoutParams
            params.topMargin = bars.top + 12
            settingsBtn.layoutParams = params
            webView.setPadding(0, 0, 0, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)

        setContentView(root)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE
            )
        }

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.textZoom = 100

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread { request.grant(request.resources) }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                val host = error.url?.let { android.net.Uri.parse(it).host } ?: ""
                if (host == AppConfig.getServerIp(this@MainActivity)) {
                    handler.proceed()
                } else {
                    handler.cancel()
                    Toast.makeText(
                        this@MainActivity,
                        "不受信任的網站，已阻擋：$host",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        loadServerUrl()
    }

    private fun loadServerUrl() {
        webView.loadUrl(AppConfig.buildUrl(this))
    }

    private fun showServerSettingDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(AppConfig.getServerIp(this@MainActivity))
            hint = "例如 10.41.40.38"
        }
        val padding = (16 * resources.displayMetrics.density).toInt()
        val container = FrameLayout(this).apply {
            setPadding(padding, padding, padding, 0)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("伺服器 IP 設定")
            .setMessage("目前連線：${AppConfig.buildUrl(this)}")
            .setView(container)
            .setPositiveButton("儲存並重新連線") { _, _ ->
                val newIp = input.text.toString().trim()
                if (newIp.isNotEmpty()) {
                    AppConfig.setServerIp(this, newIp)
                    Toast.makeText(this, "已切換到 $newIp，重新連線中...", Toast.LENGTH_SHORT).show()
                    loadServerUrl()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
