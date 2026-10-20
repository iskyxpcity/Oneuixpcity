package com.oneuialoha.app

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.UserManager
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.oneuialoha.app.data.repository.SettingsRepositoryImpl
import com.oneuialoha.app.oneuixp.OneUIAlohaPreferenceBridge
import com.oneuialoha.app.statusbar.LsposedFrameworkStatus
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.util.Locale
import org.lsposed.hiddenapibypass.HiddenApiBypass

lateinit var oneUIAlohaApp: OneUIAlohaApplication

class OneUIAlohaApplication : Application(), ViewModelStoreOwner {

    companion object {
        fun setEnableOnBackInvokedCallback(appInfo: ApplicationInfo, enable: Boolean): Boolean =
            runCatching {
                val method = ApplicationInfo::class.java.getDeclaredMethod(
                    "setEnableOnBackInvokedCallback",
                    Boolean::class.javaPrimitiveType,
                )
                method.isAccessible = true
                method.invoke(appInfo, enable)
                true
            }.getOrDefault(false)

        /** Apply the preference to both contexts used to create a recreated Activity. */
        fun applyEnableOnBackInvokedCallback(context: Context, enable: Boolean): Boolean {
            runCatching {
                HiddenApiBypass.addHiddenApiExemptions("Landroid/content/pm/ApplicationInfo;")
            }
            val contextApplied = setEnableOnBackInvokedCallback(context.applicationInfo, enable)
            val appInfo = (context.applicationContext as? OneUIAlohaApplication)?.applicationInfo
            val applicationApplied = appInfo?.let { setEnableOnBackInvokedCallback(it, enable) } ?: false
            return contextApplied || applicationApplied
        }
    }

    lateinit var okhttpClient: OkHttpClient
    private val appViewModelStore by lazy { ViewModelStore() }

    private fun isUserUnlocked(): Boolean =
        getSystemService(UserManager::class.java)?.isUserUnlocked == true

    override fun onCreate() {
        super.onCreate()
        oneUIAlohaApp = this
        LsposedFrameworkStatus.initialize()
        OneUIAlohaPreferenceBridge.initialize(this)

        if (!isUserUnlocked()) {
            return
        }

        OneUIAlohaPreferenceBridge.ensureSynchronized(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            applyEnableOnBackInvokedCallback(this, SettingsRepositoryImpl().enablePredictiveBack)
        }

        okhttpClient =
            OkHttpClient.Builder().cache(Cache(java.io.File(cacheDir, "okhttp"), 10 * 1024 * 1024))
                .addInterceptor { block ->
                    block.proceed(
                        block.request().newBuilder()
                            .header("User-Agent", "OneUIAloha/${BuildConfig.VERSION_CODE}")
                            .header("Accept-Language", Locale.getDefault().toLanguageTag()).build()
                    )
                }.build()
    }

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore
}
