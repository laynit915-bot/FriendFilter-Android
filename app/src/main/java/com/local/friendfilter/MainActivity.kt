package com.local.friendfilter

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONTokener

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private lateinit var status: TextView
    private lateinit var stats: TextView
    private lateinit var dashboard: LinearLayout
    private lateinit var browser: LinearLayout
    private val prefs by lazy { getSharedPreferences("friendfilter_local", MODE_PRIVATE) }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_main)
        web=findViewById(R.id.web); status=findViewById(R.id.status); stats=findViewById(R.id.stats)
        dashboard=findViewById(R.id.dashboard); browser=findViewById(R.id.browserPanel)
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false)
        web.settings.javaScriptEnabled=true; web.settings.domStorageEnabled=true
        web.settings.allowFileAccess=false; web.settings.allowContentAccess=false
        web.settings.userAgentString=web.settings.userAgentString.replace("; wv", "")
        web.webViewClient=object:WebViewClient(){
            override fun shouldOverrideUrlLoading(v:WebView?, r:WebResourceRequest?):Boolean {
                val h=r?.url?.host.orEmpty().lowercase(); return !(h=="facebook.com" || h.endsWith(".facebook.com"))
            }
            override fun onPageFinished(v:WebView?, url:String?) { status.text="Facebook đã mở • phiên đăng nhập được giữ trên máy" }
        }
        findViewById<Button>(R.id.loginBtn).setOnClickListener { openBrowser("https://www.facebook.com/") }
        findViewById<Button>(R.id.friendsBtn).setOnClickListener { openBrowser("https://www.facebook.com/friends/list") }
        findViewById<Button>(R.id.backBtn).setOnClickListener { showDashboard() }
        findViewById<Button>(R.id.scanBtn).setOnClickListener { scanVisibleProfiles() }
        findViewById<Button>(R.id.resultsBtn).setOnClickListener { showResults() }
        refreshStats()
    }

    private fun openBrowser(url:String){ dashboard.visibility=View.GONE; browser.visibility=View.VISIBLE; web.loadUrl(url) }
    private fun showDashboard(){ browser.visibility=View.GONE; dashboard.visibility=View.VISIBLE; refreshStats() }

    private fun scanVisibleProfiles(){
        val js="""(()=>{const m=new Map(); document.querySelectorAll('a[href]').forEach(a=>{let n=(a.innerText||a.getAttribute('aria-label')||'').trim();let h=a.href||'';if(!n||!h.includes('facebook.com/'))return;if(/\/(friends|groups|watch|marketplace|reel|photo|gaming|events)(\/|\?|$)/.test(h))return;try{let u=new URL(h);['__cft__','__tn__','ref','refid','mibextid'].forEach(k=>u.searchParams.delete(k));h=u.origin+u.pathname+(u.searchParams.toString()?'?'+u.searchParams:'')}catch(e){}if(n.length>=2&&n.length<=100)m.set(h,n)});return JSON.stringify([...m].map(([url,name])=>({name,url,observedAt:Date.now()})))})()"""
        web.evaluateJavascript(js){ raw ->
            try { val decoded=JSONTokener(raw).nextValue() as String; merge(JSONArray(decoded)); status.text="Đã ghi nhận các hồ sơ nhìn thấy trên trang. Cuộn thêm rồi bấm lại để bổ sung."; refreshStats() }
            catch(e:Exception){ status.text="Chưa đọc được danh sách trên trang này." }
        }
    }

    private fun merge(incoming:JSONArray){
        val map=linkedMapOf<String, org.json.JSONObject>(); val old=JSONArray(prefs.getString("profiles","[]"))
        for(i in 0 until old.length()){ val o=old.getJSONObject(i); map[o.optString("url")]=o }
        for(i in 0 until incoming.length()){ val o=incoming.getJSONObject(i); map[o.optString("url")]=o }
        val out=JSONArray(); map.values.forEach{out.put(it)}; prefs.edit().putString("profiles",out.toString()).apply()
    }
    private fun refreshStats(){ val a=JSONArray(prefs.getString("profiles","[]")); stats.text="Đã ghi nhận: ${a.length()} hồ sơ" }
    private fun showResults(){
        val a=JSONArray(prefs.getString("profiles","[]")); if(a.length()==0){ AlertDialog.Builder(this).setTitle("Chưa có dữ liệu").setMessage("Mở danh sách bạn bè, cuộn để Facebook tải danh sách rồi bấm Ghi nhận trang.").setPositiveButton("OK",null).show(); return }
        val lines=mutableListOf<String>(); for(i in 0 until minOf(a.length(),120)){ val o=a.getJSONObject(i); lines.add("${i+1}. ${o.optString("name")}\n${o.optString("url")}") }
        AlertDialog.Builder(this).setTitle("Hồ sơ đã ghi nhận (${a.length()})").setMessage(lines.joinToString("\n\n") + if(a.length()>120) "\n\n… và ${a.length()-120} hồ sơ khác" else "").setPositiveButton("Đóng",null).show()
    }
    @Deprecated("Deprecated in Java") override fun onBackPressed(){ if(browser.visibility==View.VISIBLE){ if(web.canGoBack()) web.goBack() else showDashboard() } else super.onBackPressed() }
}
