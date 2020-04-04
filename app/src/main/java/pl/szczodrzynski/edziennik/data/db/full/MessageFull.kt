/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */
package pl.szczodrzynski.edziennik.data.db.full

import androidx.room.Relation
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.MessageRecipient

class MessageFull(
        profileId: Int, id: Long, type: Int,
        subject: String, body: String?, senderId: Long?
) : Message(
        profileId, id, type,
        subject, body, senderId
) {
    var senderName: String? = null
    @Relation(parentColumn = "messageId", entityColumn = "messageId", entity = MessageRecipient::class)
    var recipients: MutableList<MessageRecipientFull>? = null

    fun addRecipient(recipient: MessageRecipientFull): MessageFull {
        if (recipients == null) recipients = mutableListOf()
        recipients?.add(recipient)
        return this
    }

    // metadata
    var seen = false
    var notified = false
    var addedDate: Long = 0
}
