package com.music.spotui.updates

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class UpdatePolicyTest {
    private fun release(code: Int = 76) = JSONObject().put("tag_name", "v1.1-build-$code")
        .put("html_url", "https://github.com/gpetersons/Guntify/releases/tag/v1.1-build-$code")
        .put("draft", false).put("prerelease", false).put("body", "What's new")
        .put("assets", JSONArray().put(JSONObject().put("name", "Guntify.apk").put("state", "uploaded")
            .put("size", 30_000_000).put("digest", "sha256:" + "a".repeat(64))
            .put("browser_download_url", "https://github.com/gpetersons/Guntify/releases/download/v1.1-build-$code/Guntify.apk")))
    @Test fun sameDisplayVersionCanHaveNewerBuild() { val r=UpdatePolicy.parse(release().toString())!!;assertEquals(76L,r.code);assertEquals("1.1",r.version) }
    @Test fun buildNumbersAreNumeric() { assertEquals(100L,UpdatePolicy.parse(release(100).toString())!!.code) }
    @Test fun draftsAndPrereleasesAreNeverOffered() {
        assertNull(UpdatePolicy.parse(release().put("draft",true).toString()))
        assertNull(UpdatePolicy.parse(release().put("prerelease",true).toString()))
    }
    private fun rejected(j: JSONObject) { try { UpdatePolicy.parse(j.toString());fail("Untrusted release accepted") } catch (_: IllegalArgumentException) {} catch (_: IllegalStateException) {} }
    @Test fun wrongRepositoryIsRejected() { rejected(release().put("html_url","https://github.com/other/Guntify/releases/tag/v1.1-build-76")) }
    @Test fun missingOrAmbiguousApkIsRejected() { rejected(release().put("assets",JSONArray()));val r=release();r.getJSONArray("assets").put(r.getJSONArray("assets").getJSONObject(0));rejected(r) }
    @Test fun previewApkIsNotAProductionUpdate() { val r=release();r.getJSONArray("assets").getJSONObject(0).put("name","Guntify-Desktop-Preview.apk");rejected(r) }
    @Test fun arbitraryDownloadRedirectAndInsecureUrlAreRejected() { for(url in listOf("http://github.com/gpetersons/Guntify/releases/download/v1.1-build-76/Guntify.apk","https://evil.invalid/Guntify.apk","https://github.com.evil.invalid/gpetersons/Guntify/releases/download/v1.1-build-76/Guntify.apk")){val r=release();r.getJSONArray("assets").getJSONObject(0).put("browser_download_url",url);rejected(r)} }
    @Test fun checksumIsRequired() { val r=release();r.getJSONArray("assets").getJSONObject(0).remove("digest");rejected(r) }
    @Test fun oversizedAssetIsRejected() { val r=release();r.getJSONArray("assets").getJSONObject(0).put("size",UpdatePolicy.MAX_APK_BYTES+1);rejected(r) }
    @Test fun unknownTagCannotMasqueradeAsCurrent() { rejected(release().put("tag_name","latest")) }
    @Test fun samePackageAndSignerWithHigherCodeCanUpdate() { assertTrue(UpdatePolicy.compatible("app","app",75,76,76,setOf("key"),setOf("key"))) }
    @Test fun differentPackageOrKeyIsRejected() { assertFalse(UpdatePolicy.compatible("app","preview",75,76,76,setOf("key"),setOf("key")));assertFalse(UpdatePolicy.compatible("app","app",75,76,76,setOf("key"),setOf("other"))) }
    @Test fun oldOrMislabeledApkIsRejected() { assertFalse(UpdatePolicy.compatible("app","app",75,75,75,setOf("key"),setOf("key")));assertFalse(UpdatePolicy.compatible("app","app",75,76,77,setOf("key"),setOf("key"))) }
    @Test fun missingCertificatesNeverPass() { assertFalse(UpdatePolicy.compatible("app","app",75,76,76,emptySet(),emptySet())) }
}
