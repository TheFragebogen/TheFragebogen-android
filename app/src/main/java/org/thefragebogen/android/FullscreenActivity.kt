package org.thefragebogen.android

import android.Manifest
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LOCKED
import android.content.pm.PackageManager
import android.os.Environment
import android.support.v4.content.ContextCompat
import android.view.WindowManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import android.util.Log
import android.webkit.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * TheFragebogen's companion app.
 *
 * Shows a HTML file  and presents it in fullscreen, with fixed brightness, and keeping the orientation locked.
 *
 * The file is loaded from:
 * file:///+ Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+/index.html
 */
class FullscreenActivity : AppCompatActivity() {

    val PERMISSION_REQUEST_CODE = 404

    lateinit var mWebView : WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        //Set screen brightness to max (constant)
        val layoutParams = window.attributes
        layoutParams.screenBrightness = 1.0f
        window.attributes = layoutParams

        //Lock screen orientation
        requestedOrientation = SCREEN_ORIENTATION_LOCKED

        //Setup UI
        setContentView(R.layout.activity_fullscreen)
        mWebView = findViewById<WebView>(R.id.webview)

        //Hide actionbar initially
        mWebView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)

        //Configure WebView
        mWebView.settings.javaScriptEnabled = true
        mWebView.settings.allowFileAccess = true
        mWebView.settings.allowUniversalAccessFromFileURLs = true
        mWebView.settings.allowContentAccess = true
        mWebView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        mWebView.settings.domStorageEnabled = true
        //Open URLs within WebView
        this.mWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }
        }

        //If a download is triggered in the WebView, request the screenController's data.
        //TODO This is only a workaround as Android's donwload manager does not (yet/anymore) support blob-URLs.
        mWebView.setDownloadListener { url, _, _, _, _ ->
            if (url.startsWith("blob")) {
                Log.i(localClassName, "Website triggered download via blob, requesting data via encodeURI(screenController.requestDataCSV())")

                mWebView.evaluateJavascript("javascript:encodeURI(screenController.requestDataCSV())", ValueCallback<String> {
                    Log.d(localClassName, "Received data and decoded it\n: $it")

                    //Decoding
                    var dataDecoded = URLDecoder.decode(it, StandardCharsets.UTF_8.name())
                    dataDecoded = dataDecoded.substring(1, dataDecoded.length - 1)
                    exportData(dataDecoded)
                })
            } else {
                Log.i(localClassName, "Ignoring download request for (non-blob URL): $url")
            }
        }

        //Require file access permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            loadUrl()
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.w(localClassName, "Permission has been denied by user")
                    Toast.makeText(this, "Permission are required. Please restart the app and grant permissions.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Log.i(localClassName, "Permission has been granted by user")
                    loadUrl()
                }
            }
        }
    }

    private fun loadUrl() {
        mWebView.loadUrl("file:///" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/index.html")
    }

    private fun exportData(data: String) {
        val fileName = "TheFragebogen-${Calendar.getInstance().timeInMillis}.csv"
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(path, fileName)


        Log.i(localClassName, "Storing data to ${file.absolutePath}")
        Toast.makeText(this, "Storing data to ${file.absolutePath}", Toast.LENGTH_LONG).show()

        file.writeText(data)
    }
}
