package io.github.discordkt.discordkt.discord.internal

import io.github.discordkt.discordkt.discord.APIRequester
import io.github.discordkt.discordkt.discord.DiscordClient
import kotlinx.serialization.json.*
import java.io.IOException
import kotlin.system.exitProcess

@Suppress("unused")
class DiscordBot(val client: DiscordClient) {
    var id: String?
        get() = internalID
        set(id) = run { this.internalID = id; generateHeader() }
    var token: String?
        get() = internalToken
        set(token) = run { this.internalToken = token; generateHeader() }
    var intentFlag = 0

    private var internalID: String? = null
    private var internalToken: String? = null

    @Suppress("MemberVisibilityCanBePrivate")
    var authHeader: MutableMap<String, String>? = null

    private fun generateHeader() {
        if (this.internalID != null && this.internalToken != null) {
            this.authHeader = mutableMapOf("Authorization" to "Bot $token")
        }
    }

    lateinit var commands: JsonObject

    lateinit var commandObject: Commands

    fun commands(init: Commands.() -> Unit) {
        try {
            commands = APIRequester.getRequest("applications/$id/commands")

            commandObject = Commands(this)
            commandObject.init()

            Json.parseToJsonElement(commands["data"]!!.jsonPrimitive.content).jsonArray.forEach {
                val apiCommand = it.jsonObject["name"]!!.jsonPrimitive.content
                if (!commandObject.commandNames.contains(apiCommand)) {
                    APIRequester.deleteRequest(
                        "applications/$id/commands/${it.jsonObject["id"]!!.jsonPrimitive.content}"
                    )
                }
            }
        } catch (err: IOException) {
            exitProcess(-1)
        }
    }
}
