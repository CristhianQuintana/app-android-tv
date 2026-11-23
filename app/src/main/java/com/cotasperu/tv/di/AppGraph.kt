package com.cotasperu.tv.di

import android.content.Context
import com.cotasperu.tv.BuildConfig
import com.cotasperu.tv.core.network.CotasApiClient
import com.cotasperu.tv.core.prefs.FavoritesStore
import com.cotasperu.tv.core.prefs.AppAccountStore
import com.cotasperu.tv.core.prefs.PrefsStore
import com.cotasperu.tv.core.prefs.WatchStore
import com.cotasperu.tv.core.session.EncryptedSessionStore
import com.cotasperu.tv.core.session.PrefAwareSessionStore
import com.cotasperu.tv.core.session.SessionCoordinator
import com.cotasperu.tv.data.CatalogRepository
import com.cotasperu.tv.data.HealthRepository

class AppGraph(context: Context) {
    private val appContext = context.applicationContext
    private val api = CotasApiClient(
        baseUrl = BuildConfig.API_BASE_URL,
        lanHttpOrigin = BuildConfig.LAN_HTTP_ORIGIN,
    )
    val prefs = PrefsStore(appContext)
    val appAccount = AppAccountStore(appContext)
    val favorites = FavoritesStore(appContext)
    val watch = WatchStore(appContext)
    private val sessions = SessionCoordinator(
        api = api,
        store = PrefAwareSessionStore(
            persistent = EncryptedSessionStore(appContext),
            prefs = prefs,
        ),
    )
    val health = HealthRepository(api)
    val catalog = CatalogRepository(sessions, api)
    val session = sessions
    val allowInsecurePlayback = BuildConfig.ALLOW_LAN_HTTP
    val versionName: String = BuildConfig.VERSION_NAME
}
