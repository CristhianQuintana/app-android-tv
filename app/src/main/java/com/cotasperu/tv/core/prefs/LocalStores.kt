package com.cotasperu.tv.core.prefs

import android.content.Context
import com.cotasperu.tv.domain.CatalogEntry
import com.cotasperu.tv.domain.CatalogKind
import org.json.JSONArray
import org.json.JSONObject

class PrefsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val appContext = context.applicationContext

    var keepSession: Boolean
        get() = prefs.getBoolean(KEY_KEEP, true)
        set(value) = prefs.edit().putBoolean(KEY_KEEP, value).apply()

    /** UUID estable del televisor (device-link + login). */
    fun deviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)?.trim().orEmpty()
        if (existing.isNotEmpty()) return existing
        val androidId = android.provider.Settings.Secure.getString(
            appContext.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID,
        )
        val created = if (!androidId.isNullOrBlank()) {
            java.util.UUID.nameUUIDFromBytes("cotas:$androidId".toByteArray(Charsets.UTF_8)).toString()
        } else {
            java.util.UUID.randomUUID().toString()
        }

        prefs.edit().putString(KEY_DEVICE_ID, created).apply()
        return created
    }

    var deviceName: String
        get() = prefs.getString(KEY_DEVICE_NAME, null)?.trim()?.takeIf { it.isNotEmpty() }
            ?: DEFAULT_DEVICE_NAME
        set(value) {
            val name = value.trim().take(64).ifEmpty { DEFAULT_DEVICE_NAME }
            prefs.edit().putString(KEY_DEVICE_NAME, name).apply()
        }

    private companion object {
        const val FILE = "cotas_prefs_v1"
        const val KEY_KEEP = "keep_session"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_DEVICE_NAME = "device_name"
        const val DEFAULT_DEVICE_NAME = "Google TV"
    }
}

data class AppAccount(
    val email: String,
    val name: String,
    val accessToken: String,
    val refreshToken: String?,
    val since: Long,
)

class AppAccountStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun read(): AppAccount? {
        val access = prefs.getString(KEY_ACCESS, null) ?: return null
        if (access.isBlank()) return null
        return AppAccount(
            email = prefs.getString(KEY_EMAIL, null).orEmpty(),
            name = prefs.getString(KEY_NAME, null).orEmpty(),
            accessToken = access,
            refreshToken = prefs.getString(KEY_REFRESH, null),
            since = prefs.getLong(KEY_SINCE, 0L),
        )
    }

    fun write(account: AppAccount) {
        prefs.edit()
            .putString(KEY_EMAIL, account.email)
            .putString(KEY_NAME, account.name)
            .putString(KEY_ACCESS, account.accessToken)
            .putString(KEY_REFRESH, account.refreshToken)
            .putLong(KEY_SINCE, account.since)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val FILE = "cotas.google.v1"
        const val KEY_EMAIL = "email"
        const val KEY_NAME = "name"
        const val KEY_ACCESS = "accessToken"
        const val KEY_REFRESH = "refreshToken"
        const val KEY_SINCE = "since"
    }
}

data class FavoriteItem(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val categoryId: String?,
    val kind: String,
    val savedAt: Long,
) {
    fun toEntry(): CatalogEntry = CatalogEntry(
        id = id,
        title = title,
        imageUrl = imageUrl,
        categoryId = categoryId,
    )
}

class FavoritesStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun list(kind: String): List<FavoriteItem> = readBucket(kind)

    fun isFavorite(kind: String, id: String): Boolean = readBucket(kind).any { it.id == id }

    fun toggle(entry: CatalogEntry, kind: String): Boolean {
        val current = readBucket(kind).toMutableList()
        val existing = current.indexOfFirst { it.id == entry.id }
        return if (existing >= 0) {
            current.removeAt(existing)
            writeBucket(kind, current)
            false
        } else {
            current.add(
                0,
                FavoriteItem(
                    id = entry.id,
                    title = entry.title,
                    imageUrl = entry.imageUrl,
                    categoryId = entry.categoryId,
                    kind = kind,
                    savedAt = System.currentTimeMillis(),
                ),
            )
            writeBucket(kind, current.take(MAX))
            true
        }
    }

    fun kindOf(catalogKind: CatalogKind): String? = when (catalogKind) {
        CatalogKind.LIVE_STREAMS -> "live"
        CatalogKind.VOD_STREAMS, CatalogKind.VOD_STREAM -> "vod"
        CatalogKind.SERIES, CatalogKind.SERIES_DETAIL -> "series"
        CatalogKind.SERIES_EPISODES -> "series"
        else -> null
    }

    private fun readBucket(kind: String): List<FavoriteItem> {
        val raw = prefs.getString(kind, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        FavoriteItem(
                            id = o.getString("id"),
                            title = o.optString("name", o.optString("title")),
                            imageUrl = o.optString("imageUrl").takeIf { it.isNotBlank() },
                            categoryId = o.optString("categoryId").takeIf { it.isNotBlank() },
                            kind = o.optString("kind", kind),
                            savedAt = o.optLong("savedAt", 0L),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeBucket(kind: String, items: List<FavoriteItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject()
                    .put("id", item.id)
                    .put("name", item.title)
                    .put("imageUrl", item.imageUrl ?: "")
                    .put("categoryId", item.categoryId ?: "")
                    .put("kind", item.kind)
                    .put("savedAt", item.savedAt),
            )
        }
        prefs.edit().putString(kind, arr.toString()).apply()
    }

    private companion object {
        const val FILE = "cotas_favorites_v1"
        const val MAX = 80
    }
}

data class WatchProgress(
    val seriesId: String,
    val seasonNumber: Int,
    val episodeId: String,
    val episodeTitle: String?,
    val updatedAt: Long,
)

class WatchStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun get(seriesId: String): WatchProgress? {
        val raw = prefs.getString(seriesId, null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            WatchProgress(
                seriesId = o.getString("seriesId"),
                seasonNumber = o.getInt("season"),
                episodeId = o.getString("episodeId"),
                episodeTitle = o.optString("episodeTitle").takeIf { it.isNotBlank() },
                updatedAt = o.optLong("updatedAt", 0L),
            )
        }.getOrNull()
    }

    fun save(progress: WatchProgress) {
        val o = JSONObject()
            .put("seriesId", progress.seriesId)
            .put("season", progress.seasonNumber)
            .put("episodeId", progress.episodeId)
            .put("episodeTitle", progress.episodeTitle ?: "")
            .put("updatedAt", progress.updatedAt)
        prefs.edit().putString(progress.seriesId, o.toString()).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val FILE = "cotas_watch_v1"
    }
}
