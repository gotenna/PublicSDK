package com.gotenna.sdk.sample.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.gotenna.sdk.sample.R;
import com.gotenna.sdk.sample.managers.ContactsManager;
import com.gotenna.sdk.sample.models.Contact;
import com.gotenna.sdk.sample.models.Message;
import com.gotenna.sdk.user.User;
import com.gotenna.sdk.user.UserDataStore;

import java.util.List;

public class PrivateMessageActivity extends MessageActivity
{
    // ================================================================================
    // Class Properties
    // ================================================================================

    private TextView receiverTextView;
    private Contact receiverContact;

    // ================================================================================
    // Life-Cycle Methods
    // ================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_message);

        receiverTextView = (TextView) findViewById(R.id.receiverTextView);

        // We want to encrypt private messages
        willEncryptMessages = true;
        willDisplayMessageStatus = true;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        refreshReceiverUI();
    }

    // ================================================================================
    // Button Click Methods
    // ================================================================================

    public void onChooseReceiverButtonClicked(View v)
    {
        // Show a dialog that allows the user to choose which pre-defined Contact they want to talk to.
        final List<Contact> contacts = ContactsManager.getInstance().getDemoContactsExcludingSelf();
        CharSequence[] choicesArray = new CharSequence[contacts.size()];

        for (int i = 0; i < contacts.size(); i++)
        {
            Contact contact = contacts.get(i);
            choicesArray[i] = String.format("%s - %d", contact.getName(), contact.getGid());
        }

        // Build and show the alert
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_user_dialog_title);

        builder.setSingleChoiceItems(choicesArray, -1, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                receiverContact = contacts.get(which);
                refreshReceiverUI();

                dialog.cancel();
            }
        });

        builder.show();
    }

    public void onSendMessageButtonClicked(View v)
    {
        if (receiverContact == null)
        {
            Toast toast = Toast.makeText(getApplicationContext(), R.string.invalid_receiver_toast_text, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        attemptToSendPrivateMessage(receiverContact.getGid());
    }

    // ================================================================================
    // Class Instance Methods
    // ================================================================================

    private void refreshReceiverUI()
    {
        String receiverName = receiverContact == null ? getString(R.string.none) : receiverContact.getName();
        receiverTextView.setText(getString(R.string.receiver_text, receiverName));
    }

    // ================================================================================
    // IncomingMessageListener Implementation
    // ================================================================================

    @Override
    public void onIncomingMessage(Message incomingMessage)
    {
        User currentUser = UserDataStore.getInstance().getCurrentUser();

        // Only display messages sent by the contact we selected as the receiver
        // and should be received directly by us
        if (receiverContact != null &&
                currentUser != null &&
                incomingMessage.getSenderGID() == receiverContact.getGid() &&
                incomingMessage.getReceiverGID() == currentUser.getGID())
        {
            messagesList.add(incomingMessage);
            updateMessagingUI();
        }
    }

}
