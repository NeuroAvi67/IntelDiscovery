package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.IntegrationStatus
import com.example.data.model.SearchProviderId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.integrationDataStore by preferencesDataStore(name = "intelatlas_integrations")

data class LocalIntegrationSecrets(
    val tavilyApiKey: String = "",
    val tavilyStatus: IntegrationStatus = IntegrationStatus.CONFIGURATION_REQUIRED,
    val tavilyLastMessage: String = "",
    val braveApiKey: String = "",
    val braveStatus: IntegrationStatus = IntegrationStatus.CONFIGURATION_REQUIRED,
    val braveLastMessage: String = "",
    val serpApiKey: String = "",
    val serpStatus: IntegrationStatus = IntegrationStatus.CONFIGURATION_REQUIRED,
    val serpLastMessage: String = ""
) {
    fun keyFor(provider: SearchProviderId): String = when (provider) {
        SearchProviderId.TAVILY -> tavilyApiKey
        SearchProviderId.BRAVE -> braveApiKey
        SearchProviderId.SERPAPI -> serpApiKey
    }

    fun statusFor(provider: SearchProviderId): IntegrationStatus = when (provider) {
        SearchProviderId.TAVILY -> tavilyStatus
        SearchProviderId.BRAVE -> braveStatus
        SearchProviderId.SERPAPI -> serpStatus
    }

    fun messageFor(provider: SearchProviderId): String = when (provider) {
        SearchProviderId.TAVILY -> tavilyLastMessage
        SearchProviderId.BRAVE -> braveLastMessage
        SearchProviderId.SERPAPI -> serpLastMessage
    }

    fun hasAnyConfiguredSearchApi(): Boolean =
        tavilyApiKey.isNotBlank() || braveApiKey.isNotBlank() || serpApiKey.isNotBlank()
}

class IntegrationPreferencesStore(private val context: Context) {
    private object Keys {
        val TAVILY_KEY = stringPreferencesKey("tavily_api_key")
        val TAVILY_STATUS = stringPreferencesKey("tavily_status")
        val TAVILY_MSG = stringPreferencesKey("tavily_msg")
        val BRAVE_KEY = stringPreferencesKey("brave_api_key")
        val BRAVE_STATUS = stringPreferencesKey("brave_status")
        val BRAVE_MSG = stringPreferencesKey("brave_msg")
        val SERP_KEY = stringPreferencesKey("serp_api_key")
        val SERP_STATUS = stringPreferencesKey("serp_status")
        val SERP_MSG = stringPreferencesKey("serp_msg")
    }

    val state: Flow<LocalIntegrationSecrets> = context.integrationDataStore.data.map { prefs ->
        val tavilyKey = prefs[Keys.TAVILY_KEY].orEmpty()
        val braveKey = prefs[Keys.BRAVE_KEY].orEmpty()
        val serpKey = prefs[Keys.SERP_KEY].orEmpty()
        LocalIntegrationSecrets(
            tavilyApiKey = tavilyKey,
            tavilyStatus = parseStatus(prefs[Keys.TAVILY_STATUS], tavilyKey),
            tavilyLastMessage = prefs[Keys.TAVILY_MSG].orEmpty(),
            braveApiKey = braveKey,
            braveStatus = parseStatus(prefs[Keys.BRAVE_STATUS], braveKey),
            braveLastMessage = prefs[Keys.BRAVE_MSG].orEmpty(),
            serpApiKey = serpKey,
            serpStatus = parseStatus(prefs[Keys.SERP_STATUS], serpKey),
            serpLastMessage = prefs[Keys.SERP_MSG].orEmpty()
        )
    }

    suspend fun current(): LocalIntegrationSecrets = state.first()

    suspend fun saveProviderConfig(
        provider: SearchProviderId,
        apiKey: String,
        status: IntegrationStatus,
        message: String
    ) {
        val trimmed = apiKey.trim()
        context.integrationDataStore.edit { prefs ->
            when (provider) {
                SearchProviderId.TAVILY -> {
                    prefs[Keys.TAVILY_KEY] = trimmed
                    prefs[Keys.TAVILY_STATUS] = status.name
                    prefs[Keys.TAVILY_MSG] = message
                }
                SearchProviderId.BRAVE -> {
                    prefs[Keys.BRAVE_KEY] = trimmed
                    prefs[Keys.BRAVE_STATUS] = status.name
                    prefs[Keys.BRAVE_MSG] = message
                }
                SearchProviderId.SERPAPI -> {
                    prefs[Keys.SERP_KEY] = trimmed
                    prefs[Keys.SERP_STATUS] = status.name
                    prefs[Keys.SERP_MSG] = message
                }
            }
        }
    }

    suspend fun disconnectProvider(provider: SearchProviderId) {
        saveProviderConfig(
            provider = provider,
            apiKey = "",
            status = IntegrationStatus.DISCONNECTED,
            message = "Disconnected by user"
        )
    }

    private fun parseStatus(raw: String?, key: String): IntegrationStatus {
        if (key.isBlank()) {
            return if (raw == IntegrationStatus.DISCONNECTED.name) {
                IntegrationStatus.DISCONNECTED
            } else {
                IntegrationStatus.CONFIGURATION_REQUIRED
            }
        }
        return IntegrationStatus.entries.firstOrNull { it.name == raw } ?: IntegrationStatus.CONNECTED
    }
}
