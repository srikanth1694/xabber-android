package com.xabber.android.data.extension.groupchat.invite.outgoing

import com.xabber.android.data.entity.ContactJid
import org.junit.Assert.assertEquals
import org.junit.Test

class InviteMessageExtensionElementTest {

    private val extensionElement = InviteMessageExtensionElement(ContactJid.from("localpart@group.domain"),
            "Reason to invite")

    @Test
    fun test_toXml(){
        val reference1 =
                "<invite xmlns='https://xabber.com/protocol/groups#invite' jid='localpart@group.domain'>" +
                    "<reason>Reason to invite</reason>" +
                "</invite>"
        assertEquals(reference1, extensionElement.toXML().toString())
    }

    @Test
    fun test_getNamespace(){
        assertEquals("https://xabber.com/protocol/groups#invite", extensionElement.namespace)
    }

    @Test
    fun test_getElementName(){
        assertEquals("invite", extensionElement.elementName)
    }

}