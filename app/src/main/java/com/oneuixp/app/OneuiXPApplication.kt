package com.oneuixp.app

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.UserManager
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.oneuixp.app.data.repository.SettingsRepositoryImpl
import com.oneuixp.app.oneuixp.OneuiXPreferenceBridge
import com.oneuixp.app.statusbar.LsposedFrameworkStatus
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.util.Locale
import org.lsposed.hiddenapibypass.HiddenApiBypass

lateinit var oneuiXPApp: OneuiXPApplication

class OneuiXPApplication : Application(), ViewModelStoreOwner {

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
            val appInfo = (context.applicationContext as? OneuiXPApplication)?.applicationInfo
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
        oneuiXPApp = this
        LsposedFrameworkStatus.initialize()
        OneuiXPreferenceBridge.initialize(this)

        if (!isUserUnlocked()) {
            return
        }

        OneuiXPreferenceBridge.ensureSynchronized(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            applyEnableOnBackInvokedCallback(this, SettingsRepositoryImpl().enablePredictiveBack)
        }

        okhttpClient =
            OkHttpClient.Builder().cache(Cache(java.io.File(cacheDir, "okhttp"), 10 * 1024 * 1024))
                .addInterceptor { block ->
                    block.proceed(
                        block.request().newBuilder()
                            .header("User-Agent", "OneuiXP/${BuildConfig.VERSION_CODE}")
                            .header("Accept-Language", Locale.getDefault().toLanguageTag()).build()
                    )
                }.build()
    }

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore
}
