/*
 * Copyright (c) 2022. Danterus
 * Copyright (c) 2022. Sorus Contributors
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package com.github.sorusclient.client.websocket

import com.github.sorusclient.client.adapter.AdapterManager
import com.github.sorusclient.client.notification.Notification
import com.github.sorusclient.client.notification.NotificationManager
import com.github.sorusclient.client.notification.display
import kotlinx.coroutines.runBlocking
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.lang.Exception
import java.net.URI

class Websocket: WebSocketClient(URI.create("wss://socket.sorus.ml:8443")) {

    override fun onOpen(handshakedata: ServerHandshake?) {

    }

    override fun onMessage(message: String) {
        val id = message.substring(0, message.indexOf(" "))
        val json = JSONObject(message.substring(message.indexOf(" ")))

        println("$id $json")

        if (id == "connected") {
            if (WebSocketManager.failedToConnect) {
                WebSocketManager.failedToConnect = false
                Notification().apply {
                    title = "Websocket"
                    content = "Websocket connected!"
                }.display()
            }

            WebSocketManager.sendMessage("updateStatus", JSONObject().apply {
                put("version", AdapterManager.adapter.version)
                put("action", "")
            }, true)

            WebSocketManager.connected = true
        }

        runBlocking {
            WebSocketManager.listeners[id]?.let { it(json) }
        }
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) {
        println(reason)
        WebSocketManager.connected = false
    }

    override fun onError(ex: Exception) {
        ex.printStackTrace()
        WebSocketManager.connected = false
        this.close()
    }

}