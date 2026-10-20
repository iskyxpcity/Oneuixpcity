package com.oneuialoha.common

import android.os.SystemProperties

val ONE_UI_VERSION = SystemProperties.getInt("ro.build.version.oneui", 0)
