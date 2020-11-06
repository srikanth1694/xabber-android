package com.xabber.android.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.adapter.ContactCircleEditorAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * This is a parent Fragment that helps initialize a RecyclerView with
 * Contact's Circles adapter to any extending fragment.
 * <p>
 * For it to work properly, an extending fragment must have a RecyclerView
 * list in the layout file with id = "@+id/rvCircles", as well as the
 * super() call in {@link #onActivityCreated(Bundle)}
 */
public class CircleEditorFragment extends Fragment implements ContactCircleEditorAdapter.OnCircleActionListener {

    static final String ARG_ACCOUNT = "com.xabber.android.ui.fragment.CircleEditorFragment.ARG_ACCOUNT";
    static final String ARG_USER = "com.xabber.android.ui.fragment.CircleEditorFragment.ARG_USER";

    private static final String SAVED_CIRCLES = "com.xabber.android.ui.fragment.CircleEditorFragment.SAVED_CIRCLES";
    private static final String SAVED_SELECTED = "com.xabber.android.ui.fragment.CircleEditorFragment.SAVED_SELECTED";
    private static final String SAVED_ADD_CIRCLE_NAME = "com.xabber.android.ui.fragment.CircleEditorFragment.SAVED_ADD_CIRCLE_NAME";

    private AccountJid account;
    private ContactJid contactJid;

    private RecyclerView rvContactCircles;
    private ContactCircleEditorAdapter contactCircleEditorAdapter;

    private Collection<String> circles;
    private Collection<String> selected = new HashSet<>();

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public CircleEditorFragment() {
    }

    public static CircleEditorFragment newInstance(AccountJid account, ContactJid user) {
        CircleEditorFragment fragment = new CircleEditorFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_ACCOUNT, account);
        args.putParcelable(ARG_USER, user);
        fragment.setArguments(args);
        return fragment;
    }

    private static void hideKeyboard(Activity activity) {
        // Check if no view has focus:
        if (activity != null) {
            View view = activity.getCurrentFocus();
            if (view != null) {
                InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            account = getArguments().getParcelable(ARG_ACCOUNT);
            contactJid = getArguments().getParcelable(ARG_USER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.circles_list_layout, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initRecyclerView(getView());

        if (savedInstanceState != null) {
            circles = savedInstanceState.getStringArrayList(SAVED_CIRCLES);
            selected = savedInstanceState.getStringArrayList(SAVED_SELECTED);
            String circleAddInput = savedInstanceState.getString(SAVED_ADD_CIRCLE_NAME);
            contactCircleEditorAdapter.setInputCircleName(circleAddInput);
        } else {
            if (account != null) circles = RosterManager.getInstance().getCircles(getAccount());
            if (contactJid != null) selected = RosterManager.getInstance().getCircles(getAccount(), getContactJid());
        }
    }

    void initRecyclerView(View rootView) {
        if (rootView == null) return;

        rvContactCircles = rootView.findViewById(R.id.rvCircles);
        contactCircleEditorAdapter = new ContactCircleEditorAdapter(this);

        rvContactCircles.setLayoutManager(new LinearLayoutManager(getContext()));
        rvContactCircles.setAdapter(contactCircleEditorAdapter);
        rvContactCircles.setNestedScrollingEnabled(false);
    }

    protected void setAccountCircles() {
        circles = RosterManager.getInstance().getCircles(getAccount());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (circles != null) updateCircles();
    }

    protected void updateCircles() {
        ArrayList<String> list = new ArrayList<>(circles);
        ArrayList<ContactCircleEditorAdapter.ContactCircle> circles = new ArrayList<>(this.circles.size());
        Collections.sort(list);
        contactCircleEditorAdapter.clear();

        for (int position = 0; position < list.size(); position++) {
            String circleName = list.get(position);
            circles.add(new ContactCircleEditorAdapter.ContactCircle(circleName, selected.contains(circleName)));
        }
        if (circles.size() > 0) contactCircleEditorAdapter.add(circles);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        selected = getSelected();

        outState.putStringArrayList(SAVED_CIRCLES, getCircles());
        outState.putStringArrayList(SAVED_SELECTED, new ArrayList<>(selected));
        outState.putString(SAVED_ADD_CIRCLE_NAME, contactCircleEditorAdapter.getInputCircleName());
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    RecyclerView getListView() {
        return rvContactCircles;
    }

    protected ArrayList<String> getCircles() {
        return contactCircleEditorAdapter.getCircles();
    }

    public ArrayList<String> getSelected() {
        return contactCircleEditorAdapter.getSelected();
    }

    @Override
    public void onCircleAdded() {
        hideKeyboard(getActivity());
    }

    @Override
    public void onCircleToggled() {
    }

    public void saveCircles() {
        selected = getSelected();

        if (account != null && contactJid != null) {
            try {
                RosterManager.getInstance().setCircles(account, contactJid, selected);
            } catch (NetworkException e) {
                Application.getInstance().onError(e);
            }
        }
    }

    protected AccountJid getAccount() {
        return account;
    }

    protected void setAccount(AccountJid account) {
        this.account = account;
    }

    protected ContactJid getContactJid() {
        return contactJid;
    }

    protected void setContactJid(ContactJid contactJid) {
        this.contactJid = contactJid;
    }
}