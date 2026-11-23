package com.cotasperu.tv.core.network

import com.cotasperu.tv.domain.ApiResult
import com.cotasperu.tv.domain.AppError
import com.cotasperu.tv.domain.AuthTokens
import com.cotasperu.tv.domain.DeviceLinkCreated
import com.cotasperu.tv.domain.DeviceLinkPoll
import com.cotasperu.tv.domain.HealthSnapshot
import com.cotasperu.tv.domain.PlaybackAuthorization
import com.cotasperu.tv.domain.PlaybackTarget
import com.cotasperu.tv.domain.UserProfile

interface CotasApi {
    fun health(): ApiResult<HealthSnapshot>
    fun login(
        username: String,
        password: String,
        deviceId: String,
        platform: String,
        deviceName: String,
    ): ApiResult<AuthTokens>
    fun register(email: String, password: String): ApiResult<AuthTokens> =
        ApiResult.Err(AppError.NotFound)
    fun refresh(refreshToken: String): ApiResult<AuthTokens>
    fun logout(accessToken: String): ApiResult<Unit>
    fun profile(accessToken: String): ApiResult<UserProfile>
    fun createDeviceLinkSession(
        deviceId: String,
        platform: String,
        deviceName: String,
    ): ApiResult<DeviceLinkCreated>
    fun pollDeviceLinkSession(linkCode: String, deviceId: String): ApiResult<DeviceLinkPoll>
    fun pollQrSession(sessionId: String, pollSecret: String): ApiResult<DeviceLinkPoll> =
        ApiResult.Err(AppError.NotFound)
    fun cancelQrSession(sessionId: String, pollSecret: String): ApiResult<Unit> =
        ApiResult.Err(AppError.NotFound)
    fun catalog(path: String, accessToken: String, query: Map<String, String> = emptyMap()): ApiResult<String>
    fun authorizePlayback(accessToken: String, target: PlaybackTarget): ApiResult<PlaybackAuthorization>
}
