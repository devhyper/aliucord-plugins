package io.github.devhyper.notifier

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.aliucord.patcher.before
import com.aliucord.patcher.instead
import com.discord.gateway.`GatewaySocket$close$1`
import com.discord.models.message.Message
import com.discord.stores.StoreConnectivity
import com.discord.stores.StoreMessagesHolder
import com.discord.utilities.fcm.NotificationClient
import com.discord.utilities.fcm.NotificationData
import com.discord.utilities.fcm.NotificationRenderer

@AliucordPlugin(requiresRestart = false)
class Notifier : Plugin() {
    override fun start(p0: Context?) {
        var globalMessage = "null"
        var backgrounded = false
        val patch =
            patcher.instead<`GatewaySocket$close$1`>("invoke") { null } //prevents gateway from closing when app is backgrounded
        val patch2 = patcher.after<StoreMessagesHolder>("addMessages", List::class.java) {
            //check if device is backgrounded, if true send notification
            if (backgrounded) {
                val messages = it.args[0] as List<Message>
                for (message in messages) {
                    //send notification for message
                    globalMessage = message.content
                    val messageId = Pair("message_id", message.id.toString())
                    val messageType = Pair("message_type", message.type.toString())
                    val messageType2 = Pair("message_type_", message.type.toString())
                    val messageContent = Pair("message_content", message.content)
                    val userId = Pair("user_id", message.author.id.toString())
                    val userUsername = Pair("user_username", message.author.username)
                    val userDiscriminator = Pair("user_discriminator", message.author.f())
                    val userAvatar = Pair("user_avatar", message.author.a().toString())
                    val applicationId = Pair("application_id", message.applicationId.toString())
                    val type = Pair("type", NotificationData.TYPE_MESSAGE_CREATE)
                    val channel = Pair("channel_id", message.channelId.toString())

                    val map = mutableMapOf<String, String>(
                        channel,
                        userAvatar,
                        applicationId,
                        type,
                        messageContent,
                        userUsername,
                        userDiscriminator,
                        userId,
                        messageId,
                        messageType,
                        messageType2
                    )
                    val notificationData = NotificationData(map)
                    val notificationClient = NotificationClient.INSTANCE
                    val notificationRenderer = NotificationRenderer.INSTANCE
                    notificationRenderer.display(
                        p0,
                        notificationData,
                        notificationClient.`settings$app_productionBetaRelease`
                    )
                }
            }
        }
        val patch3 = patcher.after<NotificationData>("getContent", Context::class.java) {
            val charSequence = globalMessage as CharSequence
            it.result = charSequence
        }
        val patch4 = patcher.before<StoreConnectivity>("handleBackgrounded", Boolean::class.java) {
            backgrounded = it.args[0] as Boolean
        }
    }

    override fun stop(p0: Context?) {
        patcher.unpatchAll()
    }
}