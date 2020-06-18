package com.mtechlabs.soundbeatzsalone

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.widget.Toast
import java.util.regex.Pattern

fun Context.toast(msg:String){
    Toast.makeText(this,msg,Toast.LENGTH_LONG).show()
}

fun Context.isConnected() : Boolean{
    val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val nw      = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    } else {
        val nwInfo = connectivityManager.activeNetworkInfo ?: return false
        return nwInfo.isConnected
    }
}

fun Context.rateApp(){
    val appName = this.packageName
    try {
        this.startActivity(
            Intent(
                Intent.ACTION_VIEW, Uri.parse(
                    "market://details?id=$appName"
                )
            )
        )
    }catch (exception : ActivityNotFoundException){
        this.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(
            "http://play.google.com/store/apps/details?id=$appName")))
    }
}

fun Context.moreApps(){
    val appName = this.packageName
    try {
        this.startActivity(
            Intent(
                Intent.ACTION_VIEW, Uri.parse(
                    "market://details?id=$appName"
                )
            )
        )
    }catch (exception : ActivityNotFoundException){
        this.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(
            "http://play.google.com/store/apps/details?id=$appName")))
    }
}

fun Context.openDialer(url : String?) = this.startActivity(Intent(
    Intent.ACTION_DIAL,Uri.parse(url)))

fun Context.openEmail(url:String?) = this.startActivity(Intent(
    Intent.ACTION_SENDTO,Uri.parse(url)))

fun Context.openYoutubeApp(url:String?) : Boolean{
    val id = Utils.getYouTubeVideoId(url)
    val intent = Intent(Intent.ACTION_VIEW,Uri.parse("vnd.youtube:$id"))
    return try{
        this.startActivity(intent)
        true
    }catch (exc:ActivityNotFoundException){
        false
    }
}

fun Context.openMediaPlayerAudio(url:String){
    this.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(url),
        "audio/*"))
}

fun Context.openMediaPlayerVideo(url:String){
    this.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(url),
        "video/*"))
}

fun Context.shareApp(){
    this.startActivity(Intent()
        .setAction(Intent.ACTION_SEND)
        .putExtra(Intent.EXTRA_TEXT,this.getString(R.string.shareMsg)+this.packageName)
        .setType("text/plain"))
}

class Utils {
    companion object{
        fun getYouTubeVideoId(url:String?) : String?{
            val pattern = "(?<=watch\\\\?v=|/videos/|embed\\\\/|youtu.be\\\\/|\\\\/v\\\\/|\\\\/e\\\\/|watch\\\\?v%3D|watch\\\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\\u200C\\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\\\&\\\\?\\\\n]*"
            val compilePattern = Pattern.compile(pattern)
            val matcher = compilePattern.matcher(url)
            if(matcher.find()){
                return matcher.group()
            }
            return null
        }
    }
}