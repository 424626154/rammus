package com.jarvanmo.rammus

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.net.Uri
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.util.Log
import com.alibaba.sdk.android.push.CommonCallback
import com.alibaba.sdk.android.push.huawei.HuaWeiRegister
import com.alibaba.sdk.android.push.noonesdk.PushServiceFactory
import com.alibaba.sdk.android.push.register.*
import com.heytap.msp.push.HeytapPushManager
import com.huawei.hms.aaid.HmsInstanceId
import com.xiaomi.mipush.sdk.MiPushClient
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar


class RammusPlugin : FlutterPlugin, MethodCallHandler {

    private var applicationContext: Context? = null
    private var methodChannel: MethodChannel? = null

    companion object {
        private const val TAG = "RammusPlugin"
        private val inHandler = Handler()
        private var gottenApplication : Application? = null
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val instance = RammusPlugin()
            instance.onAttachedToEngine(registrar.context(), registrar.messenger())
        }
        @JvmStatic
        fun initPushService(application: Application){
//            createNotificationChannel(application);
            gottenApplication = application
            PushServiceFactory.init(application.applicationContext)
            val pushService = PushServiceFactory.getCloudPushService()
            pushService.setPushIntentService(RammusPushIntentService::class.java)
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "register" -> register()
            "deviceId" -> result.success(PushServiceFactory.getCloudPushService().deviceId)
            "turnOnPushChannel" -> turnOnPushChannel(result)
            "turnOffPushChannel" -> turnOffPushChannel(result)
            "checkPushChannelStatus" -> checkPushChannelStatus(result)
            "bindAccount" -> bindAccount(call, result)
            "unbindAccount" -> unbindAccount(result)
            "bindTag" -> bindTag(call, result)
            "unbindTag" -> unbindTag(call, result)
            "listTags" -> listTags(call, result)
            "addAlias" -> addAlias(call, result)
            "removeAlias" -> removeAlias(call, result)
            "listAliases" -> listAliases(result)
            "setupNotificationManager" -> setupNotificationManager(call, result)
            "bindPhoneNumber" -> bindPhoneNumber(call, result)
            "unbindPhoneNumber" -> unbindPhoneNumber(result)
            "applicationBadgeNumberClean" ->setApplicationBadgeNumber(call)
            else -> result.notImplemented()
        }

    }

    private fun register() {
        if (gottenApplication == null) {
            Log.w(TAG, "注册推送服务失败，请检查是否在运行本语句前执行了`RammusPlugin.initPushService`.")
            return;
        }
        var application = gottenApplication!!
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.register(application.applicationContext, object : CommonCallback {
            override fun onSuccess(response: String?) {
                inHandler.postDelayed({
                    RammusPushHandler.methodChannel?.invokeMethod(
                        "initCloudChannelResult", mapOf(
                            "isSuccessful" to true,
                            "response" to response
                        )
                    )
                }, 2000)
            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                inHandler.postDelayed({
                    RammusPushHandler.methodChannel?.invokeMethod(
                        "initCloudChannelResult", mapOf(
                            "isSuccessful" to false,
                            "errorCode" to errorCode,
                            "errorMessage" to errorMessage
                        )
                    )
                }, 2000)
            }
        })
//            pushService.setPushIntentService(RammusPushIntentService::class.java)
        val appInfo = application.packageManager
            .getApplicationInfo(application.packageName, PackageManager.GET_META_DATA)
        val xiaomiAppId =
            appInfo.metaData.getString("com.xiaomi.push.client.app_id")?.removePrefix("xiaomi_")
        val xiaomiAppKey = appInfo.metaData.getString("com.xiaomi.push.client.app_key")
            ?.removePrefix("xiaomi_")
        if ((xiaomiAppId != null && xiaomiAppId.isNotBlank())
            && (xiaomiAppKey != null && xiaomiAppKey.isNotBlank())
        ) {
            Log.d(TAG, "正在注册小米推送服务...")
            MiPushRegister.register(
                application.applicationContext,
                xiaomiAppId,
                xiaomiAppKey
            )
        }
        val huaweiAppId = appInfo.metaData.get("com.huawei.hms.client.appid")
        if (huaweiAppId != null && huaweiAppId.toString().isNotBlank()) {
            Log.d(TAG, "正在注册华为推送服务...")
            HuaWeiRegister.register(application)
        }
        val oppoAppKey =
            appInfo.metaData.getString("com.oppo.push.client.app_key")?.removePrefix("oppo_")
        val oppoAppSecret =
            appInfo.metaData.getString("com.oppo.push.client.app_secret")?.removePrefix("oppo_")
        if ((oppoAppKey != null && oppoAppKey.isNotBlank())
            && (oppoAppSecret != null && oppoAppSecret.isNotBlank())
        ) {
            Log.d(TAG, "正在注册Oppo推送服务...")
            OppoRegister.register(
                application.applicationContext,
                oppoAppKey,
                oppoAppSecret
            )
        }
        val meizuAppId =
            appInfo.metaData.getString("com.meizu.push.client.app_id")?.removePrefix("meizu_")
        val meizuAppKey =
            appInfo.metaData.getString("com.meizu.push.client.app_key")?.removePrefix("meizu_")
        if ((meizuAppId != null && meizuAppId.isNotBlank())
            && (meizuAppKey != null && meizuAppKey.isNotBlank())
        ) {
            Log.d(TAG, "正在注册魅族推送服务...")
            MeizuRegister.register(
                application.applicationContext,
                meizuAppId,
                meizuAppKey
            )
        }
        val vivoAppId = appInfo.metaData.get("com.vivo.push.app_id")
        val vivoApiKey = appInfo.metaData.get("com.vivo.push.api_key")
        if ((vivoAppId != null && vivoAppId.toString().isNotBlank())
            && (vivoApiKey != null && vivoApiKey.toString().isNotBlank())
        ) {
            Log.d(TAG, "正在注册Vivo推送服务...")
            VivoRegister.register(application.applicationContext)
        }
        val gcmSendId = appInfo.metaData.getString("com.gcm.push.send_id")?.removePrefix("gcm_")
        val gcmApplicationId =
            appInfo.metaData.getString("com.gcm.push.app_id")?.removePrefix("gcm_")
        val gcmProjectId = appInfo.metaData.getString("com.gcm.push.project_id")?.removePrefix("gcm_")
        val gcmApiKey = appInfo.metaData.getString("com.gcm.push.api_key")?.removePrefix("gcm_")
        if ((gcmSendId != null && gcmSendId.isNotBlank())
            && (gcmApplicationId != null && gcmApplicationId.isNotBlank())
        ) {
            Log.d(TAG, "正在注册Gcm推送服务...")
            GcmRegister.register(
                application.applicationContext,
                gcmSendId,
                gcmApplicationId,
                gcmProjectId,
                gcmApiKey
            )
        }
    }


    private fun turnOnPushChannel(result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.turnOnPushChannel(object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to true,
                        "response" to response
                    )
                )

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                    )
                )
            }
        })
    }

    private fun turnOffPushChannel(result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.turnOffPushChannel(object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to true,
                        "response" to response
                    )
                )

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                    )
                )
            }
        })
    }


    private fun checkPushChannelStatus(result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.checkPushChannelStatus(object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to true,
                        "response" to response
                    )
                )

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                    )
                )
            }
        })
    }


    private fun bindAccount(call: MethodCall, result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.bindAccount(call.arguments as String?, object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to true,
                        "response" to response
                    )
                )

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                    )
                )
            }
        })
    }


    private fun unbindAccount(result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.unbindAccount(object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to true,
                        "response" to response
                    )
                )

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                    )
                )
            }
        })
    }

    //bindPhoneNumber


    private fun bindPhoneNumber(call: MethodCall, result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.bindPhoneNumber(call.arguments as String?, object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to true,
                        "response" to response
                    )
                )

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                    )
                )
            }
        })
    }


    private fun unbindPhoneNumber(result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.unbindPhoneNumber(object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to true,
                        "response" to response
                    )
                )

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                    )
                )
            }
        })
    }


    private fun bindTag(call: MethodCall, result: Result) {
//        target: Int, tags: Array<String>, alias: String, callback: CommonCallback
        val target = call.argument("target") ?: 1
        val tagsInArrayList = call.argument("tags") ?: arrayListOf<String>()
        val alias = call.argument<String?>("alias")

        val arr = arrayOfNulls<String>(tagsInArrayList.size)
        val tags: Array<String> = tagsInArrayList.toArray(arr)

        val pushService = PushServiceFactory.getCloudPushService()

        pushService.bindTag(target, tags, alias, object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to true,
                        "response" to response
                    )
                )

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                    )
                )
            }
        })
    }


    private fun unbindTag(call: MethodCall, result: Result) {
//        target: Int, tags: Array<String>, alias: String, callback: CommonCallback
        val target = call.argument("target") ?: 1
        val tagsInArrayList = call.argument("tags") ?: arrayListOf<String>()
        val alias = call.argument<String?>("alias")

        val arr = arrayOfNulls<String>(tagsInArrayList.size)
        val tags: Array<String> = tagsInArrayList.toArray(arr)

        val pushService = PushServiceFactory.getCloudPushService()

        pushService.unbindTag(target, tags, alias, object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to true,
                        "response" to response
                    )
                )

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                    )
                )
            }
        })
    }

    private fun listTags(call: MethodCall, result: Result) {
        val target = call.arguments as Int? ?: 1
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.listTags(target, object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to true,
                        "response" to response
                    )
                )

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                    )
                )
            }
        })
    }


    private fun addAlias(call: MethodCall, result: Result) {
        val alias = call.arguments as String?
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.addAlias(alias, object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to true,
                        "response" to response
                    )
                )

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                    )
                )
            }
        })
    }

    private fun removeAlias(call: MethodCall, result: Result) {
        val alias = call.arguments as String?
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.removeAlias(alias, object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to true,
                        "response" to response
                    )
                )

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to true,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                    )
                )
            }
        })
    }

    private fun listAliases(result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.listAliases(object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to true,
                        "response" to response
                    )
                )

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(
                    mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                    )
                )
            }
        })
    }


    private fun setupNotificationManager(call: MethodCall, result: Result) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = call.arguments as List<Map<String, Any?>>
            val mNotificationManager =
                applicationContext?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationChannels = mutableListOf<NotificationChannel>()
            for (channel in channels) {
                // 通知渠道的id
                val id = channel["id"] ?: applicationContext?.packageName
                // 用户可以看到的通知渠道的名字.
                val name = channel["name"] ?: applicationContext?.packageName
                // 用户可以看到的通知渠道的描述
                val description = channel["description"] ?: applicationContext?.packageName
                val importance = channel["importance"] ?: NotificationManager.IMPORTANCE_DEFAULT
                val mChannel = NotificationChannel(id as String, name as String, importance as Int)
                // 配置通知渠道的属性
                mChannel.description = description as String
                mChannel.enableLights(true)
                mChannel.enableVibration(true)
                notificationChannels.add(mChannel)
            }
            if (notificationChannels.isNotEmpty()) {
                mNotificationManager.createNotificationChannels(notificationChannels)
            }
        }
        result.success(true)
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        onAttachedToEngine(binding.applicationContext, binding.binaryMessenger)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = null
        methodChannel?.setMethodCallHandler(null);
        methodChannel = null;
        RammusPushHandler.methodChannel = null;
    }

    private fun onAttachedToEngine(applicationContext: Context, messenger: BinaryMessenger) {
        this.applicationContext = applicationContext
        methodChannel = MethodChannel(messenger, "com.jarvanmo/rammus")
        if (methodChannel != null) methodChannel?.setMethodCallHandler(this)
        RammusPushHandler.methodChannel = methodChannel;
    }

    private fun getHuaweiToken(call: MethodCall, result: Result) {
        var huaweiAppId = call.argument<String?>("huaweiAppId")
        var isHuaweiDevice = Build.BRAND.equals(
            "huawei",
            ignoreCase = true
        ) || Build.BRAND.equals("honor", ignoreCase = true)
        Log.d(TAG, "isHuaweiDevice:$isHuaweiDevice")
        if (isHuaweiDevice) {
            Thread {
                Log.d(TAG, "huaweiAppId:$huaweiAppId")
                try {
                    var huaweiToken = HmsInstanceId.getInstance(applicationContext)
                        .getToken(huaweiAppId, "HCM");
                    Log.d(TAG, "huaweiToken:$huaweiToken")
                    result.success(huaweiToken)
                } catch (e: Exception) {
                    result.error("-1", e.message, e)
                }
            }.start()
        } else {
            result.success("")
        }
    }

    private fun getXiaomiRegId(result: Result) {
        if (Build.BRAND.equals(
                "xiaomi",
                ignoreCase = true
            ) || Build.BRAND.equals("redmi", ignoreCase = true) || Build.BRAND.equals(
                "blackshark",
                ignoreCase = true
            )
        ) {
            var regId = MiPushClient.getRegId(applicationContext)
            result.success(regId)
        } else {
            result.success("")
        }

    }
    //获得vivo regId
    private fun getVivoRegId(result: Result) {
        if (Build.BRAND.equals(
                "vivo",
                ignoreCase = true
            )
        ) {
            var regId = com.vivo.push.PushClient.getInstance(applicationContext).regId
            result.success(regId)
        }else{
            result.success("")
        }
    }
    //获得oppo regId
    private fun getOppoRegId(result: Result) {
        if (Build.BRAND.equals(
                "OPPO",
                ignoreCase = true
            )
        ) {
            var regId = HeytapPushManager.getRegisterID()
            result.success(regId)
        }else{
            result.success("")
        }
    }

    //设置android角标
    private fun setApplicationBadgeNumber(call: MethodCall){
        val appInfo = gottenApplication!!.packageManager
            .getApplicationInfo(gottenApplication!!.packageName, PackageManager.GET_META_DATA)

        var num = call.argument("num") as Int? ?: 0
        setHuaWeiApplicationBadgeNumber(num,appInfo)

    }

    //清理华为角标 https://developer.huawei.com/consumer/cn/doc/development/Corner-Guides/30802
    private fun setHuaWeiApplicationBadgeNumber(num: Int,appInfo: ApplicationInfo) {
        val huaweiAppId = appInfo.metaData.getString("com.huawei.hms.client.appid")
        if (huaweiAppId != null && huaweiAppId.toString().isNotBlank()){
            try {
                val bundle = Bundle()
                bundle.putString("package", applicationContext?.packageName) // com.test.badge is your package name
                bundle.putString("class", applicationContext?.packageName+".MainActivity") // com.test. badge.MainActivity is your apk main activity
                bundle.putInt("badgenumber", num)
                gottenApplication!!.contentResolver.call(Uri.parse("content://com.huawei.android.launcher.settings/badge/"), "change_badge", null, bundle)
            } catch (e: Exception) {
                Log.w(TAG, "setHuaWeiApplicationBadgeNumberClean: 失败"+e.message)
            }
        }

    }
}
