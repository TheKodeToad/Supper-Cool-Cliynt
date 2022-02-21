package com.github.sorusclient.client.server

import com.github.sorusclient.client.adapter.AdapterManager
import com.github.sorusclient.client.adapter.event.GameJoinEvent
import com.github.sorusclient.client.adapter.event.GameLeaveEvent
import com.github.sorusclient.client.adapter.event.SorusCustomPacketEvent
import com.github.sorusclient.client.event.EventManager
import org.apache.commons.io.IOUtils
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URL

object ServerIntegrationManager {

    private const val baseServersUrl = "https://raw.githubusercontent.com/sorusclient/asset/main/server"
    private const val serversJsonUrl = "$baseServersUrl/servers.json"

    val joinListeners: MutableMap<String, (Any) -> Unit> = HashMap()
    val leaveListeners: MutableList<() -> Unit> = ArrayList()

    init {
        val eventManager = EventManager
        eventManager.register<GameJoinEvent> { onGameJoin() }
        eventManager.register<GameLeaveEvent> { onGameLeave() }
        eventManager.register(this::onCustomPacket)
    }

    private fun onGameJoin() {
        val server = AdapterManager.getAdapter().currentServer
        if (server != null) {
            Thread {
                val json = getJsonForServer(server.ip)
                if (json != null) {
                    applyServerConfiguration(json)
                }
            }.start()
        }
    }

    private fun onGameLeave() {
        for (listener in leaveListeners) {
            listener()
        }
    }

    private fun onCustomPacket(event: SorusCustomPacketEvent) {
        if (event.channel == "integration") {
            applyServerConfiguration(event.contents)
        }
    }

    private fun getJsonForServer(ip: String): String? {
        try {
            val inputStream = URL(serversJsonUrl).openStream()
            val jsonString = IOUtils.toString(inputStream)
            inputStream.close()
            val json = JSONObject(jsonString).toMap()
            for ((key, value) in json) {
                if (ip.matches((value as String).toRegex())) {
                    val inputStream1 = URL("$baseServersUrl/$key.json").openStream()
                    val serverJson = IOUtils.toString(inputStream1)
                    inputStream1.close()
                    return serverJson
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun applyServerConfiguration(json: String) {
        val jsonObject = JSONObject(json).toMap()
        try {
            for ((key, value) in jsonObject) {
                if (joinListeners[key] != null) {
                    joinListeners[key]?.let { it(value) }
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

}