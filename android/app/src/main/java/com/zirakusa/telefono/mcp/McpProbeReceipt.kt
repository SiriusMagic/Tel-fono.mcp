package com.zirakusa.telefono.mcp

import org.json.JSONObject

internal object McpProbeReceipt {
    private val trace = Regex("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")
    @Volatile var id: String? = null
        private set

    fun accept(requestId: Any?): JSONObject {
        if (requestId is String && trace.matches(requestId)) id = requestId
        return JSONObject()
    }
}
