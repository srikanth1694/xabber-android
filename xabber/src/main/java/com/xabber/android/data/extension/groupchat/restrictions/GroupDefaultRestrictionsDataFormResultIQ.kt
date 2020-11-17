package com.xabber.android.data.extension.groupchat.restrictions

import org.jivesoftware.smackx.xdata.packet.DataForm

class GroupDefaultRestrictionsDataFormResultIQ: AbstractGroupDefaultRestrictionsIQ() {

    var dataForm: DataForm? = null

    init {
        this.type = Type.result
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {}

}