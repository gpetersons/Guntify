package com.music.spotui.security
import org.junit.Assert.*
import org.junit.Test
import java.io.File
class CiHardeningTest {
    @Test fun workflowActionsUseImmutableCommits() {
        listOf("guntify-build.yml","guntify-release.yml").forEach { file ->
            val yaml = File("../.github/workflows/$file").readText()
            val actions = Regex("uses: ([^\\s]+)").findAll(yaml).map { it.groupValues[1] }.toList()
            assertTrue(actions.isNotEmpty())
            actions.forEach { action -> assertTrue(action, action.matches(Regex("[^@]+@[0-9a-f]{40}"))) }
            assertTrue(yaml.contains("npm ci --no-fund"))
        }
    }
}
