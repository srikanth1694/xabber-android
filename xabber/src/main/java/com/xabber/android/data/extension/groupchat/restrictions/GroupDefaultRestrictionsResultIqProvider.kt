package com.xabber.android.data.extension.groupchat.restrictions

import com.xabber.android.data.extension.groupchat.members.GroupchatMembersQueryIQ
import com.xabber.android.data.extension.httpfileupload.CustomDataProvider
import org.jivesoftware.smack.provider.IQProvider
import org.jivesoftware.smackx.xdata.packet.DataForm
import org.xmlpull.v1.XmlPullParser

class GroupDefaultRestrictionsResultIqProvider: IQProvider<GroupDefaultRestrictionsDataFormResultIQ>() {

    override fun parse(parser: XmlPullParser, initialDepth: Int): GroupDefaultRestrictionsDataFormResultIQ {
        val resultIQ = GroupDefaultRestrictionsDataFormResultIQ()
        var dataForm: DataForm? = DataForm(DataForm.Type.form)
        outerloop@ while (true) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.namespace) {
                    DataForm.NAMESPACE -> {
                        dataForm = CustomDataProvider.INSTANCE.parse(parser, initialDepth)
                        break@outerloop
                    }
                    else -> parser.next()
                }
                XmlPullParser.END_TAG -> if (GroupchatMembersQueryIQ.ELEMENT == parser.name) {
                    break@outerloop
                } else parser.next()
                else -> parser.next()
            }
        }
        resultIQ.dataForm = dataForm
        return resultIQ
    }

}