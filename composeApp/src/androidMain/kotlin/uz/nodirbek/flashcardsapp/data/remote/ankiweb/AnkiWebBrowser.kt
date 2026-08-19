package uz.nodirbek.flashcardsapp.data.remote.ankiweb

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AnkiWebException(message: String) : Exception(message)

data class AnkiWebDeckSummary(val sharedId: String, val title: String)

data class AnkiWebDeckDetail(
    val sharedId: String,
    val title: String,
    val description: String,
    val statsLine: String
)

/**
 * Поиск и скачивание публичных колод из каталога AnkiWeb Shared Decks
 * (ankiweb.net/shared/decks) — без логина, только общедоступный контент.
 *
 * У Shared Decks нет официального публичного API: страница поиска — обычный
 * HTML-шелл, а сами результаты (и кнопка «Download» на странице колоды)
 * дорисовываются клиентским JS уже после события load (это SvelteKit SPA,
 * который асинхронно гидрируется и подгружает данные через свой внутренний
 * protobuf-эндпоинт). Поэтому нельзя просто прочитать DOM в onPageFinished —
 * нужно опрашивать DOM (poll) несколько раз с задержкой, пока не появятся
 * результаты или не истечёт таймаут. WebView также явно измеряется/лэйаутится
 * ненулевым размером — не будучи прикреплённым к оконной иерархии, Chromium
 * иначе может не выполнять полноценный рендер/раскладку страницы.
 */
class AnkiWebBrowser(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun searchUrl(query: String): String =
        "https://ankiweb.net/shared/decks?search=${URLEncoder.encode(query, "UTF-8")}"

    private fun detailUrl(sharedId: String): String =
        "https://ankiweb.net/shared/info/$sharedId"

    suspend fun search(query: String): List<AnkiWebDeckSummary> = suspendCancellableCoroutine { cont ->
        mainHandler.post {
            var resumed = false
            val cancelled = AtomicBoolean(false)
            val pollStarted = AtomicBoolean(false)
            val webView = createWebView()
            val url = searchUrl(query)
            fun finish(json: String?) {
                if (resumed) return
                resumed = true
                try {
                    Log.d(TAG, "search() response: query=\"$query\" rawJson=$json")
                    if (isRateLimited(json)) {
                        Log.e(TAG, "search() failed: query=\"$query\" is rate-limited by AnkiWeb")
                        cont.resumeWithException(AnkiWebException(RATE_LIMIT_MESSAGE))
                    } else {
                        val results = parseSummaries(json)
                        Log.d(TAG, "search() parsed: query=\"$query\" resultCount=${results.size}")
                        cont.resume(results)
                    }
                } finally {
                    mainHandler.post { webView.destroy() }
                }
            }
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, pageUrl: String) {
                    if (resumed) return
                    if (!pollStarted.compareAndSet(false, true)) return
                    Log.d(TAG, "search() page loaded: $pageUrl")
                    pollEvaluate(
                        webView = view,
                        script = SEARCH_RESULTS_JS,
                        isCancelled = { cancelled.get() },
                        isReady = { json -> parseSummaries(json).isNotEmpty() || isRateLimited(json) },
                        onFinished = { json -> finish(json) }
                    )
                }

                override fun onReceivedError(view: WebView, errorCode: Int, description: String?, failingUrl: String?) {
                    if (resumed) return
                    resumed = true
                    Log.e(TAG, "search() onReceivedError: query=\"$query\" code=$errorCode desc=$description url=$failingUrl")
                    cont.resumeWithException(AnkiWebException("Ошибка сети: $description"))
                    mainHandler.post { webView.destroy() }
                }
            }
            cont.invokeOnCancellation {
                cancelled.set(true)
                mainHandler.post { webView.destroy() }
            }
            Log.d(TAG, "search() request: query=\"$query\" url=$url")
            webView.loadUrl(url)
        }
    }

    suspend fun fetchDetail(sharedId: String): AnkiWebDeckDetail = suspendCancellableCoroutine { cont ->
        mainHandler.post {
            var resumed = false
            val cancelled = AtomicBoolean(false)
            val pollStarted = AtomicBoolean(false)
            val webView = createWebView()
            val url = detailUrl(sharedId)
            fun finish(json: String?) {
                if (resumed) return
                resumed = true
                try {
                    Log.d(TAG, "fetchDetail() response: sharedId=$sharedId rawJson=$json")
                    if (isRateLimited(json)) {
                        Log.e(TAG, "fetchDetail() failed: sharedId=$sharedId is rate-limited by AnkiWeb")
                        cont.resumeWithException(AnkiWebException(RATE_LIMIT_MESSAGE))
                    } else {
                        cont.resume(parseDetail(sharedId, json))
                    }
                } finally {
                    mainHandler.post { webView.destroy() }
                }
            }
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, pageUrl: String) {
                    if (resumed) return
                    if (!pollStarted.compareAndSet(false, true)) return
                    Log.d(TAG, "fetchDetail() page loaded: $pageUrl")
                    pollEvaluate(
                        webView = view,
                        script = DETAIL_INFO_JS,
                        isCancelled = { cancelled.get() },
                        isReady = { json -> !parseDetail(sharedId, json).title.isNullOrBlank() || isRateLimited(json) },
                        onFinished = { json -> finish(json) }
                    )
                }

                override fun onReceivedError(view: WebView, errorCode: Int, description: String?, failingUrl: String?) {
                    Log.w(TAG, "fetchDetail() onReceivedError: sharedId=$sharedId code=$errorCode desc=$description url=$failingUrl")
                }
            }
            cont.invokeOnCancellation {
                cancelled.set(true)
                mainHandler.post { webView.destroy() }
            }
            Log.d(TAG, "fetchDetail() request: sharedId=$sharedId url=$url")
            webView.loadUrl(url)
        }
    }

    suspend fun download(sharedId: String): File {
        val (downloadUrl, userAgent) = awaitDownloadUrl(sharedId)
        return withContext(Dispatchers.IO) {
            val cookie = CookieManager.getInstance().getCookie(downloadUrl)
            Log.d(
                TAG,
                "download() request: sharedId=$sharedId url=$downloadUrl userAgent=$userAgent " +
                    "cookie=${if (cookie.isNullOrBlank()) "<none>" else "<present, ${cookie.length} chars>"}"
            )
            val connection = URL(downloadUrl).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", userAgent)
            if (cookie != null) connection.setRequestProperty("Cookie", cookie)
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            try {
                val code = connection.responseCode
                Log.d(
                    TAG,
                    "download() response: sharedId=$sharedId code=$code message=${connection.responseMessage} " +
                        "contentType=${connection.contentType} contentLength=${connection.contentLengthLong} " +
                        "headers=${connection.headerFields}"
                )
                if (code != HttpURLConnection.HTTP_OK) {
                    val errorBody = try {
                        connection.errorStream?.bufferedReader()?.use { it.readText() }?.take(500)
                    } catch (e: Exception) {
                        null
                    }
                    Log.e(TAG, "download() failed: sharedId=$sharedId code=$code body=$errorBody")
                    throw AnkiWebException("Не удалось скачать колоду (код $code)")
                }
                val cacheDir = File(appContext.cacheDir, "ankiweb_download").apply { mkdirs() }
                val outFile = File(cacheDir, "deck_${sharedId}_${System.currentTimeMillis()}.apkg")
                connection.inputStream.use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                }
                Log.d(TAG, "download() saved: sharedId=$sharedId file=${outFile.path} bytes=${outFile.length()}")
                outFile
            } catch (e: Exception) {
                Log.e(TAG, "download() exception: sharedId=$sharedId url=$downloadUrl", e)
                throw e
            } finally {
                connection.disconnect()
            }
        }
    }

    /**
     * Ловим URL со скачиванием (одноразовый подписанный токен `?t=...`) через
     * [WebViewClient.shouldInterceptRequest] — этот колбэк вызывается ДО того, как WebView
     * реально отправит запрос по сети. Раньше мы ждали [WebView.setDownloadListener], но он
     * срабатывает уже ПОСЛЕ того, как сам WebView успел выполнить настоящий сетевой запрос
     * по этому URL (именно так WebView понимает, что это скачиваемый файл, а не страница) —
     * то есть одноразовый токен AnkiWeb уже "сжигался" самим WebView, и наш повторный запрос
     * тем же URL в [download] закономерно проваливался. Перехватывая запрос раньше, мы
     * отменяем внутреннюю попытку WebView (отдаём пустой ответ) и делаем ровно один настоящий
     * HTTP-запрос сами.
     */
    private suspend fun awaitDownloadUrl(sharedId: String): Pair<String, String> =
        suspendCancellableCoroutine { cont ->
            mainHandler.post {
                // shouldInterceptRequest вызывается WebView на фоновом потоке (не на главном, в
                // отличие от остальных колбэков здесь), поэтому обычный var небезопасен —
                // нужен атомарный флаг, чтобы cont.resume/resumeWithException вызвался ровно один раз.
                val resumed = AtomicBoolean(false)
                val webView = createWebView()
                val userAgent = webView.settings.userAgentString

                fun tryResumeWithDownloadUrl(url: String, source: String): Boolean {
                    if (!url.contains("/svc/shared/download-deck/")) return false
                    if (!resumed.compareAndSet(false, true)) {
                        Log.d(TAG, "awaitDownloadUrl(): $source saw download URL but already resumed, ignoring: $url")
                        return false
                    }
                    Log.d(TAG, "awaitDownloadUrl(): captured via $source: $url")
                    cont.resume(url to userAgent)
                    mainHandler.post { webView.destroy() }
                    return true
                }

                webView.setDownloadListener { url, _, _, _, _ -> tryResumeWithDownloadUrl(url, "setDownloadListener") }

                webView.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                        val url = request.url.toString()
                        return if (tryResumeWithDownloadUrl(url, "shouldInterceptRequest")) {
                            // Пустой ответ вместо настоящего сетевого запроса — токен остаётся неиспользованным.
                            WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                        } else {
                            null
                        }
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        if (resumed.get()) return
                        Log.d(TAG, "awaitDownloadUrl(): detail page loaded, clicking Download: $url")
                        pollEvaluate(
                            webView = view,
                            script = CLICK_DOWNLOAD_JS,
                            isCancelled = { resumed.get() },
                            isReady = { result -> result == "true" || result == "\"rate_limited\"" },
                            onFinished = { result ->
                                Log.d(TAG, "awaitDownloadUrl(): CLICK_DOWNLOAD_JS result=$result")
                                if (result != "true" && resumed.compareAndSet(false, true)) {
                                    val message = if (result == "\"rate_limited\"") RATE_LIMIT_MESSAGE
                                        else "Кнопка скачивания не найдена на странице колоды"
                                    Log.e(TAG, "awaitDownloadUrl(): failed for sharedId=$sharedId: $message")
                                    cont.resumeWithException(AnkiWebException(message))
                                    mainHandler.post { webView.destroy() }
                                }
                                // if result == "true", we wait for shouldInterceptRequest/setDownloadListener with the real URL
                            }
                        )
                    }

                    override fun onReceivedError(view: WebView, errorCode: Int, description: String?, failingUrl: String?) {
                        Log.w(TAG, "awaitDownloadUrl(): onReceivedError code=$errorCode desc=$description url=$failingUrl")
                    }
                }
                cont.invokeOnCancellation {
                    resumed.set(true)
                    mainHandler.post { webView.destroy() }
                }
                Log.d(TAG, "awaitDownloadUrl(): loading detail page for sharedId=$sharedId")
                webView.loadUrl(detailUrl(sharedId))
            }
        }

    /**
     * Опрашивает [script] через evaluateJavascript до [maxAttempts] раз с интервалом [delayMs],
     * пока [isReady] не вернёт true (страница SPA дорисовала нужный контент) или не кончатся попытки —
     * тогда возвращается последний полученный результат как есть.
     */
    private fun pollEvaluate(
        webView: WebView,
        script: String,
        maxAttempts: Int = 16,
        delayMs: Long = 400,
        isCancelled: () -> Boolean = { false },
        isReady: (String?) -> Boolean,
        onFinished: (String?) -> Unit
    ) {
        var attempt = 0
        fun tryOnce() {
            // The coroutine may be cancelled (and the WebView destroyed) between scheduling
            // this retry and it firing — skip the call instead of hitting a destroyed WebView.
            if (isCancelled()) return
            webView.evaluateJavascript(script) { result ->
                if (isCancelled()) return@evaluateJavascript
                attempt++
                val ready = isReady(result)
                if (ready || attempt >= maxAttempts) {
                    if (!ready) Log.w(TAG, "pollEvaluate(): gave up after $attempt attempts, lastResult=$result")
                    onFinished(result)
                } else {
                    mainHandler.postDelayed({ tryOnce() }, delayMs)
                }
            }
        }
        tryOnce()
    }

    private fun createWebView(): WebView {
        val webView = WebView(appContext)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.userAgentString = webView.settings.userAgentString + " FlashCardsAppAnkiBrowser/1.0"
        CookieManager.getInstance().setAcceptCookie(true)
        // Не прикреплён к оконной иерархии — даём ему явный ненулевой размер, иначе
        // некоторые версии WebView/Chromium не выполняют полноценный layout/рендер SPA.
        webView.visibility = View.GONE
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        webView.layout(0, 0, 1080, 1920)
        return webView
    }

    private fun parseSummaries(rawJson: String?): List<AnkiWebDeckSummary> {
        val json = unquoteEvaluateJavascriptResult(rawJson) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                val id = obj.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val title = obj.optString("title").ifBlank { "Без названия" }
                AnkiWebDeckSummary(sharedId = id, title = title)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseDetail(sharedId: String, rawJson: String?): AnkiWebDeckDetail {
        val json = unquoteEvaluateJavascriptResult(rawJson)
        return try {
            val obj = org.json.JSONObject(json ?: "{}")
            AnkiWebDeckDetail(
                sharedId = sharedId,
                title = obj.optString("title"),
                description = obj.optString("description"),
                statsLine = obj.optString("stats")
            )
        } catch (e: Exception) {
            AnkiWebDeckDetail(sharedId = sharedId, title = "", description = "", statsLine = "")
        }
    }

    /**
     * AnkiWeb жёстко ограничивает анонимный (без логина) доступ к Shared Decks: уже после первых
     * запросов подряд сервер вместо результатов/страницы колоды отдаёт HTML-заглушку
     * "Please log in to perform more searches." Без этой проверки такая заглушка молча
     * распознавалась бы как пустой список результатов ("Ничего не найдено") или как
     * отсутствующая кнопка скачивания — то есть колода выглядела рабочей, но скачать её было нельзя.
     */
    private fun isRateLimited(rawJson: String?): Boolean =
        unquoteEvaluateJavascriptResult(rawJson) == RATE_LIMIT_MARKER

    /** evaluateJavascript возвращает JSON-строку результата; если результат — JS-строка, она приходит в двойных кавычках JSON-строки. */
    private fun unquoteEvaluateJavascriptResult(raw: String?): String? {
        if (raw == null || raw == "null") return null
        return if (raw.startsWith("\"") && raw.endsWith("\"")) {
            org.json.JSONTokener(raw).nextValue() as? String
        } else {
            raw
        }
    }

    private companion object {
        /** Тег для logcat — фильтруйте по нему (`adb logcat -s $TAG`), чтобы видеть все запросы/ответы скачивания колод. */
        const val TAG = "AnkiWebBrowser"

        /** Должен совпадать со строкой, которую JS-скрипты ниже возвращают при обнаружении заглушки анонимного лимита AnkiWeb. */
        const val RATE_LIMIT_MARKER = "rate_limited"
        const val RATE_LIMIT_MESSAGE =
            "AnkiWeb временно ограничил анонимный доступ к Shared Decks (\"Please log in to perform more searches\"). " +
                "Подождите немного перед следующим запросом или попробуйте позже."

        /** Проверяет, не подменил ли AnkiWeb страницу заглушкой лимита анонимного доступа. */
        const val RATE_LIMIT_CHECK_JS = """
            (function() {
                var t = document.body.innerText || '';
                return t.indexOf('Please log in to perform') !== -1 || t.indexOf('log in to perform more searches') !== -1;
            })()
        """

        const val SEARCH_RESULTS_JS = """
            (function() {
                if ($RATE_LIMIT_CHECK_JS) { return '$RATE_LIMIT_MARKER'; }
                var out = [];
                document.querySelectorAll('a[href^="/shared/info/"]').forEach(function(a) {
                    var m = a.getAttribute('href').match(/\/shared\/info\/(\d+)/);
                    if (m) { out.push({ id: m[1], title: a.textContent.trim() }); }
                });
                return JSON.stringify(out);
            })();
        """

        const val DETAIL_INFO_JS = """
            (function() {
                if ($RATE_LIMIT_CHECK_JS) { return '$RATE_LIMIT_MARKER'; }
                var h1 = document.querySelector('h1');
                var title = h1 ? h1.textContent.trim() : '';
                var bodyText = document.body.innerText || '';
                var descStart = bodyText.indexOf('Description');
                var descEnd = bodyText.indexOf('Download');
                var description = (descStart >= 0 && descEnd > descStart)
                    ? bodyText.slice(descStart + 'Description'.length, descEnd).trim()
                    : '';
                var statsMatch = bodyText.match(/[\d.]+\s?(KB|MB)\.[^\n]*/);
                return JSON.stringify({
                    title: title,
                    description: description,
                    stats: statsMatch ? statsMatch[0] : ''
                });
            })();
        """

        const val CLICK_DOWNLOAD_JS = """
            (function() {
                if ($RATE_LIMIT_CHECK_JS) { return '$RATE_LIMIT_MARKER'; }
                var btns = document.querySelectorAll('button');
                for (var i = 0; i < btns.length; i++) {
                    if (btns[i].textContent.trim() === 'Download') { btns[i].click(); return true; }
                }
                return false;
            })();
        """
    }
}
