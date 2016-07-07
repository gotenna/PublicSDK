package com.gotenna.sdk.sample.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.gotenna.sdk.commands.GTCommand;
import com.gotenna.sdk.commands.GTCommandCenter;
import com.gotenna.sdk.commands.GTError;
import com.gotenna.sdk.interfaces.GTErrorListener;
import com.gotenna.sdk.responses.GTResponse;
import com.gotenna.sdk.sample.R;
import com.gotenna.sdk.sample.managers.ContactsManager;
import com.gotenna.sdk.sample.models.Contact;
import com.gotenna.sdk.types.GTDataTypes;
import com.gotenna.sdk.user.User;
import com.gotenna.sdk.user.UserDataStore;

import java.util.List;

public class SetGidActivity extends ChildActivity
{
    // ================================================================================
    // Class Properties
    // ================================================================================

    private TextView usernameTextView;
    private TextView gidTextView;

    // ================================================================================
    // Life-Cycle Methods
    // ================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_gid);

        usernameTextView = (TextView) findViewById(R.id.usernameTextView);
        gidTextView = (TextView) findViewById(R.id.gidTextView);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        refreshUI();
    }

    // ================================================================================
    // Button Click Methods
    // ================================================================================

    public void onChangeUserButtonClicked(View v)
    {
        // Show a dialog that allows the user to choose which pre-defined Contact they want to be
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
                Contact selectedContact = contacts.get(which);
                sendSetGidCommand(selectedContact.getName(), selectedContact.getGid());
                refreshUI();

                dialog.cancel();
            }
        });

        builder.show();
    }

    public void onSetGidButtonClicked(View v)
    {
        User currentUser = UserDataStore.getInstance().getCurrentUser();

        if (currentUser != null)
        {
            sendSetGidCommand(currentUser.getName(), currentUser.getGID());
        }
    }

    // ================================================================================
    // Class Instance Methods
    // ================================================================================

    private void refreshUI()
    {
        User currentUser = UserDataStore.getInstance().getCurrentUser();

        if (currentUser == null)
        {
            String noneText = getString(R.string.none);
            usernameTextView.setText(getString(R.string.current_username_text, noneText));
            gidTextView.setText(getString(R.string.current_gid_text, noneText));
        }
        else
        {
            usernameTextView.setText(getString(R.string.current_username_text, currentUser.getName()));
            gidTextView.setText(getString(R.string.current_gid_text, currentUser.getGID()));
        }
    }

    private void sendSetGidCommand(String username, long gid)
    {
        // The UserDataStore automatically saves the user's basic info after setGoTennaGID is called
        GTCommandCenter.getInstance().setGoTennaGID(gid, username, new GTCommand.GTCommandResponseListener()
        {
            @Override
            public void onResponse(GTResponse response)
            {
                String toastMessage = null;

                if (response.getResponseCode() == GTDataTypes.GTCommandResponseCode.POSITIVE)
                {
                    toastMessage = getString(R.string.set_gid_success_toast_text);
                }
                else
                {
                    toastMessage = getString(R.string.error_occurred);
                }

                Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_LONG).show();
            }
        }, new GTErrorListener()
        {
            @Override
            public void onError(GTError error)
            {
                Toast.makeText(getApplicationContext(), R.string.error_occurred, Toast.LENGTH_LONG).show();
            }
        });
    }
}
