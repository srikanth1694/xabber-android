package com.xabber.android.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.groupchat.OnGroupchatRequestListener
import com.xabber.android.data.extension.groupchat.rights.GroupchatMemberRightsReplyIQ
import com.xabber.android.data.message.chat.groupchat.GroupChat
import com.xabber.android.data.message.chat.groupchat.GroupchatMember
import com.xabber.android.data.message.chat.groupchat.GroupchatMemberManager
import com.xabber.android.ui.activity.GroupchatMemberActivity
import com.xabber.android.ui.adapter.groups.GroupMemberRightsFormListAdapter
import com.xabber.android.ui.color.ColorManager
import org.jivesoftware.smackx.xdata.FormField
import org.jivesoftware.smackx.xdata.packet.DataForm

class GroupchatMemberInfoFragment(val groupchatMember: GroupchatMember, val groupchat: GroupChat)
    : Fragment(), OnGroupchatRequestListener, GroupMemberRightsFormListAdapter.Listener {

    var recyclerView: RecyclerView? = null
    var adapter: GroupMemberRightsFormListAdapter? = null

    var oldDataForm: DataForm? = null

    private val newFields = mutableMapOf<String, FormField>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.groupchat_member_edit_fragment, container, false)
        recyclerView = view.findViewById(R.id.groupchat_member_rights_recycler_view)
        recyclerView?.layoutManager = LinearLayoutManager(context).apply {
            orientation = LinearLayoutManager.VERTICAL
        }

        GroupchatMemberManager.getInstance().requestGroupchatMemberRightsForm(groupchat.account,
                groupchat.contactJid, groupchatMember)

        return view
    }

    override fun onResume() {
        Application.getInstance().addUIListener(OnGroupchatRequestListener::class.java, this)
        super.onResume()
    }

    override fun onPause() {
        Application.getInstance().removeUIListener(OnGroupchatRequestListener::class.java, this)
        super.onPause()
    }

    private fun setupRecyclerViewWithDataForm(dataForm: DataForm){
        adapter = GroupMemberRightsFormListAdapter(dataForm,
                ColorManager.getInstance().accountPainter.getAccountSendButtonColor(groupchat.account),
                fragmentManager!!, this)

        recyclerView?.adapter = adapter
        adapter?.notifyDataSetChanged()

    }

    override fun onGroupchatBlocklistReceived(account: AccountJid?, groupchatJid: ContactJid?) { }

    override fun onGroupchatInvitesReceived(account: AccountJid?, groupchatJid: ContactJid?) { }

    override fun onGroupchatMembersReceived(account: AccountJid?, groupchatJid: ContactJid?) { }

    override fun onMeReceived(accountJid: AccountJid?, groupchatJid: ContactJid?) { }

    override fun onGroupchatMemberUpdated(accountJid: AccountJid?, groupchatJid: ContactJid?, groupchatMemberId: String?) { }

    override fun onGroupchatMemberRightsFormReceived(accountJid: AccountJid,
                                                     groupchatJid: ContactJid,
                                                     iq: GroupchatMemberRightsReplyIQ) {

        for (field in iq.dataFrom!!.fields)
            if (field.variable == GroupchatMemberRightsReplyIQ.FIELD_USER_ID
                    && groupchatMember.id == field.values[0]){
                oldDataForm = iq.dataFrom
                Application.getInstance().runOnUiThread {
                    setupRecyclerViewWithDataForm(iq.dataFrom!!)
                }
                break
            }

    }

    override fun onOptionPicked(field: FormField, option: FormField.Option?, isChecked: Boolean) {

        // удалить из списка newFields поле с таким же var если оно уже есть там в любом случае
        if (newFields.containsKey(field.variable)) newFields.remove(field.variable)

        // проверить отличается ли полученное поле с опциями от того, что пришло в айкью
        // если отличается, то добавить в список новых полей
        if (checkPickIsNew(field, option, isChecked)) {

            val newFormField = FormField(field.variable)
            newFormField.type = FormField.Type.list_single
            newFormField.label = field.label
            if (option != null)
                newFormField.addValue(option.value)
            else newFormField.addValue("")

            newFields[field.variable] = newFormField
        }

        // проверить размер списка новых полей и отправить сигнал о наличии или отсуствии новых полей
        notifyActivityAboutNewFieldSizeChanged()
    }

    private fun checkPickIsNew(newField: FormField, newOption: FormField.Option?, isChecked: Boolean): Boolean{
        for (oldField in oldDataForm!!.fields){
            if (oldField.variable == newField.variable){
                if (newOption != null){
                    if (oldField.values != null && oldField.values.size != 0){
                        if (oldField.values[0] as String == newOption.value) return false
                    } else return true
                } else {
                    return oldField.values != null && oldField.values.size != 0 && !isChecked
                }
            }
        }
        return true
    }

    private fun notifyActivityAboutNewFieldSizeChanged(){
        if (activity != null && activity is GroupchatMemberActivity)
            (activity as GroupchatMemberActivity).onNewMemberRightsFormFieldChanged(newFields.size)
    }

    private fun createNewDataFrom(): DataForm {
        val newDataForm = DataForm(DataForm.Type.submit).apply {
            title = oldDataForm?.title
            instructions = oldDataForm?.instructions
        }

        for (oldFormField in oldDataForm!!.fields){

            if (oldFormField.variable == null) continue

            val formFieldToBeAdded = FormField(oldFormField.variable).apply {
                type = oldFormField.type
            }

            if (newFields.containsKey(formFieldToBeAdded.variable))
                formFieldToBeAdded.addValue(newFields[formFieldToBeAdded.variable]!!.values[0])
            else if (oldFormField.values != null && oldFormField.values.size > 0)
                formFieldToBeAdded.addValue(oldFormField.values[0])

            newDataForm.addField(formFieldToBeAdded)
        }

        return newDataForm
    }

    fun sendSaveRequest() = GroupchatMemberManager.getInstance()
            .requestGroupchatMemberRightsChange(groupchat, createNewDataFrom())

    companion object{
        const val TAG = "com.xabber.android.ui.fragment.GroupchatMemberInfoFragment"
    }

}