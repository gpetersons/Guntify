package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.HomeItem
import com.music.spotui.data.entity.HomeSection
import com.music.spotui.data.preferences.HomeHiddenItems

/** Preserve shelf titles/purposes, spread artists, and avoid repeating the same card. */
object HomePolishPolicy {
    fun refine(sections:List<HomeSection>):List<HomeSection> {
        val shown=mutableListOf<HomeItem>()
        val keys=mutableSetOf<String>()
        val known=HomeDiversityPolicy.knownArtists(sections.flatMap { it.items })
        return sections.mapNotNull { section ->
            val fresh=section.items.filter { HomeHiddenItems.key(it) !in keys }
            val choices=HomeDiversityPolicy.spread(fresh,fresh.size,known,shown)
            if(choices.isEmpty())null else section.copy(items=choices).also {shown+=choices.take(6);keys+=choices.take(6).map(HomeHiddenItems::key)}
        }.take(4)
    }
}
