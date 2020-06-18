package com.mtechlabs.soundbeatzsalone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ConnectivityReceiver : BroadcastReceiver() {

    companion object{
        var receiver : ConnectivityReceiverListener? = null
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        receiver?.onNetworkConnected(context?.isConnected())
    }

}