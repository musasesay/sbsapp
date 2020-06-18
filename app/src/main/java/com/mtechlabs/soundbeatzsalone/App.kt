package com.mtechlabs.soundbeatzsalone

import android.app.Application
import com.onesignal.OneSignal

class App : Application(){

    companion object{
        var app : App? = null
        fun getInstance():App{
            return app ?: App()
        }
    }

    override fun onCreate() {
        super.onCreate()
        app = this
        OneSignal.startInit(this)
            .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
            .unsubscribeWhenNotificationsAreDisabled(true)
            .init()
    }



    fun setConnectivitylistener(conn:ConnectivityReceiverListener){
        ConnectivityReceiver.receiver = conn
    }
}