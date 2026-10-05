package com.local.friendfilter

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONTokener

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private lateinit var status: TextView
    private lateinit var stats: TextView
    private lateinit var dashboard: ScrollView
    private lateinit var browser: LinearLayout
    private lateinit var loading: LinearLayout
    private lateinit var loadingDetail: TextView
    private val prefs by lazy { getSharedPreferences("friendfilter_local", MODE_PRIVATE) }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        web = findViewById(R.id.web)
        status = findViewById(R.id.status)
        stats = findViewById(R.id.stats)
        dashboard = findViewById(R.id.dashboard)
        browser = findViewById(R.id.browserPanel)
        loading = findViewById(R.id.loadingOverlay)
        loadingDetail = findViewById(R.id.loadingDetail)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = false
        web.settings.allowContentAccess = false
        web.settings.userAgentString = web.settings.userAgentString.replace("; wv", "")
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView?, r: WebResourceRequest?): Boolean {
                val h = r?.url?.host.orEmpty().lowercase()
                return !(h == "facebook.com" || h.endsWith(".facebook.com"))
            }
            override fun onPageFinished(v: WebView?, url: String?) {
                status.text = "Facebook đã mở • phiên đăng nhập được giữ trên máy"
            }
        }

        findViewById<Button>(R.id.loginBtn).setOnClickListener { openBrowser("https://www.facebook.com/") }
        findViewById<Button>(R.id.friendsBtn).setOnClickListener { openFriends() }
        findViewById<Button>(R.id.backBtn).setOnClickListener { showDashboard() }
        findViewById<Button>(R.id.scanBtn).setOnClickListener { scanVisibleProfiles() }
        findViewById<Button>(R.id.resultsBtn).setOnClickListener { showResultsControls() }
        refreshStats()
    }

    private fun openFriends() {
        // Facebook frequently changes/removes /friends/list. The profile friends surface is more resilient.
        openBrowser("https://www.facebook.com/me/friends")
    }

    private fun openBrowser(url: String) {
        dashboard.visibility = View.GONE
        browser.visibility = View.VISIBLE
        web.loadUrl(url)
    }

    private fun showDashboard() {
        browser.visibility = View.GONE
        dashboard.visibility = View.VISIBLE
        refreshStats()
    }

    private fun setLoading(show: Boolean, detail: String = "Đang đọc dữ liệu hiển thị trên trang") {
        loadingDetail.text = detail
        loading.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun scanVisibleProfiles() {
        setLoading(true, "Đang nhận diện hồ sơ, loại trùng và lưu cục bộ…")
        val js = """(()=>{const m=new Map();document.querySelectorAll('a[href]').forEach(a=>{let n=(a.innerText||a.getAttribute('aria-label')||'').trim();let h=a.href||'';if(!n||!h.includes('facebook.com/'))return;if(/\/(friends|groups|watch|marketplace|reel|photo|gaming|events|notifications|messages)(\/|\?|$)/.test(h))return;try{let u=new URL(h);['__cft__','__tn__','ref','refid','mibextid'].forEach(k=>u.searchParams.delete(k));h=u.origin+u.pathname+(u.searchParams.toString()?'?'+u.searchParams:'')}catch(e){}if(n.length>=2&&n.length<=100)m.set(h,n)});return JSON.stringify([...m].map(([url,name])=>({name,url,observedAt:Date.now(),likes:null,comments:null,messages:null})))})()"""
        web.evaluateJavascript(js) { raw ->
            try {
                val decoded = JSONTokener(raw).nextValue() as String
                val incoming = JSONArray(decoded)
                val before = profileArray().length()
                merge(incoming)
                val after = profileArray().length()
                val added = after - before
                setLoading(false)
                status.text = "Quét xong • +$added hồ sơ mới • tổng $after"
                refreshStats()
                AlertDialog.Builder(this)
                    .setTitle("Quét hoàn tất ✦")
                    .setMessage("Đã thấy ${incoming.length()} mục trên trang.\nThêm mới: $added hồ sơ.\nTổng đã lưu: $after hồ sơ.\n\nCuộn thêm danh sách rồi bấm Ghi nhận trang để tiếp tục.")
                    .setPositiveButton("OK", null).show()
            } catch (e: Exception) {
                setLoading(false)
                status.text = "Chưa đọc được danh sách trên trang này."
            }
        }
    }

    private fun profileArray() = JSONArray(prefs.getString("profiles", "[]"))

    private fun merge(incoming: JSONArray) {
        val map = linkedMapOf<String, org.json.JSONObject>()
        val old = profileArray()
        for (i in 0 until old.length()) {
            val o = old.getJSONObject(i)
            map[o.optString("url")] = o
        }
        for (i in 0 until incoming.length()) {
            val o = incoming.getJSONObject(i)
            map[o.optString("url")] = o
        }
        val out = JSONArray()
        map.values.forEach { out.put(it) }
        prefs.edit().putString("profiles", out.toString()).apply()
    }

    private fun refreshStats() {
        stats.text = profileArray().length().toString()
    }

    private fun showResultsControls() {
        val a = profileArray()
        if (a.length() == 0) {
            AlertDialog.Builder(this).setTitle("Chưa có dữ liệu")
                .setMessage("Mở danh sách bạn bè, cuộn để tải thêm rồi bấm Ghi nhận trang.")
                .setPositiveButton("OK", null).show()
            return
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(42, 20, 42, 0)
        }
        val sort = Spinner(this)
        val sortItems = arrayOf("Mới ghi nhận", "Tên A → Z", "Tên Z → A", "Like nhiều nhất*", "Comment nhiều nhất*", "Message nhiều nhất*")
        sort.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sortItems)
        val page = Spinner(this)
        val pageItems = arrayOf("20 / trang", "50 / trang", "100 / trang", "500 / trang", "1.000 / trang")
        page.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, pageItems)
        box.addView(TextView(this).apply { text = "Sắp xếp" })
        box.addView(sort)
        box.addView(TextView(this).apply { text = "\nSố lượng hiển thị" })
        box.addView(page)
        box.addView(TextView(this).apply { text = "\n* Like / Comment / Message chỉ sắp xếp khi có dữ liệu hợp lệ; không có sẽ hiển thị Chưa có dữ liệu." })

        AlertDialog.Builder(this).setTitle("Kết quả • ${a.length()} hồ sơ")
            .setView(box)
            .setNegativeButton("Đóng", null)
            .setPositiveButton("Xem") { _, _ ->
                val limits = intArrayOf(20, 50, 100, 500, 1000)
                showResults(sort.selectedItemPosition, limits[page.selectedItemPosition])
            }.show()
    }

    private fun showResults(sortMode: Int, limit: Int) {
        val a = profileArray()
        val list = mutableListOf<org.json.JSONObject>()
        for (i in 0 until a.length()) list.add(a.getJSONObject(i))
        when (sortMode) {
            1 -> list.sortBy { it.optString("name").lowercase() }
            2 -> list.sortByDescending { it.optString("name").lowercase() }
            3 -> list.sortByDescending { if (it.isNull("likes")) -1 else it.optInt("likes") }
            4 -> list.sortByDescending { if (it.isNull("comments")) -1 else it.optInt("comments") }
            5 -> list.sortByDescending { if (it.isNull("messages")) -1 else it.optInt("messages") }
            else -> list.sortByDescending { it.optLong("observedAt") }
        }
        val lines = list.take(limit).mapIndexed { i, o ->
            val like = if (o.isNull("likes") || !o.has("likes")) "—" else o.optInt("likes").toString()
            val comment = if (o.isNull("comments") || !o.has("comments")) "—" else o.optInt("comments").toString()
            val message = if (o.isNull("messages") || !o.has("messages")) "—" else o.optInt("messages").toString()
            "${i + 1}. ${o.optString("name")}\n👍 $like   💬 $comment   ✉ $message\n${o.optString("url")}"
        }
        AlertDialog.Builder(this)
            .setTitle("Hiển thị ${minOf(limit, list.size)} / ${list.size}")
            .setMessage(lines.joinToString("\n\n"))
            .setPositiveButton("Đóng", null).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (loading.visibility == View.VISIBLE) setLoading(false)
        else if (browser.visibility == View.VISIBLE) {
            if (web.canGoBack()) web.goBack() else showDashboard()
        } else super.onBackPressed()
    }
}
