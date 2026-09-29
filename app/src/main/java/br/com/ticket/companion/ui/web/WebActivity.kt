package br.com.ticket.companion.ui.web

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.com.ticket.companion.BuildConfig
import br.com.ticket.companion.domain.web.NavigationDecision
import br.com.ticket.companion.domain.web.WebChromeBar
import br.com.ticket.companion.domain.web.WebNavigationPolicy
import br.com.ticket.companion.domain.web.WebOrigin
import br.com.ticket.companion.ui.MainActivity
import java.io.File
import java.io.IOException

/**
 * Atendimento web (a SPA do Ticket) dentro do app. Roda no processo `:web`, separado do Companion:
 * um estouro de memória do WebView não derruba o listener de notificações, e vice-versa.
 *
 * Regras que esta tela NÃO pode quebrar (a decisão em si vive em [WebNavigationPolicy], testada):
 *  - só a origem recebida em [EXTRA_ORIGIN] carrega aqui dentro; o resto abre no navegador;
 *  - câmera, microfone, localização e a ponte de notificações só valem para essa origem;
 *  - não usa Hilt, Room nem as preferências do Companion (SharedPreferences não sincroniza entre processos).
 */
class WebActivity : ComponentActivity() {
    private lateinit var origin: String
    private lateinit var policy: WebNavigationPolicy
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var cameraFile: File? = null
    private var cameraUri: Uri? = null
    private var mediaRequest: PermissionRequest? = null
    private var geoCallback: GeolocationPermissions.Callback? = null
    private var geoOrigin: String? = null

    private val fileChooser = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val callback = fileCallback ?: return@registerForActivityResult
        fileCallback = null
        var uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        val photo = cameraUri
        if (uris == null && result.resultCode == RESULT_OK && photo != null && (cameraFile?.length() ?: 0L) > 0L) {
            uris = arrayOf(photo)
        }
        callback.onReceiveValue(uris)
        cameraFile = null
        cameraUri = null
    }

    private val mediaPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val request = mediaRequest ?: return@registerForActivityResult
        mediaRequest = null
        val allowed = grantableResources(request)
        if (allowed.isEmpty()) request.deny() else request.grant(allowed)
    }

    private val locationPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val callback = geoCallback ?: return@registerForActivityResult
        val requestOrigin = geoOrigin.orEmpty()
        geoCallback = null
        geoOrigin = null
        callback.invoke(requestOrigin, hasLocationPermission(), false)
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val canonical = WebOrigin.normalize(intent.getStringExtra(EXTRA_ORIGIN), BuildConfig.DEBUG)
        if (canonical == null) {
            finish()
            return
        }
        origin = canonical
        policy = WebNavigationPolicy(canonical)
        title = "Atendimento"
        WebNotifications.ensureChannels(this)
        requestNotificationPermissionIfNeeded()
        startBackgroundService()
        createLayout()
        configureWebView()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
        webView.loadUrl("$canonical/")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, MENU_RELOAD, 0, "Recarregar").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(0, MENU_SETTINGS, 1, "Ajustes do app").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        MENU_RELOAD -> { webView.reload(); true }
        MENU_SETTINGS -> {
            startActivity(Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_STAY, true))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        fileCallback?.onReceiveValue(null)
        fileCallback = null
        if (::webView.isInitialized) webView.destroy()
        // Saiu de propósito (voltar/fechar): sem WebView não há o que manter conectado.
        if (isFinishing) stopService(Intent(this, TicketWebService::class.java))
        super.onDestroy()
    }

    private fun createLayout() {
        val root = FrameLayout(this)
        webView = WebView(this)
        root.addView(webView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
        }
        root.addView(progressBar, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        setContentView(root)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        webView.webViewClient = TicketWebViewClient()
        webView.webChromeClient = TicketWebChromeClient()
        webView.addJavascriptInterface(NativeNotificationBridge(), WebNotifications.BRIDGE_NAME)
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, _ -> download(url, userAgent, contentDisposition, mimetype) }
    }

    private fun updateBar(url: String?) {
        if (WebChromeBar.showsOn(url)) actionBar?.show() else actionBar?.hide()
    }

    private fun startBackgroundService() {
        ContextCompat.startForegroundService(this, Intent(this, TicketWebService::class.java).putExtra(EXTRA_ORIGIN, origin))
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !has(Manifest.permission.POST_NOTIFICATIONS)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun has(permission: String) = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun hasLocationPermission() = has(Manifest.permission.ACCESS_FINE_LOCATION) || has(Manifest.permission.ACCESS_COARSE_LOCATION)

    private fun permissionFor(resource: String): String? = when (resource) {
        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> Manifest.permission.CAMERA
        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> Manifest.permission.RECORD_AUDIO
        else -> null
    }

    private fun missingPermissions(request: PermissionRequest): Array<String> =
        request.resources.mapNotNull(::permissionFor).filterNot(::has).toTypedArray()

    /** Só câmera e microfone; qualquer outro recurso pedido pela página é negado. */
    private fun grantableResources(request: PermissionRequest): Array<String> =
        request.resources.filter { permissionFor(it)?.let(::has) == true }.toTypedArray()

    private fun download(url: String, userAgent: String?, contentDisposition: String?, mimetype: String?) {
        val scheme = url.toUri().scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return
        try {
            val filename = File(URLUtil.guessFileName(url, contentDisposition, mimetype)).name
            val request = DownloadManager.Request(url.toUri())
                .setMimeType(mimetype)
                .setTitle(filename)
                .setDescription("Baixando arquivo")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            CookieManager.getInstance().getCookie(url)?.let { request.addRequestHeader("Cookie", it) }
            userAgent?.let { request.addRequestHeader("User-Agent", it) }
            (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            Toast.makeText(this, "Download iniciado", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Não foi possível baixar o arquivo.", Toast.LENGTH_LONG).show()
        }
    }

    private fun openExternal(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Não foi possível abrir o link.", Toast.LENGTH_LONG).show()
        }
    }

    private fun createCameraUri(): Uri? = try {
        val file = File.createTempFile("ticket-camera-", ".jpg", externalCacheDir ?: cacheDir)
        cameraFile = file
        FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    } catch (_: IOException) {
        cameraFile = null
        null
    }

    private fun fileChooserIntent(): Intent {
        val content = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        val chooser = Intent.createChooser(content, "Selecionar arquivo")
        cameraUri = createCameraUri()
        cameraUri?.let { photoUri ->
            val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                .putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(camera))
        }
        return chooser
    }

    /** Só a origem travada pode pedir notificação nativa; o teste é feito na thread de UI, contra a URL atual. */
    private inner class NativeNotificationBridge {
        @JavascriptInterface
        fun post(title: String?, body: String?, tag: String?) {
            runOnUiThread {
                if (policy.isSameOrigin(webView.url)) WebNotifications.show(this@WebActivity, origin, title, body, tag)
            }
        }
    }

    private inner class TicketWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!request.isForMainFrame) return false
            return when (policy.decide(request.url.toString())) {
                NavigationDecision.LOAD_IN_APP -> false
                NavigationDecision.OPEN_EXTERNAL -> { openExternal(request.url); true }
                NavigationDecision.BLOCK -> true
            }
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            progressBar.visibility = View.VISIBLE
            updateBar(url)
        }

        // A SPA troca de rota sem recarregar a página: sem isto a barra ficaria presa ao estado da primeira carga.
        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) = updateBar(url)

        override fun onPageFinished(view: WebView, url: String?) {
            progressBar.visibility = View.GONE
            if (policy.isSameOrigin(url)) view.evaluateJavascript(WebNotifications.BRIDGE_SCRIPT, null)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            // O renderizador morreu (memória, crash): sem isto o processo inteiro cai. Recria a tela.
            recreate()
            return true
        }
    }

    private inner class TicketWebChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            progressBar.progress = newProgress
            progressBar.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
        }

        override fun onPermissionRequest(request: PermissionRequest) {
            if (!policy.isSameOrigin(request.origin.toString())) {
                request.deny()
                return
            }
            val missing = missingPermissions(request)
            if (missing.isEmpty()) {
                val allowed = grantableResources(request)
                if (allowed.isEmpty()) request.deny() else request.grant(allowed)
                return
            }
            mediaRequest?.deny()
            mediaRequest = request
            mediaPermissions.launch(missing)
        }

        override fun onGeolocationPermissionsShowPrompt(requestOrigin: String, callback: GeolocationPermissions.Callback) {
            if (!policy.isSameOrigin(requestOrigin)) {
                callback.invoke(requestOrigin, false, false)
                return
            }
            if (hasLocationPermission()) {
                callback.invoke(requestOrigin, true, false)
                return
            }
            geoCallback?.invoke(geoOrigin.orEmpty(), false, false)
            geoCallback = callback
            geoOrigin = requestOrigin
            locationPermissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }

        override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
            fileCallback?.onReceiveValue(null)
            fileCallback = callback
            return try {
                fileChooser.launch(fileChooserIntent())
                true
            } catch (_: ActivityNotFoundException) {
                fileCallback = null
                Toast.makeText(this@WebActivity, "Nenhum seletor de arquivos disponível.", Toast.LENGTH_LONG).show()
                false
            }
        }
    }

    companion object {
        const val EXTRA_ORIGIN = "br.com.ticket.companion.web.ORIGIN"
        private const val MENU_RELOAD = 1
        private const val MENU_SETTINGS = 2

        fun intent(context: Context, origin: String): Intent =
            Intent(context, WebActivity::class.java).putExtra(EXTRA_ORIGIN, origin)
    }
}
