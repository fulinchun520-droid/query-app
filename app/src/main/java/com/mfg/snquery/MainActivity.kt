package com.mfg.snquery

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.http.SslError
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * ============================================================
 *  設定區 —— 之後換伺服器 IP / 網址，只要改這裡就好
 * ============================================================
 */
object AppConfig {
    // App 開啟後預設載入的網址
    const val START_URL = "https://10.41.40.38:8091/sn_query"

    // 只信任這個 host 的 SSL 憑證錯誤（自簽憑證用）。
    // 其他任何網域一律照正常安全規則處理，不會被放行。
    const val TRUSTED_HOST = "10.41.40.38"
}

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private val CAMERA_PERMISSION_REQUEST_CODE = 1001

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        // 先跟系統要相機權限（Android 6+ 需要 runtime permission）
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

        // ── 相機/麥克風權限：網頁呼叫 getUserMedia 時，App 直接放行 ──
        // (瀏覽器版本卡很久的相機權限問題，在原生 App 裡自己控制就不會卡)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    request.grant(request.resources)
                }
            }
        }

        // ── SSL 憑證處理：只信任 AppConfig.TRUSTED_HOST 這個網域的自簽憑證 ──
        // 其餘網域一律走正常憑證驗證流程，不會被放行。
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                val host = error.url?.let { android.net.Uri.parse(it).host } ?: ""
                if (host == AppConfig.TRUSTED_HOST) {
                    handler.proceed()   // 只放行我們自己這台已知的伺服器
                } else {
                    handler.cancel()    // 其他網域一律照正常規則擋掉
                    Toast.makeText(
                        this@MainActivity,
                        "不受信任的網站，已阻擋：$host",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        webView.loadUrl(AppConfig.START_URL)
    }

    // 讓實體返回鍵在網頁瀏覽記錄裡「上一頁」，而不是直接關閉 App
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
