package com.jarvanmo.rammus

import android.os.Bundle
import android.util.Log
import com.huawei.hms.push.HmsMessageService

class RammusHmsMessageService : HmsMessageService(){

    override fun onNewToken(token: String?, bundle: Bundle?) {
        Log.i("RammusHmsMessageService", "onSubjectToken called, token: $token ")
    }

    override fun onTokenError(e: Exception, bundle: Bundle) {
        val errCode = e.hashCode()
        val errInfo = e.message
        Log.i("RammusHmsMessageService", "onTokenError called, errCode: $errCode ,errInfo: $errInfo ")
    }

}