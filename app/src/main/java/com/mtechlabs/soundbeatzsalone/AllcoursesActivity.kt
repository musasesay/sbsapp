package com.mtechlabs.soundbeatzsalone


import android.Manifest
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.ads.*
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.*



class AllcoursesActivity : AppCompatActivity(),ConnectivityReceiverListener,
NavigationView.OnNavigationItemSelectedListener,AdvancedWebView.Listener{

    private lateinit var mWebView : AdvancedWebView
    private var pBar : ProgressBar? = null
    private lateinit var request : DownloadManager.Request
    private val requetStorageCode = 100
    private var isShown : Boolean = false
    private val br = ConnectivityReceiver()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(Config.SIDEBAR_ENABLED) {
            setContentView(R.layout.activity_drawer)
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
            val drawerToggle = ActionBarDrawerToggle(this,drawer,toolbar,R.string.navigation_drawer_open,
                R.string.navigation_drawer_close)
            drawer.addDrawerListener(drawerToggle)
            drawerToggle.syncState()
            findViewById<NavigationView>(R.id.nav_view)
                .setNavigationItemSelectedListener(this)
        }else{
            setContentView(R.layout.activity_main)
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
        }

        pBar = findViewById(R.id.pb)
        mWebView = findViewById(R.id.webView)
        mWebView.setListener(this,this)

        if (Config.ADMOB_ENABLED) {
            findViewById<FrameLayout>(R.id.frame).visibility = View.VISIBLE
            setBannerAdds()
        } else {
            findViewById<FrameLayout>(R.id.frame).visibility = View.GONE
        }
        setWebView()
        val intentFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        registerReceiver(br, intentFilter)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if(!Config.SIDEBAR_ENABLED) {
            menuInflater.inflate(R.menu.drawer_menu, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.nav_share->{shareApp()}
            R.id.nav_more_apps->{ moreApps()}
            R.id.nav_review->{rateApp()}
            R.id.nav_allcourses->{
                val intent = Intent(this, AllcoursesActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_webmail->{
                val intent = Intent(this, UnimakMailActivity::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        mWebView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mWebView.onDestroy()
        unregisterReceiver(br)
    }

    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            requetStorageCode->{
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    downloadFile(true)
                }else{
                    downloadFile(false)
                }
            }
        }
    }

    /**
     * Show Interstitial  Add before site loaded once.
     */
    private fun showInterstitialAd(){
        if(Config.ADMOB_ENABLED){
            if(Config.INTERSENTIAL_ID != null){
                val ad = InterstitialAd(this)
                ad.adUnitId = Config.INTERSENTIAL_ID
                val adReq = AdRequest.Builder().build()
                ad.loadAd(adReq)
                ad.adListener = object : AdListener(){
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        ad.show()
                        showProgress(false)
                    }

                    override fun onAdFailedToLoad(p0: Int) {
                        super.onAdFailedToLoad(p0)
                        showProgress(false)
                    }
                }

            }
        }
    }

    /**
     * Stops the default behavior of back pressed and show confirm dialog for user
     * to select whether to exit or not.
     */
    override fun onBackPressed() {
        //super.onBackPressed()
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if(drawer != null) {
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START)
            } else {
                if (mWebView.canGoBack()) {
                    mWebView.goBack()
                } else {
                    showAlertDialogExit()
                }
            }
        }else{
            if (mWebView.canGoBack()) {
                mWebView.goBack()
            } else {
                showAlertDialogExit()
            }
        }
    }

    /**
     * show alert to confirm user wants to exit from App.
     */
    private fun showAlertDialogExit(){
        val dialog = AlertDialog.Builder(this)
            .setTitle("Confirmation")
            .setMessage("Are you sure you want to close App?")
            .setPositiveButton(R.string.yes){ _, _ ->
                this.finish()
            }
            .setNegativeButton(R.string.no){
                dialogInterface, _ -> dialogInterface.cancel()
            }
        dialog.show()
    }

    private fun setWebView(){
        mWebView.clearCache(true)
        mWebView.clearHistory()
        mWebView.settings.javaScriptEnabled = true
        mWebView.settings.javaScriptCanOpenWindowsAutomatically = true

        if(isConnected()) {
            showProgress(true)
            mWebView.loadUrl(Config.ALLCOUSS_URL)
        }else{
            mWebView.loadUrl(Config.NO_INTERNET_URL)
            showProgress(false)
        }
        setDownloadListener(mWebView)
    }

    private fun setDownloadListener(wv:AdvancedWebView){

        wv.setDownloadListener{ url, userAgent, contentDisp, mimeType, _ ->
            request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimeType)
            request.addRequestHeader("cookie",CookieManager.getInstance().getCookie(url))
            request.addRequestHeader("User-Agent",userAgent)
            request.setDescription("Downloading File...")
            request.setTitle(URLUtil.guessFileName(url, contentDisp, mimeType))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisp, mimeType))
            request.setVisibleInDownloadsUi(true)
            requestPermission()
        }
    }

    private fun requestPermission() : Boolean{
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    requetStorageCode)
            } else {
                downloadFile(true)
            }
        }else{
            downloadFile(true)
        }
        return false
    }
    /**
     * Loads and displays the provided HTML source text
     *
     * @param b boolean variable to check storage permission before downloading
     */
    private fun downloadFile(b:Boolean){
        if(b){
            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            toast("Downloading File please check DOWNLOADS Folder...")
        }else{
            toast("Permission Needed to Download File...")
        }
    }

    /**
     * Show banner adds bottom of activity
     */
    private fun setBannerAdds(){
        if(Config.ADMOB_APP_ID!=null && Config.BANNER_ID!=null && Config.INTERSENTIAL_ID!=null){
            MobileAds.initialize(this,Config.ADMOB_APP_ID)
            val adView = AdView(this)
            adView.adSize = AdSize.BANNER
            adView.adUnitId = Config.BANNER_ID
            findViewById<FrameLayout>(R.id.frame).addView(adView)
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
            adView.adListener = object : AdListener(){
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    findViewById<FrameLayout>(R.id.frame).visibility = View.VISIBLE
                    showProgress(false)
                }

                override fun onAdFailedToLoad(p0: Int) {
                    super.onAdFailedToLoad(p0)
                    findViewById<FrameLayout>(R.id.frame).visibility = View.GONE
                    showProgress(false)
                }
            }
        }
    }

    /**
     * Set Progress to display when needed.
     * @param b a boolean variable to detect whether to show or hide progress bar
     */
    fun showProgress(b:Boolean){
        if(b){
            pb.visibility = View.VISIBLE
        }else{
            pb.visibility = View.GONE
        }
    }

    /**
     * Check Network connectivity
     * @param isConnected if its connected then load the website other wise load local website
     * for offline access
     *
     */
    override fun onNetworkConnected(isConnected: Boolean?) {
        if(isConnected == true) {
            mWebView.loadUrl(Config.ALLCOUSS_URL)
            toast("Internet Connected")
        }else{
            mWebView.loadUrl(Config.NO_INTERNET_URL)
            toast("Internet disconnected")
        }

    }

    override fun onResume() {
        super.onResume()
        App.getInstance().setConnectivitylistener(this)
        mWebView.onResume()
    }

    /**
     * Sidebar Navigation Listener
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.nav_share->{shareApp()}
            R.id.nav_more_apps->{ moreApps()}
            R.id.nav_review->{rateApp()}
            R.id.nav_home->{
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_allcourses->{
                val intent = Intent(this, AllcoursesActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_webmail->{
                val intent = Intent(this, UnimakMailActivity::class.java)
                startActivity(intent)
            }
        }
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onPageFinished(url: String?) {
        if(!isShown){
            showInterstitialAd()
            isShown = true
        }
    }

    override fun onPageError(errorCode: Int, description: String?, failingUrl: String?) {

    }

    override fun onDownloadRequested(
        url: String?,
        suggestedFilename: String?,
        mimeType: String?,
        contentLength: Long,
        contentDisposition: String?,
        userAgent: String?
    ) {

    }


    override fun onExternalPageRequest(url: String?) {

    }

    /**
     * When user click any external url in the site this method will call & will handle request
     * @param view Webview to load site http & https url
     * @param url  Url of link to detect & call their respective functions.
     * Handle External Url Request to compare with following
        tel : call Default dialer to dial Number
        mailto : call Default email client to send Email
        www.youtube,m.youtube : open youtube app if installed
        mp3 : open default music player to play music mp3
        mp4,3gp : open default video player to play mp4 & 3gp videos
        http,https : open urls in current webview
     */
    override fun onExternalPageRequest(view: WebView?, url: String?): Boolean {
        if(isConnected()){
            if(url?.startsWith("tel:") == true){
                openDialer(url)
            }else if(url?.startsWith("mailto:") == true){
                openEmail(url)
            }else if(url?.contains("www.youtube")==true
                || url?.contains("m.youtube")==true){
                openYoutubeApp(url)
            }else if(url?.endsWith(".mp3") == true){
                openMediaPlayerAudio(url)
            }else if(url?.endsWith(".mp4")==true || url?.endsWith(".3gp")==true){
                openMediaPlayerVideo(url)
            }else if(url?.startsWith("http")==true || url?.startsWith("https")==true){
                view?.loadUrl(url)
            }
        }
        return true
    }

    override fun onPageStarted(url: String?, favicon: Bitmap?) {

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mWebView.onActivityResult(requestCode,resultCode,data)
    }
}
