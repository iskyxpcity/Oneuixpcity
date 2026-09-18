package me.weishu.kernelsu

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import me.weishu.kernelsu.Natives.Profile.RootProfileFlag

/**
 * @author weishu
 * @date 2022/12/8.
 */
object Natives {
    // minimal supported kernel version
    // 10915: allowlist breaking change, add app profile
    // 10931: app profile struct add 'version' field
    // 10946: add capabilities
    // 10977: change groups_count and groups to avoid overflow write
    // 11071: Fix the issue of failing to set a custom SELinux type.
    // 12143: breaking: new supercall impl
    // 32310: new get_allow_list ioctl
    // 32336: new set_sepolicy ioctl
    // 32377: add set_init_pgrp ioctl
    // 32513: add uapi version
    const val MINIMAL_SUPPORTED_KERNEL = 32513

    const val KERNEL_SU_DOMAIN = "u:r:ksu:s0"

    const val ROOT_UID = 0
    const val ROOT_GID = 0

    // Legacy API compatibility only. OneuiXP does not load or ship KernelSU JNI.
    // Never report successful root operations when the backend is absent.
    val version: Int get() = -1
    val isSafeMode: Boolean get() = false
    val isLkmMode: Boolean get() = false
    val isLateLoadMode: Boolean get() = false
    val isManager: Boolean get() = false
    val isPrBuild: Boolean get() = false

    fun uidShouldUmount(uid: Int): Boolean = false
    fun getAppProfile(key: String?, uid: Int): Profile = Profile(key ?: "", uid)
    fun setAppProfile(profile: Profile?): Boolean = false
    fun isSuEnabled(): Boolean = false
    fun setSuEnabled(enabled: Boolean): Boolean = false
    fun isKernelUmountEnabled(): Boolean = false
    fun setKernelUmountEnabled(enabled: Boolean): Boolean = false
    fun isWebViewZygoteUmountEnabled(): Boolean = false
    fun setWebViewZygoteUmountEnabled(enabled: Boolean): Boolean = false
    fun isSelinuxHideEnabled(): Boolean = false
    fun setSelinuxHideEnabled(enabled: Boolean): Int = -1
    fun getUserName(uid: Int): String? = null
    fun getSuperuserCount(): Int = 0
    fun setDefaultUmountModules(umountModules: Boolean): Boolean = false
    fun isDefaultUmountModules(): Boolean = false
    val kernelUAPIVersion: Int get() = -1
    val managerUAPIVersion: Int get() = -1
    fun checkUAPIMismatch(): Boolean = false
    fun requireNewKernel(): Boolean = false

    @Keep
    @Immutable
    @Parcelize
    @Serializable
    data class Profile(
        // and there is a default profile for root and non-root
        val name: String,
        // current uid for the package, this is convivent for kernel to check
        // if the package name doesn't match uid, then it should be invalidated.
        val currentUid: Int = 0,

        // if this is true, kernel will grant root permission to this package
        val allowSu: Boolean = false,

        // these are used for root profile
        val rootUseDefault: Boolean = true,
        val rootTemplate: String? = null,
        val uid: Int = ROOT_UID,
        val gid: Int = ROOT_GID,
        val groups: List<Int> = mutableListOf(),
        val capabilities: List<Int> = mutableListOf(),
        val context: String = KERNEL_SU_DOMAIN,
        val namespace: Int = Namespace.INHERITED.ordinal,

        val nonRootUseDefault: Boolean = true,
        val umountModules: Boolean = true,
        var rules: String = "", // this field is save in ksud!!

        val flags: Long = FLAG_KSU_NO_NEW_PRIVS,
    ) : Parcelable {
        @Keep
        enum class RootProfileFlag(val display: String, val desc: Int) {
            NO_NEW_PRIVS(
                "NO_NEW_PRIVS",
                R.string.profile_flags_desc_no_new_privs
            )
        }

        enum class Namespace {
            INHERITED,
            GLOBAL,
            INDIVIDUAL,
        }

        constructor() : this("")
    }

    const val FLAG_KSU_NO_NEW_PRIVS = 1L
}

fun List<RootProfileFlag>.toRawFlags(): Long =
    fold(0L) { acc, flag -> acc.or(1L.shl(flag.ordinal)) }

fun List<RootProfileFlag>.toOrdinalList(): List<Int> =
    map { it.ordinal }

fun Long.toRootProfileFlags(): List<RootProfileFlag> =
    RootProfileFlag.entries.filter { 1L.shl(it.ordinal).and(this) != 0L }.toList()
