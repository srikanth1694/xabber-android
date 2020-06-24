package com.xabber.android.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.xabber.android.data.extension.groupchat.invite.OnGroupchatSelectorListToolbarActionResult;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.chat.groupchat.GroupChat;
import com.xabber.android.data.message.chat.groupchat.GroupchatManager;
import com.xabber.android.ui.activity.GroupchatSettingsActivity.GroupchatSelectorListToolbarActions;
import com.xabber.android.ui.adapter.GroupchatInvitesAdapter;
import com.xabber.android.ui.fragment.GroupchatInfoFragment.GroupchatSelectorListItemActions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GroupchatInvitesFragment extends Fragment implements GroupchatSelectorListToolbarActions, OnGroupchatSelectorListToolbarActionResult {

    private static final String ARG_ACCOUNT = "com.xabber.android.ui.fragment.GroupchatInvitesFragment.ARG_ACCOUNT";
    private static final String ARG_GROUPCHAT_CONTACT = "com.xabber.android.ui.fragment.GroupchatInvitesFragment.ARG_GROUPCHAT_CONTACT";

    private AccountJid account;
    private ContactJid groupchatContact;
    private GroupChat groupChat;

    private RecyclerView invitesList;
    private GroupchatInvitesAdapter adapter;

    private GroupchatSelectorListItemActions invitesListListener;

    public static GroupchatInvitesFragment newInstance(AccountJid account, ContactJid groupchatContact) {
        GroupchatInvitesFragment fragment = new GroupchatInvitesFragment();
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
            invitesListListener = (GroupchatSelectorListItemActions) requireActivity();
        } else {
            throw new RuntimeException(requireActivity().toString()
                    + " must implement GroupchatSettingsActivity.GroupchatElementListActionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        invitesListListener = null;
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
        invitesList = view.findViewById(R.id.groupchatSettingsElementList);
        invitesList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new GroupchatInvitesAdapter();
        adapter.setListener(invitesListListener);
        invitesList.setAdapter(adapter);
        adapter.setInvites(groupChat.getListOfInvites());
        return view;
    }

    public void actOnSelection() {
        adapter.disableItemClicks(true);
        Set<String> checkedInvites = adapter.getCheckedInvites();
        if (checkedInvites.size() == 0) {
            adapter.disableItemClicks(false);
            return;
        }
        if (checkedInvites.size() == 1) {
            GroupchatManager.getInstance().revokeGroupchatInvitation(account, groupchatContact, checkedInvites.iterator().next());
        } else {
            GroupchatManager.getInstance().revokeGroupchatInvitations(account, groupchatContact, checkedInvites);
        }
    }

    public void cancelSelection() {
        adapter.setCheckedInvites(new ArrayList<>());
    }

    @Override
    public void onActionSuccess(AccountJid account, ContactJid groupchatJid, List<String> successfulJids) {
        if (checkIfWrongEntity(account, groupchatJid)) return;
        adapter.setInvites(groupChat.getListOfInvites());
        adapter.removeCheckedInvites(successfulJids);
        adapter.disableItemClicks(false);
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
        Toast.makeText(getContext(), "Failed to revoke an invitation to " + failedJids, Toast.LENGTH_SHORT).show();
    }

    private boolean checkIfWrongEntity(AccountJid account, ContactJid groupchatJid) {
        if (account == null) return true;
        if (groupchatJid == null) return true;
        if (!account.getBareJid().equals(this.account.getBareJid())) return true;
        return !groupchatJid.getBareJid().equals(this.groupchatContact.getBareJid());
    }
}
