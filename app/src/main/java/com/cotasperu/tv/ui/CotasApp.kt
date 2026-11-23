package com.cotasperu.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cotasperu.tv.di.AppGraph
import com.cotasperu.tv.ui.theme.CotasBg

@Composable
fun CotasApp(graph: AppGraph) {
    val factory = CotasViewModelFactory(graph)
    val gateViewModel: GateViewModel = viewModel(factory = factory)
    val homeViewModel: HomeViewModel = viewModel(factory = factory)
    val gate = gateViewModel.gate
    var splashPhase by rememberSaveable { mutableStateOf(SplashPhase.Playing) }
    val splashDone = splashPhase == SplashPhase.Done

    LaunchedEffect(homeViewModel.sessionExpired) {
        if (homeViewModel.sessionExpired) {
            homeViewModel.consumeSessionExpired()
            homeViewModel.closePlayer()
            gateViewModel.onSessionLost()
        }
    }

    // Tras el splash, no dejar Checking en negro: abrir login si health tarda.
    LaunchedEffect(splashDone) {
        if (!splashDone) return@LaunchedEffect
        kotlinx.coroutines.delay(2_500)
        gateViewModel.ensureLoginVisible()
    }

    Box(Modifier.fillMaxSize().background(CotasBg)) {
        if (splashDone) {
            when (val current = gate) {
                Gate.Checking -> {
                    Box(Modifier.fillMaxSize().background(Color.Black))
                }
                is Gate.Unavailable -> MessageScreen(
                    title = "Servicio no disponible",
                    message = current.message,
                    primaryLabel = "Reintentar",
                    onPrimary = gateViewModel::start,
                )
                is Gate.Unverified -> MessageScreen(
                    title = "No se pudo comprobar la sesión",
                    message = current.message,
                    primaryLabel = "Reintentar",
                    onPrimary = gateViewModel::retrySession,
                    secondaryLabel = "Iniciar sesión",
                    onSecondary = gateViewModel::useAnotherAccount,
                )
                is Gate.Login -> LoginScreen(
                    username = gateViewModel.username,
                    password = gateViewModel.password,
                    registrationMode = gateViewModel.registrationMode,
                    registrationName = gateViewModel.registrationName,
                    message = current.message,
                    submitting = gateViewModel.submitting,
                    locked = gateViewModel.loginLockSeconds > 0,
                    keepSession = gateViewModel.keepSession,
                    qrLinkCode = gateViewModel.qrLinkCode,
                    qrPayload = gateViewModel.qrPayload,
                    qrStatusText = gateViewModel.qrStatusText,
                    qrExpiresInSec = gateViewModel.qrExpiresInSec,
                    qrLoading = gateViewModel.qrLoading,
                    onUsername = gateViewModel::onUsername,
                    onPassword = gateViewModel::onPassword,
                    onRegistrationName = gateViewModel::onRegistrationName,
                    onKeepSession = gateViewModel::updateKeepSession,
                    onSubmit = if (gateViewModel.registrationMode) gateViewModel::register else gateViewModel::login,
                    onToggleRegistration = {
                        gateViewModel.toggleRegistrationMode(!gateViewModel.registrationMode)
                    },
                    onRegenerateQr = gateViewModel::regenerateQr,
                )
                Gate.Selector -> AccessSelectorScreen(
                    onStreamCode = { gateViewModel.enterCatalog("Stream Code") },
                    onLogin = { gateViewModel.enterCatalog("Cuenta COTAS") },
                )
                is Gate.Ready -> {
                    LaunchedEffect(current.displayName) {
                        homeViewModel.showSession()
                    }
                    BackHandler(
                        enabled = homeViewModel.playback == null,
                    ) {
                        when {
                            homeViewModel.detail != null -> homeViewModel.closeDetail()
                            homeViewModel.nav != NavItem.HOME -> homeViewModel.openNav(NavItem.HOME)
                            else -> Unit
                        }
                    }
                    val active = homeViewModel.playback
                    if (active != null) {
                        PlayerScreen(
                            session = active,
                            onExit = homeViewModel::closePlayer,
                            onRetryAuthorize = homeViewModel::retryPlayback,
                        )
                    } else {
                        CatalogShell(
                            displayName = current.displayName,
                            versionName = graph.versionName,
                            nav = homeViewModel.nav,
                            homeRails = homeViewModel.homeRails,
                            sectionRails = homeViewModel.sectionRails,
                            categories = homeViewModel.categories,
                            categoryId = homeViewModel.categoryId,
                            detail = homeViewModel.detail,
                            notice = homeViewModel.notice,
                            authorizing = homeViewModel.authorizing,
                            favoriteIds = homeViewModel.favoriteIds,
                            searchQuery = homeViewModel.searchQuery,
                            searchResults = homeViewModel.searchResults,
                            searching = homeViewModel.searching,
                            keepSession = homeViewModel.keepSession,
                            liveMode = homeViewModel.liveMode,
                            sectionFilterQuery = homeViewModel.sectionFilterQuery,
                            epgQuery = homeViewModel.epgQuery,
                            epgHits = homeViewModel.epgHits,
                            epgSearching = homeViewModel.epgSearching,
                            epgSearchStatus = homeViewModel.epgSearchStatus,
                            epgSearchHasMore = homeViewModel.epgSearchHasMore,
                            guideByStreamId = homeViewModel.guideByStreamId,
                            guideLoading = homeViewModel.guideLoading,
                            guideStatus = homeViewModel.guideStatus,
                            guideDayOffset = homeViewModel.guideDayOffset,
                            guideWindowShiftHours = homeViewModel.guideWindowShiftHours,
                            onNav = homeViewModel::openNav,
                            onSelectCategory = homeViewModel::selectCategory,
                            onOpen = homeViewModel::openEntry,
                            onOpenChild = homeViewModel::openChild,
                            onCloseDetail = homeViewModel::closeDetail,
                            onPlay = homeViewModel::play,
                            onToggleFavorite = homeViewModel::toggleFavorite,
                            onSearchQuery = homeViewModel::onSearchQuery,
                            onSearch = homeViewModel::runSearch,
                            onSectionFilter = homeViewModel::onSectionFilter,
                            onLiveMode = homeViewModel::selectLiveMode,
                            onEpgQuery = homeViewModel::onEpgQuery,
                            onEpgSearch = homeViewModel::runEpgSearch,
                            onEpgLoadMore = homeViewModel::loadMoreEpgSearch,
                            onEpgHit = homeViewModel::openEpgHit,
                            onGuideDayOffset = homeViewModel::selectGuideDay,
                            onGuideShiftWindow = homeViewModel::shiftGuideWindow,
                            onGuideVisibleStreamIds = homeViewModel::ensureGuideForStreamIds,
                            onGuideOpenChannel = homeViewModel::openGuideChannel,
                            onKeepSession = homeViewModel::updateKeepSession,
                            onLogout = gateViewModel::logout,
                            onRetry = homeViewModel::reload,
                        )
                    }
                }
            }
        } else {
            // Bajo el splash: negro (health corre en paralelo sin spinner).
            Box(Modifier.fillMaxSize().background(Color.Black))
        }

        if (!splashDone) {
            SplashIntro(
                phase = splashPhase,
                onPhase = { splashPhase = it },
            )
        }
    }
}
