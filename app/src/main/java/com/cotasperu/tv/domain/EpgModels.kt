package com.cotasperu.tv.domain

data class EpgProgram(
    val id: String?,
    val title: String,
    val description: String?,
    val startAt: String?,
    val endAt: String?,
    val startLabel: String?,
    val endLabel: String?,
)

data class ChannelEpgGuide(
    val streamId: String,
    val generatedAt: String?,
    val now: EpgProgram?,
    val next: EpgProgram?,
    val items: List<EpgProgram>,
)

enum class EpgHitStatus {
    LIVE,
    UPCOMING,
    ENDED,
}

data class EpgSearchHit(
    val programId: String?,
    val title: String,
    val description: String?,
    val streamId: String?,
    val channelName: String?,
    val startAt: String?,
    val endAt: String?,
    val startLocal: String?,
    val endLocal: String?,
    val status: EpgHitStatus,
    val statusLabel: String,
)

data class EpgSearchPage(
    val page: Int,
    val total: Int,
    val totalPages: Int,
    val query: String,
    val items: List<EpgSearchHit>,
)
