/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.data.connection;

import android.support.annotation.NonNull;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.roster.AccountRosterListener;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.parsing.ExceptionLoggingCallback;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.sm.predicates.ForEveryStanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;

import java.io.IOException;

/**
 * Abstract connection.
 *
 * @author alexander.ivanov
 */
public abstract class ConnectionItem implements ConnectionListener {

    @NonNull
    private final AccountJid account;

    /**
     * Connection options.
     */
    @NonNull
    private final ConnectionSettings connectionSettings;

    /**
     * XMPP connection.
     */
    @NonNull
    private XMPPTCPConnection connection;

    /**
     * Connection was requested by user.
     */
    private boolean isConnectionRequestedByUser;

    /**
     * Current state.
     */
    private ConnectionState state;

    /**
     * Whether force reconnection is in progress.
     */
    private boolean disconnectionRequested;

    /**
     * Need to register account on XMPP server.
     */
    private boolean registerNewAccount;

    @NonNull
    private final AccountRosterListener rosterListener;

    public ConnectionItem(boolean custom,
                          String host, int port, DomainBareJid serverName, Localpart userName,
                          Resourcepart resource, boolean storePassword, String password,
                          boolean saslEnabled, TLSMode tlsMode, boolean compression,
                          ProxyType proxyType, String proxyHost, int proxyPort,
                          String proxyUser, String proxyPassword) {
        this.account = AccountJid.from(userName, serverName, resource);
        rosterListener = new AccountRosterListener(getAccount());

        connectionSettings = new ConnectionSettings(userName,
                serverName, resource, custom, host, port, password,
                saslEnabled, tlsMode, compression, proxyType, proxyHost,
                proxyPort, proxyUser, proxyPassword);
        connection = ConnectionBuilder.build(connectionSettings);

        isConnectionRequestedByUser = false;
        disconnectionRequested = false;
        state = ConnectionState.offline;
    }

    @NonNull
    public AccountJid getAccount() {
        return account;
    }

    /**
     * Register new account on server.
     */
    public void registerAccount() {
        registerNewAccount = true;
    }

    /**
     * Report if this connection is to register a new account on XMPP server.
     */
    public boolean isRegisterAccount() {
        return registerNewAccount;
    }

    @NonNull
    public XMPPTCPConnection getConnection() {
        return connection;
    }

    /**
     * @return connection options.
     */
    @NonNull
    public ConnectionSettings getConnectionSettings() {
        return connectionSettings;
    }

    public ConnectionState getState() {
        return state;
    }

    /**
     * Returns real full jid, that was assigned while login.
     *
     * @return <code>null</code> if connection is not established.
     */
    public EntityFullJid getRealJid() {
        return connection.getUser();
    }

    /**
     * @param userRequest action was requested by user.
     * @return Whether connection is available.
     */
    protected abstract boolean isConnectionAvailable(boolean userRequest);

    /**
     * Connect or disconnect from server depending on internal flags.
     *
     * @param userRequest action was requested by user.
     * @return Whether state has been changed.
     */
    public boolean updateConnection(boolean userRequest) {
        boolean available = isConnectionAvailable(userRequest);

        LogManager.i(this, "updateConnection userRequest: " + userRequest + " isConnectionAvailable " + available);

        if (NetworkManager.getInstance().getState() != NetworkState.available
                || !available || disconnectionRequested) {
            ConnectionState target = available ? ConnectionState.waiting : ConnectionState.offline;
            if (state == ConnectionState.connected || state == ConnectionState.authentication
                    || state == ConnectionState.connecting) {
                if (userRequest) {
                    isConnectionRequestedByUser = false;
                }
                disconnect(connection);
                connectionClosed();
            } else if (state == target) {
                return false;
            }
            state = target;
            return true;
        } else {
            if (state == ConnectionState.offline || state == ConnectionState.waiting) {
                if (userRequest) {
                    isConnectionRequestedByUser = true;
                }
                restartConnection();

                return true;
            } else {
                return false;
            }
        }
    }

    private void restartConnection() {
        removeConnectionListeners();

        connection = ConnectionBuilder.build(connectionSettings);
        configureConnection();
        addConnectionListeners();
        state = ConnectionState.connecting;
        ConnectionManager.getInstance().onConnection(connection);
        new ConnectionThread(connection, this).start(registerNewAccount);
    }

    private void removeConnectionListeners() {
        connection.removeConnectionListener(this);

        final Roster roster = Roster.getInstanceFor(connection);
        roster.removeRosterListener(rosterListener);
        roster.removeRosterLoadedListener(rosterListener);
    }

    private void configureConnection() {
        // enable Stream Management support. SMACK will only enable SM if supported by the server,
        // so no additional checks are required.
        connection.setUseStreamManagement(true);

        // by default Smack disconnects in case of parsing errors
        connection.setParsingExceptionCallback(new ExceptionLoggingCallback());

        ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(connection);
        reconnectionManager.enableAutomaticReconnection();
        reconnectionManager.setReconnectionPolicy(ReconnectionManager.ReconnectionPolicy.RANDOM_INCREASING_DELAY);
    }

    private void addConnectionListeners() {
        final Roster roster = Roster.getInstanceFor(connection);
        roster.addRosterListener(rosterListener);
        roster.addRosterLoadedListener(rosterListener);
        roster.setSubscriptionMode(Roster.SubscriptionMode.manual);
        roster.setRosterLoadedAtLogin(true);

        connection.addAsyncStanzaListener(everyStanzaListener, ForEveryStanza.INSTANCE);
        connection.addConnectionListener(this);
    }

    /**
     * Disconnect and connect using new connection.
     */
    public void forceReconnect() {
        if (!getState().isConnectable()) {
            return;
        }

        Thread thread = new Thread("Force reconnection thread for " + connection) {
            @Override
            public void run() {
                if (connection.isConnected()) {
                    connection.disconnect();
                    try {
                        connection.connect().login();
                    } catch (XMPPException | SmackException | InterruptedException | IOException e) {
                        LogManager.exception(this, e);
                    }
                }
            }

        };
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    public void reconnect() {
        if (state == ConnectionState.waiting) {
            Thread thread = new Thread("Reconnection thread for " + connection) {
                @Override
                public void run() {
                    try {
                        connection.connect().login();
                    } catch (XMPPException | SmackException | InterruptedException | IOException e) {
                        LogManager.exception(this, e);
                    }
                }

            };
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setDaemon(true);
            thread.start();
        }
    }

    /**
     * Starts disconnection in another thread.
     */
    protected static void disconnect(final AbstractXMPPConnection xmppConnection) {
        Thread thread = new Thread("Disconnection thread for " + xmppConnection) {
            @Override
            public void run() {
                xmppConnection.disconnect();
            }

        };
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    public abstract void onAuthFailed();

    /**
     * Update password.
     */
    protected void onPasswordChanged(String password) {
        connectionSettings.setPassword(password);
    }

    /**
     * New account has been registered on XMPP server.
     */
    protected void onAccountRegistered() {
        registerNewAccount = false;
        state = ConnectionState.authentication;
    }

    @Override
    public void connected(XMPPConnection connection) {

        if (isRegisterAccount()) {
            state = ConnectionState.registration;
        } else {
            state = ConnectionState.authentication;
        }

        ConnectionManager.getInstance().onConnected(this);
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        PingManager.getInstanceFor(connection).registerPingFailedListener(pingFailedListener);
        state = ConnectionState.connected;
        ConnectionManager.getInstance().onAuthorized(this, resumed);
    }

    @Override
    public void connectionClosed() {
        state = ConnectionState.offline;

        ConnectionManager.getInstance().onDisconnect(this);

        if (isConnectionRequestedByUser) {
            Application.getInstance().onError(R.string.CONNECTION_FAILED);
        }
        isConnectionRequestedByUser = false;

        AccountManager.getInstance().onAccountChanged(getAccount());
    }

    // going to reconnect with Smack Reconnection manager
    @Override
    public void connectionClosedOnError(final Exception e) {
        state = ConnectionState.waiting;
        AccountManager.getInstance().onAccountChanged(getAccount());

        PingManager.getInstanceFor(connection).unregisterPingFailedListener(pingFailedListener);
//        connectionClosed();
    }

    @Override
    public void reconnectionSuccessful() {
    }

    @Override
    public void reconnectingIn(int seconds) {
        if (state != ConnectionState.waiting) {
            state = ConnectionState.waiting;
            AccountManager.getInstance().onAccountChanged(getAccount());
        }
    }

    @Override
    public void reconnectionFailed(Exception e) {
    }

    private StanzaListener everyStanzaListener = new StanzaListener() {
        @Override
        public void processPacket(final Stanza stanza) throws SmackException.NotConnectedException {
            Application.getInstance().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ConnectionManager.getInstance().processPacket(ConnectionItem.this, stanza);
                }
            });

        }
    };

    private PingFailedListener pingFailedListener = new PingFailedListener() {
        @Override
        public void pingFailed() {
            LogManager.i(this, "pingFailed for " + getAccount());
//            forceReconnect();
        }
    };

}
