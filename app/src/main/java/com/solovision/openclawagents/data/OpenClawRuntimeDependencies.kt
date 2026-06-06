package com.solovision.openclawagents.data

import android.content.Context
import android.util.Log
import com.solovision.openclawagents.BuildConfig

data class OpenClawRuntimeDependencies(
    val repository: OpenClawRepository,
    val missionControlService: MissionControlService?
)

fun buildOpenClawRuntimeDependencies(context: Context): OpenClawRuntimeDependencies {
    val gatewayUrl = resolveGatewayUrl(context)
    val sessionKey = resolveSessionKey(context)
    val apiKey = resolveApiKey(context)

    if (gatewayUrl.isBlank() || sessionKey.isBlank()) {
        Log.w("OpenClawRuntime", "Gateway URL or session key is blank, falling back to fake repository")
        return OpenClawRuntimeDependencies(
            repository = FakeOpenClawRepository(),
            missionControlService = null
        )
    }

    return runCatching {
        val transport = GatewayRpcOpenClawTransport(
            context = context,
            config = OpenClawBackendConfig(
                gatewayUrl = gatewayUrl,
                sessionKey = sessionKey,
                apiKey = apiKey
            )
        )
        OpenClawRuntimeDependencies(
            repository = RealOpenClawRepository(transport),
            missionControlService = MissionControlService(transport)
        )
    }.getOrElse { error ->
        Log.e("OpenClawRuntime", "Failed to initialize real repository, falling back to fake", error)
        OpenClawRuntimeDependencies(
            repository = FakeOpenClawRepository(),
            missionControlService = null
        )
    }
}

private fun resolveGatewayUrl(context: Context): String {
    val prefs = context.getSharedPreferences("openclaw_gateway", Context.MODE_PRIVATE)
    return prefs.getString("gateway_url", null)
        ?.takeIf { it.isNotBlank() }
        ?: BuildConfig.OPENCLAW_GATEWAY_URL.trim()
}

private fun resolveSessionKey(context: Context): String {
    val prefs = context.getSharedPreferences("openclaw_gateway", Context.MODE_PRIVATE)
    return prefs.getString("session_key", null)
        ?.takeIf { it.isNotBlank() }
        ?: BuildConfig.OPENCLAW_SESSION_KEY.trim()
}

private fun resolveApiKey(context: Context): String? {
    val prefs = context.getSharedPreferences("openclaw_gateway", Context.MODE_PRIVATE)
    return prefs.getString("api_key", null)
        ?.takeIf { it.isNotBlank() }
        ?: BuildConfig.OPENCLAW_API_KEY.trim().takeIf { it.isNotBlank() }
}
