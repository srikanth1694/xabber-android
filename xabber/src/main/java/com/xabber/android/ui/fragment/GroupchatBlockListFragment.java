package com.xabber.android.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.groupchat.block.GroupchatBlocklistItemElement;
import com.xabber.android.data.extension.groupchat.invite.OnGroupchatSelectorListToolbarActionResult;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.chat.groupchat.GroupChat;
import com.xabber.android.data.message.chat.groupchat.GroupchatMemberManager;
import com.xabber.android.ui.activity.GroupchatSettingsActivity.GroupchatSelectorListToolbarActions;
import com.xabber.android.ui.adapter.GroupchatBlocklistAdapter;
import com.xabber.android.ui.fragment.GroupchatInfoFragment.GroupchatSelectorListItemActions;
import com.xabber.android.ui.widget.DividerItemDecoration;

import java.util.ArrayList;
import java.util.List;

public class GroupchatBlockListFragment extends Fragment implements GroupchatSelectorListToolbarActions, OnGroupchatSelectorListToolbarActionResult {

    private static final String ARG_ACCOUNT = "com.xabber.android.ui.fragment.GroupchatBlockListFragment.ARG_ACCOUNT";
    private static final String ARG_GROUPCHAT_CONTACT = "com.xabber.android.ui.fragment.GroupchatBlockListFragment.ARG_GROUPCHAT_CONTACT";

    private AccountJid account;
    private ContactJid groupchatContact;
    private GroupChat groupChat;

    private RecyclerView blockList;
    private GroupchatBlocklistAdapter adapter;
    private TextView placeholder;

    private GroupchatSelectorListItemActions blockListListener;

    public static GroupchatBlockListFragment newInstance(AccountJid account, ContactJid groupchatContact) {
        GroupchatBlockListFragment fragment = new GroupchatBlockListFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_ACCOUNT, account);
        args.putParcelable(ARG_GROUPCHAT_CONTACT, groupchatContact);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (requireActivity() instanceof GroupchatSelectorListItemActions) {
            blockListListener = (GroupchatSelectorListItemActions) requireActivity();
        } else {
            throw new RuntimeException(requireActivity().toString()
                    + " must implement GroupchatSettingsActivity.GroupchatElementListActionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        blockListListener = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            account = args.getParcelable(ARG_ACCOUNT);
            groupchatContact = args.getParcelable(ARG_GROUPCHAT_CONTACT);
        } else {
            requireActivity().finish();
        }

        AbstractChat chat = ChatManager.getInstance().getChat(account, groupchatContact);
        if (chat instanceof GroupChat) {
            groupChat = (GroupChat) chat;
        } else {
            requireActivity().finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnGroupchatSelectorListToolbarActionResult.class, this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnGroupchatSelectorListToolbarActionResult.class, this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_groupchat_settings_list, container, false);
        placeholder = view.findViewById(R.id.groupchatSettingsPlaceholderTV);
        blockList = view.findViewById(R.id.groupchatSettingsElementList);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        blockList.setLayoutManager(llm);
        DividerItemDecoration divider = new DividerItemDecoration(blockList.getContext(), llm.getOrientation());
        divider.skipDividerOnLastItem(true);
        blockList.addItemDecoration(divider);
        adapter = new GroupchatBlocklistAdapter();
        adapter.setListener(blockListListener);
        blockList.setAdapter(adapter);
        if (groupChat.getListOfBlockedElements() != null
                && groupChat.getListOfBlockedElements().size() > 0)
            adapter.setBlockedItems(groupChat.getListOfBlockedElements());
        setupPlaceholder();
        return view;
    }

    private void setupPlaceholder(){
        if (groupChat.getListOfBlockedElements() != null
                && groupChat.getListOfBlockedElements().size() > 0){
            blockList.setVisibility(View.VISIBLE);
            placeholder.setVisibility(View.GONE);
        } else {
            blockList.setVisibility(View.GONE);
            placeholder.setVisibility(View.VISIBLE);
            placeholder.setText(getString(R.string.groupchat_blocklist_empty));
        }
    }

    private void prepopulateList() {
        ArrayList<GroupchatBlocklistItemElement> prepopList = new ArrayList<>();
        prepopList.add(new GroupchatBlocklistItemElement(GroupchatBlocklistItemElement.ItemType.id, "I"));
        prepopList.add(new GroupchatBlocklistItemElement(GroupchatBlocklistItemElement.ItemType.domain, "1"));
        prepopList.add(new GroupchatBlocklistItemElement(GroupchatBlocklistItemElement.ItemType.jid, "two"));
        prepopList.add(new GroupchatBlocklistItemElement(GroupchatBlocklistItemElement.ItemType.domain, "3"));
        prepopList.add(new GroupchatBlocklistItemElement(GroupchatBlocklistItemElement.ItemType.jid, "one"));
        prepopList.add(new GroupchatBlocklistItemElement(GroupchatBlocklistItemElement.ItemType.id, "III"));
        prepopList.add(new GroupchatBlocklistItemElement(GroupchatBlocklistItemElement.ItemType.jid, "five"));
        prepopList.add(new GroupchatBlocklistItemElement(GroupchatBlocklistItemElement.ItemType.jid, "three"));
        prepopList.add(new GroupchatBlocklistItemElement(GroupchatBlocklistItemElement.ItemType.id, "II"));
        prepopList.add(new GroupchatBlocklistItemElement(GroupchatBlocklistItemElement.ItemType.domain, "2"));
        prepopList.add(new GroupchatBlocklistItemElement(GroupchatBlocklistItemElement.ItemType.jid, "four"));
        adapter.setBlockedItems(prepopList);
    }

    public void actOnSelection() {
        adapter.disableItemClicks(true);
        List<GroupchatBlocklistItemElement> selectedElements = adapter.getSelectedItems();
        if (selectedElements.size() == 0) {
            adapter.disableItemClicks(false);
            return;
        }
        if (selectedElements.size() == 1) {
            GroupchatMemberManager.getInstance().unblockGroupchatBlockedElement(account, groupchatContact, selectedElements.get(0));
        } else {
            GroupchatMemberManager.getInstance().unblockGroupchatBlockedElements(account, groupchatContact, selectedElements);
        }
    }

    public void cancelSelection() {
        adapter.cancelSelection();
    }

    @Override
    public void onActionSuccess(AccountJid account, ContactJid groupchatJid, List<String> successfulJids) {
        if (checkIfWrongEntity(account, groupchatJid)) return;
        adapter.setBlockedItems(groupChat.getListOfBlockedElements());
        adapter.removeSelectionStateFrom(successfulJids);
        adapter.disableItemClicks(false);
        setupPlaceholder();
    }

    @Override
    public void onPartialSuccess(AccountJid account, ContactJid groupchatJid, List<String> successfulJids, List<String> failedJids) {
        onActionSuccess(account, groupchatJid, successfulJids);
        onActionFailure(account, groupchatJid, failedJids);
    }

    @Override
    public void onActionFailure(AccountJid account, ContactJid groupchatJid, List<String> failedJids) {
        if (checkIfWrongEntity(account, groupchatJid)) return;
        adapter.disableItemClicks(false);
        Toast.makeText(getContext(), getString(R.string.groupchat_failed_to_unblock) + failedJids, Toast.LENGTH_SHORT).show();
    }

    private boolean checkIfWrongEntity(AccountJid account, ContactJid groupchatJid) {
        if (account == null) return true;
        if (groupchatJid == null) return true;
        if (!account.getBareJid().equals(this.account.getBareJid())) return true;
        return !groupchatJid.getBareJid().equals(this.groupchatContact.getBareJid());
    }
}
