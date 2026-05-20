package com.example.mybussines.brokers.xtb

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mybussines.R

class XtbWebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var tvStatus: TextView
    private lateinit var btnSaveSession: Button

    private lateinit var sessionStore: XtbSessionStore
    private lateinit var bridge: XtbWebBridge

    private val handler = Handler(Looper.getMainLooper())

    private var returnedToApp = false
    private var loginDetected = false

    private val baseUrl = "https://xstation5.xtb.com/"

    private val importantPatterns = listOf(
        "balancesummaryservice/subscribebalancesummary",
        "balancesummaryservice/subscribepersonsummary",
        "positionservice/subscribeportfoliopositiongroups",
        "historyreportservice/subscribereportshistory",
        "portfolioclosedpositionservice/getclosedpositions",
        "portfolioclosedpositionservice/getclosedpositionsnetprofit",
        "homeviewportfolioservice/subscribetiles",
        "orderservice/subscribeordergroups"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_xtb_webview)

        webView = findViewById(R.id.webView)
        tvStatus = findViewById(R.id.tvStatus)
        btnSaveSession = findViewById(R.id.btnSaveSession)

        sessionStore = XtbSessionStore(this)
        sessionStore.clearCapturedRequests()

        bridge = XtbWebBridge(this)

        setupWebView()

        btnSaveSession.setOnClickListener {
            persistSession(webView.url)
            setResult(RESULT_OK)
            finish()
        }

        webView.loadUrl(baseUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        WebView.setWebContentsDebuggingEnabled(true)

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            javaScriptCanOpenWindowsAutomatically = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString =
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        }

        webView.addJavascriptInterface(bridge, "XTBBridge")
        webView.webChromeClient = WebChromeClient()

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean = false

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                tvStatus.text = "Ładowanie: $url"
                Log.d("XTB_WEBVIEW", "Page started: $url")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                injectNetworkHook()
                persistSession(url)

                if (looksLoggedIn(url)) {
                    loginDetected = true
                    tvStatus.text = "Zalogowano. Czekam na endpointy..."
                    scheduleEndpointCheck()
                } else {
                    tvStatus.text = "Załadowano: $url"
                }

                Log.d("XTB_WEBVIEW", "Page finished: $url")
            }
        }
    }

    private fun scheduleEndpointCheck() {
        handler.postDelayed({
            val requests = sessionStore.getCapturedRequests()
            val importantCount = requests.count {
                isImportantEndpoint(it.url.lowercase())
            }

            Log.d("XTB_SESSION", "capturedRequests=${requests.size}")
            Log.d("XTB_SESSION", "importantRequests=$importantCount")

            if (importantCount > 0) {
                tvStatus.text = "Złapano $importantCount kluczowych endpointów"
            }

            if (!returnedToApp && importantCount >= 3) {
                returnedToApp = true
                persistSession(webView.url)
                setResult(RESULT_OK)
                finish()
            }
        }, 6000L)

        handler.postDelayed({
            if (!returnedToApp && loginDetected) {
                returnedToApp = true
                persistSession(webView.url)
                setResult(RESULT_OK)
                finish()
            }
        }, 18000L)
    }

    private fun isImportantEndpoint(url: String): Boolean {
        return importantPatterns.any { url.contains(it) }
    }

    private fun looksLoggedIn(url: String?): Boolean {
        if (url.isNullOrBlank()) return false

        val normalized = url.lowercase()
        val cookies = CookieManager.getInstance()
            .getCookie("https://xstation5.xtb.com")
            .orEmpty()

        return (
                normalized.contains("#/real") ||
                        normalized.contains("loggedin") ||
                        normalized.contains("xstation5.xtb.com/")
                ) && cookies.isNotBlank()
    }

    private fun persistSession(lastUrl: String?) {
        val cookieManager = CookieManager.getInstance()

        val cookiesForBase = cookieManager.getCookie(baseUrl).orEmpty()
        val cookiesForHost = cookieManager.getCookie("https://xstation5.xtb.com").orEmpty()
        val cookies = if (cookiesForBase.isNotBlank()) cookiesForBase else cookiesForHost

        val session = XtbSession(
            isLoggedIn = cookies.isNotBlank() || looksLoggedIn(lastUrl),
            lastUrl = lastUrl,
            cookies = cookies
        )

        sessionStore.saveSession(session)

        Log.d("XTB_SESSION", "Session saved. lastUrl=$lastUrl")
        Log.d("XTB_SESSION", "Cookies present=${cookies.isNotBlank()}")
        Log.d("XTB_SESSION", "Captured requests count=${sessionStore.getCapturedRequests().size}")
    }

    private fun injectNetworkHook() {
        val js = """
            (function() {
              if (window.__xtbHookInstalled) return;
              window.__xtbHookInstalled = true;

              function headersToObject(headers) {
                const obj = {};
                try {
                  if (!headers) return obj;

                  if (headers instanceof Headers) {
                    headers.forEach(function(value, key) {
                      obj[key] = value;
                    });
                    return obj;
                  }

                  if (Array.isArray(headers)) {
                    headers.forEach(function(pair) {
                      if (pair && pair.length >= 2) obj[pair[0]] = pair[1];
                    });
                    return obj;
                  }

                  if (typeof headers === "object") {
                    Object.keys(headers).forEach(function(key) {
                      obj[key] = headers[key];
                    });
                    return obj;
                  }
                } catch (e) {}
                return obj;
              }

              function shorten(value) {
                try {
                  if (value === undefined || value === null) return null;
                  const str = String(value);
                  return str.length > 2000 ? str.substring(0, 2000) : str;
                } catch (e) {
                  return null;
                }
              }

              function sendToAndroid(method, url, body, response, headers) {
                try {
                  if (!url) return;
                  window.XTBBridge.captureWithHeaders(
                    String(method || "GET"),
                    String(url),
                    body === undefined ? null : body,
                    response === undefined ? null : response,
                    JSON.stringify(headers || {})
                  );
                } catch (e) {}
              }

              const originalFetch = window.fetch;
              if (originalFetch) {
                window.fetch = async function(input, init) {
                  let url = "";
                  let method = "GET";
                  let body = null;
                  let headers = {};

                  try {
                    url = typeof input === "string" ? input : (input && input.url ? input.url : "");
                    method = (init && init.method) ? init.method : (input && input.method ? input.method : "GET");
                    body = init && init.body ? shorten(init.body) : null;

                    headers = headersToObject(
                      init && init.headers ? init.headers : (input && input.headers ? input.headers : null)
                    );
                  } catch (e) {}

                  const response = await originalFetch.apply(this, arguments);

                  try {
                    const cloned = response.clone();
                    let preview = null;

                    try {
                      preview = await cloned.text();
                    } catch (e) {
                      preview = null;
                    }

                    sendToAndroid(method, url, body, shorten(preview), headers);
                  } catch (e) {}

                  return response;
                };
              }

              const originalOpen = XMLHttpRequest.prototype.open;
              const originalSend = XMLHttpRequest.prototype.send;
              const originalSetRequestHeader = XMLHttpRequest.prototype.setRequestHeader;

              XMLHttpRequest.prototype.open = function(method, url) {
                this.__xtbMethod = method;
                this.__xtbUrl = url;
                this.__xtbHeaders = {};
                return originalOpen.apply(this, arguments);
              };

              XMLHttpRequest.prototype.setRequestHeader = function(name, value) {
                try {
                  this.__xtbHeaders[name] = value;
                } catch (e) {}
                return originalSetRequestHeader.apply(this, arguments);
              };

              XMLHttpRequest.prototype.send = function(body) {
                try {
                  const xhr = this;
                  const method = xhr.__xtbMethod || "GET";
                  const url = xhr.__xtbUrl || "";
                  const headers = xhr.__xtbHeaders || {};
                  const requestBody = shorten(body);

                  xhr.addEventListener("loadend", function() {
                    let responsePreview = null;
                    try {
                      responsePreview = shorten(xhr.responseText);
                    } catch (e) {
                      responsePreview = null;
                    }

                    sendToAndroid(method, url, requestBody, responsePreview, headers);
                  });
                } catch (e) {}

                return originalSend.apply(this, arguments);
              };
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }
}