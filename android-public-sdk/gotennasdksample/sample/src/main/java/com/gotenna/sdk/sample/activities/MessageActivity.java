package com.gotenna.sdk.sample.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.gotenna.sdk.bluetooth.GTConnectionManager;
import com.gotenna.sdk.commands.GTCommand.GTCommandResponseListener;
import com.gotenna.sdk.commands.GTCommandCenter;
import com.gotenna.sdk.commands.GTError;
import com.gotenna.sdk.gids.GIDManager;
import com.gotenna.sdk.interfaces.GTErrorListener;
import com.gotenna.sdk.responses.GTResponse;
import com.gotenna.sdk.sample.R;
import com.gotenna.sdk.sample.adapters.MessagesArrayAdapter;
import com.gotenna.sdk.sample.managers.IncomingMessagesManager;
import com.gotenna.sdk.sample.managers.IncomingMessagesManager.IncomingMessageListener;
import com.gotenna.sdk.sample.models.Message;
import com.gotenna.sdk.types.GTDataTypes;
import com.gotenna.sdk.user.User;
import com.gotenna.sdk.user.UserDataStore;
import com.gotenna.sdk.utils.Utils;

import java.util.ArrayList;

/**
 * An abstract activity that contains most of the logic for sending out messages
 * and updating the necessary UI.
 *
 * Created on 2/10/16
 *
 * @author ThomasColligan
 */
public abstract class MessageActivity extends ChildActivity implements IncomingMessageListener
{
    // ================================================================================
    // Class Properties
    // ================================================================================

    private static final int MESSAGE_RESEND_DELAY_MILLISECONDS = 5000;
    private static final String LOG_TAG = "MessageActivity";

    protected boolean willEncryptMessages;
    protected boolean willDisplayMessageStatus;
    protected Handler messageResendHandler;
    protected ArrayList<Message> messagesList;
    protected MessagesArrayAdapter messagesArrayAdapter;

    protected ListView messagesListView;
    protected EditText sendMessageEditText;

    // ================================================================================
    // Overridden Activity Methods
    // ================================================================================

    @Override
    protected void onPostCreate (Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);

        messageResendHandler = new Handler();
        messagesList = new ArrayList<>();

        messagesListView = (ListView) findViewById(R.id.messagesListView);
        sendMessageEditText = (EditText) findViewById(R.id.sendMessageEditText);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        updateMessagingUI();
        IncomingMessagesManager.getInstance().addIncomingMessageListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        IncomingMessagesManager.getInstance().removeIncomingMessageListener(this);
    }

    // ================================================================================
    // Class Instance Methods
    // ================================================================================

    protected void updateMessagingUI()
    {
        if (messagesArrayAdapter == null)
        {
            messagesArrayAdapter = new MessagesArrayAdapter(this, messagesList);
            messagesArrayAdapter.setWillDisplayMessageStatus(willDisplayMessageStatus);
            messagesListView.setAdapter(messagesArrayAdapter);
        }
        else
        {
            messagesArrayAdapter.notifyDataSetChanged();
        }
    }

    public void attemptToSendPrivateMessage(long receiverGID)
    {
        attemptToSendMessage(receiverGID, false);
    }

    public void attemptToSendBroadcastMessage()
    {
        attemptToSendMessage(GIDManager.SHOUT_GID, true);
    }

    public void attemptToSendGroupMessage(long groupGID)
    {
        attemptToSendMessage(groupGID, false);
    }

    private void attemptToSendMessage(long receiverGID, boolean isBroadcast)
    {
        User currentUser = UserDataStore.getInstance().getCurrentUser();

        if (currentUser == null)
        {
            Toast toast = Toast.makeText(getApplicationContext(), R.string.must_choose_user_toast_text, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();

            Intent intent = new Intent(this, SetGidActivity.class);
            startActivity(intent);
            return;
        }

        String messageText = sendMessageEditText.getText().toString();

        if (messageText.length() == 0)
        {
            return;
        }

        Message messageToSend = Message.createReadyToSendMessage(currentUser.getGID(), receiverGID, messageText);
        boolean didSend = sendMessage(messageToSend, isBroadcast);

        if (didSend)
        {
            sendMessageEditText.setText("");
            messagesList.add(messageToSend);
            updateMessagingUI();
        }
    }

    private boolean sendMessage(final Message message, final boolean isBroadcast)
    {
        if (message != null && message.toBytes() != null)
        {
            if (GTConnectionManager.getInstance().isConnected())
            {
                final GTCommandResponseListener responseListener = new GTCommandResponseListener()
                {
                    @Override
                    public void onResponse(GTResponse response)
                    {
                        // Parse the response we got about whether our message got through successfully
                        if (response.getResponseCode() == GTDataTypes.GTCommandResponseCode.POSITIVE)
                        {
                            message.setMessageStatus(Message.MessageStatus.SENT_SUCCESSFULLY);
                        }
                        else
                        {
                            message.setMessageStatus(Message.MessageStatus.ERROR_SENDING);
                        }

                        updateMessagingUI();
                    }
                };

                final GTErrorListener errorListener = new GTErrorListener()
                {
                    @Override
                    public void onError(GTError error)
                    {
                        if (error.getCode() == GTError.DATA_RATE_LIMIT_EXCEEDED)
                        {
                            Log.w(LOG_TAG, String.format("Data rate limit was exceeded. Resending message in %d seconds", MESSAGE_RESEND_DELAY_MILLISECONDS / Utils.MILLISECONDS_PER_SECOND));
                            final GTErrorListener localErrorListener = this;

                            // The goTenna SDK only allows you to send out so many messages within a 1 minute window.
                            // Try resending the message again later.
                            messageResendHandler.postDelayed(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    Log.i(LOG_TAG, "Resending message after data limit was exceeded");
                                    sendMessage(message, isBroadcast, responseListener, localErrorListener);
                                }
                            }, MESSAGE_RESEND_DELAY_MILLISECONDS);
                        }
                        else
                        {
                            message.setMessageStatus(Message.MessageStatus.ERROR_SENDING);
                            updateMessagingUI();

                            Log.w(LOG_TAG, error.toString());
                        }
                    }
                };

                // Actually send the message out
                sendMessage(message, isBroadcast, responseListener, errorListener);

                return true;
            }
            else
            {
                Toast toast = Toast.makeText(getApplicationContext(), R.string.gotenna_disconnected, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();

                return false;
            }
        }

        Toast toast = Toast.makeText(getApplicationContext(), R.string.error_occurred, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

        return false;
    }

    private void sendMessage(final Message message,
                             final boolean isBroadcast,
                             final GTCommandResponseListener responseListener,
                             final GTErrorListener errorListener)
    {
        // This is where we use the SDK to actually send the message out
        if (isBroadcast)
        {
            GTCommandCenter.getInstance().sendBroadcastMessage(message.toBytes(), responseListener, errorListener);
        }
        else
        {
            GTCommandCenter.getInstance().sendMessage(message.toBytes(),
                    message.getReceiverGID(),
                    responseListener,
                    errorListener,
                    willEncryptMessages);
        }
    }
}
