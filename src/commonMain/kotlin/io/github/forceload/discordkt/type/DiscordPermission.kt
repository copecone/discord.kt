package io.github.forceload.discordkt.type

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * https://discord.com/developers/docs/topics/permissions#permissions-bitwise-permission-flags
 */

enum class DiscordPermission(val id: Long) {
    CREATE_INSTANT_INVITE(1L shl 0),
    KICK_MEMBERS(1L shl 1),
    BAN_MEMBERS(1L shl 2),
    ADMINISTRATOR(1L shl 3),
    MANAGE_CHANNELS(1L shl 4),
    MANAGE_GUILD(1L shl 5),
    ADD_REACTIONS(1L shl 6),
    VIEW_AUDIT_LOG(1L shl 7),
    PRIORITY_SPEAKER(1L shl 8),
    STREAM(1L shl 9),
    VIEW_CHANNEL(1L shl 10),
    SEND_MESSAGES(1L shl 11),
    SEND_TTS_MESSAGES(1L shl 12),
    MANAGE_MESSAGES(1L shl 13),
    EMBED_LINKS(1L shl 14),
    ATTACH_FILES(1L shl 15),
    READ_MESSAGE_HISTORY(1L shl 16),
    MENTION_EVERYONE(1L shl 17),
    USE_EXTERNAL_EMOJIS(1L shl 18),
    VIEW_GUILD_INSIGHTS(1L shl 19),
    CONNECT(1L shl 20),
    SPEAK(1L shl 21),
    MUTE_MEMBERS(1L shl 22),
    DEAFEN_MEMBERS(1L shl 23),
    MOVE_MEMBERS(1L shl 24),
    USE_VAD(1L shl 25),
    CHANGE_NICKNAME(1L shl 26),
    MANAGE_NICKNAMES(1L shl 27),
    MANAGE_ROLES(1L shl 28),
    MANAGE_WEBHOOKS(1L shl 29),
    MANAGE_GUILD_EXPRESSIONS(1L shl 30),
    USE_APPLICATION_COMMANDS(1L shl 31),
    REQUEST_TO_SPEAK(1L shl 32),
    MANAGE_EVENTS(1L shl 33),
    MANAGE_THREADS(1L shl 34),
    CREATE_PUBLIC_THREADS(1L shl 35),
    CREATE_PRIVATE_THREADS(1L shl 36),
    USE_EXTERNAL_STICKERS(1L shl 37),
    SEND_MESSAGES_IN_THREADS(1L shl 38),
    USE_EMBEDDED_ACTIVITIES(1L shl 39),
    MODERATE_MEMBERS(1L shl 40),
    VIEW_CREATOR_MONETIZATION_ANALYTICS(1L shl 41),
    USE_SOUNDBOARD(1L shl 42),
    USE_EXTERNAL_SOUNDS(1L shl 45),
    SEND_VOICE_MESSAGES(1L shl 46);

    object SetSerializer: KSerializer<Set<DiscordPermission>> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("DiscordPermission", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): Set<DiscordPermission> {
            val permString = decoder.decodeString()
            val permission = permString.toLong()

            val permissionSet = mutableSetOf<DiscordPermission>()
            DiscordPermission.entries.forEach {
                if (permission and it.id == it.id) permissionSet.add(it)
            }

            return permissionSet
        }

        override fun serialize(encoder: Encoder, value: Set<DiscordPermission>) {
            var result = 0L
            value.forEach { result = result or it.id }

            encoder.encodeString(result.toString())
        }
    }
}