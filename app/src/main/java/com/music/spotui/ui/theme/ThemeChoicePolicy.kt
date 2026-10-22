package com.music.spotui.ui.theme

object ThemeChoicePolicy {
    fun dark(followDevice: Boolean, manualDark: Boolean, deviceDark: Boolean) = if (followDevice) deviceDark else manualDark
}
