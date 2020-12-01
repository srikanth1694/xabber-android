package com.xabber.android.data.extension.groupchat.rights

import com.xabber.android.data.message.chat.groupchat.GroupChat
import org.jivesoftware.smackx.xdata.packet.DataForm

class GroupRequestMemberRightsChangeIQ(val groupchat: GroupChat, val dataForm: DataForm)
    : GroupchatAbstractRightsIQ() {

    init {
        to = groupchat.fullJidIfPossible ?: groupchat.contactJid.jid
        type = Type.set
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        append(dataForm.toXML())
    }

}