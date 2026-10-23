package com.music.spotui.security

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PermissionManifestTest {
    @Test fun unnecessaryNetworkMutationPermissionIsRemoved() {
        val m = File("src/main/AndroidManifest.xml").readText()
        assertTrue(m.contains("android.permission.CHANGE_NETWORK_STATE\" tools:node=\"remove\""))
        assertTrue(m.contains("android.permission.CHANGE_WIFI_STATE"))
        assertTrue(m.contains("android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE"))
        assertTrue(m.contains("android.permission.REQUEST_INSTALL_PACKAGES"))
        assertTrue(m.contains("android.permission.INTERNET"))
    }
}
