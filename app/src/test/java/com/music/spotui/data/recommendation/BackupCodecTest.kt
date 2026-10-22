package com.music.spotui.data.recommendation

import com.music.spotui.data.backup.UserBackup
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class BackupCodecTest {
    private fun encoded(value: Any): JSONObject = UserBackup.javaClass.getDeclaredMethod("encode", Any::class.java).apply { isAccessible = true }.invoke(UserBackup, value) as JSONObject
    private fun decoded(value: JSONObject): Any = UserBackup.javaClass.getDeclaredMethod("decode", JSONObject::class.java).apply { isAccessible = true }.invoke(UserBackup, value)
    @Test fun integerTypesSurviveWithoutPrecisionLoss() {
        assertEquals(Long.MAX_VALUE, decoded(encoded(Long.MAX_VALUE)))
        assertEquals(Int.MIN_VALUE, decoded(encoded(Int.MIN_VALUE)))
    }
    @Test fun ordinarySettingsKeepTheirTypes() {
        assertEquals(true, decoded(encoded(true)))
        assertEquals(0.25f, decoded(encoded(0.25f)))
        assertEquals("☀ music", decoded(encoded("☀ music")))
    }
    @Test fun stringSetsRemainSets() {
        val values = setOf("one", "two", "three")
        assertEquals(values, decoded(encoded(values)))
    }
    @Test fun malformedPreferenceTypesAreRejected() {
        assertThrows(java.lang.reflect.InvocationTargetException::class.java) { decoded(JSONObject().put("type", "unknown").put("value", "bad")) }
    }
}
