package com.gotenna.sdk.sample.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gotenna.sdk.commands.GTCommandCenter;
import com.gotenna.sdk.commands.GTCommandCenter.GTGroupInviteResponseListener;
import com.gotenna.sdk.commands.GTCommandCenter.GTGroupInviteErrorListener;
import com.gotenna.sdk.commands.GTError;
import com.gotenna.sdk.exceptions.GTDataMissingException;
import com.gotenna.sdk.responses.GTResponse;
import com.gotenna.sdk.sample.R;
import com.gotenna.sdk.sample.adapters.GroupInvitationArrayAdapter;
import com.gotenna.sdk.sample.adapters.GroupInvitationArrayAdapter.ResendInviteListener;
import com.gotenna.sdk.sample.managers.ContactsManager;
import com.gotenna.sdk.sample.models.Contact;
import com.gotenna.sdk.sample.models.GroupInvitation;
import com.gotenna.sdk.sample.models.GroupInvitation.GroupInvitationState;
import com.gotenna.sdk.types.GTDataTypes;
import com.gotenna.sdk.user.User;
import com.gotenna.sdk.user.UserDataStore;

import java.util.ArrayList;
import java.util.List;

public class CreateGroupActivity extends ChildActivity implements GTGroupInviteResponseListener, GTGroupInviteErrorListener, ResendInviteListener
{
    // ================================================================================
    // Class Properties
    // ================================================================================

    private static final String LOG_TAG = "CreateGroupActivity";

    private LinearLayout noGroupDetectedLayout;
    private LinearLayout groupsDetectedLayout;
    private TextView yourGroupGidTextView;
    private ListView groupMembersListView;

    private GroupCreationState groupCreationState;
    private long selectedGroupGID;
    private List<Long> groupMembersList;
    private List<GroupInvitation> groupInvitationList;
    private GroupInvitationArrayAdapter groupInvitationArrayAdapter;

    public enum GroupCreationState
    {
        NO_GROUPS_DETECTED,
        GROUPS_DETECTED,
        GROUP_SELECTED
    }

    // ================================================================================
    // Life-cycle Methods
    // ================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        noGroupDetectedLayout = (LinearLayout) findViewById(R.id.noGroupDetectedLayout);
        groupsDetectedLayout = (LinearLayout) findViewById(R.id.groupsDetectedLayout);
        yourGroupGidTextView = (TextView) findViewById(R.id.yourGroupGidTextView);
        groupMembersListView = (ListView) findViewById(R.id.groupMembersListView);

        groupInvitationList = new ArrayList<>();

        // Everyone we know of, including ourselves, will be in this group
        groupMembersList = new ArrayList<>();

        List<Contact> allContactsList = ContactsManager.getInstance().getAllDemoContacts();

        for (Contact contact : allContactsList)
        {
            groupMembersList.add(contact.getGid());
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        updateGroupCreationState();
        updateLayoutUI();
        updateInvitationListUI();
    }

    // ================================================================================
    // Button Click Methods
    // ================================================================================

    public void onCreateGroupButtonClicked(View v)
    {
        // Make sure we have a valid user first
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

        // Create a group invite for every group member besides ourselves
        List<Contact> allContactsList = ContactsManager.getInstance().getAllDemoContacts();
        groupInvitationList.clear();

        for (Contact contact : allContactsList)
        {
            long contactGID = contact.getGid();

            if (contactGID != currentUser.getGID())
            {
                groupInvitationList.add(new GroupInvitation(contact, GroupInvitationState.SENDING));
            }
        }

        try
        {
            // Send out all of the group invites via the goTenna
            selectedGroupGID = GTCommandCenter.getInstance().createGroupWithGIDs(groupMembersList, this, this);

            updateGroupCreationState();
            updateLayoutUI();
            updateInvitationListUI();
        }
        catch (GTDataMissingException e)
        {
            Log.w(LOG_TAG, e);
        }
    }

    public void onSelectGroupButtonClicked(View v)
    {
        // Make sure we have a valid user first
        final User currentUser = UserDataStore.getInstance().getCurrentUser();

        if (currentUser == null)
        {
            Toast toast = Toast.makeText(getApplicationContext(), R.string.must_choose_user_toast_text, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();

            Intent intent = new Intent(this, SetGidActivity.class);
            startActivity(intent);
            return;
        }

        // Show a dialog that allows the user to choose which group they want to send invites for
        final ArrayList<Long> groupGIDs = currentUser.getGroupGIDs();
        CharSequence[] choicesArray = new CharSequence[groupGIDs.size()];

        for (int i = 0; i < groupGIDs.size(); i++)
        {
            Long groupGID = groupGIDs.get(i);
            choicesArray[i] = groupGID.toString();
        }

        // Build and show the alert
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_group_dialog_title);

        builder.setSingleChoiceItems(choicesArray, -1, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                // The user selected a group to display
                selectedGroupGID = groupGIDs.get(which);

                // Setup the invites, pretending everyone was already invited
                // If we aren't the group creator we have no actual way of knowing who was successfully invited or not
                List<Contact> allContactsList = ContactsManager.getInstance().getAllDemoContacts();
                groupInvitationList.clear();

                for (Contact contact : allContactsList)
                {
                    if (contact.getGid() != currentUser.getGID())
                    {
                        groupInvitationList.add(new GroupInvitation(contact, GroupInvitationState.RECEIVED));
                    }
                }

                updateGroupCreationState();
                updateLayoutUI();
                updateInvitationListUI();

                dialog.cancel();
            }
        });

        builder.show();
    }

    // ================================================================================
    // Class Instance Methods
    // ================================================================================

    private void updateGroupCreationState()
    {
        groupCreationState = GroupCreationState.NO_GROUPS_DETECTED;
        User currentUser = UserDataStore.getInstance().getCurrentUser();

        if (currentUser != null)
        {
            ArrayList<Long> groupGIDs = currentUser.getGroupGIDs();

            if (groupGIDs.size() > 0)
            {
                groupCreationState = GroupCreationState.GROUPS_DETECTED;
            }
        }

        if (selectedGroupGID != 0)
        {
            groupCreationState = GroupCreationState.GROUP_SELECTED;
        }
    }

    private void updateLayoutUI()
    {
        switch (groupCreationState)
        {
            case NO_GROUPS_DETECTED:
            {
                noGroupDetectedLayout.setVisibility(View.VISIBLE);
                groupsDetectedLayout.setVisibility(View.GONE);
                yourGroupGidTextView.setVisibility(View.GONE);
            }
                break;
            case GROUPS_DETECTED:
            {
                noGroupDetectedLayout.setVisibility(View.GONE);
                groupsDetectedLayout.setVisibility(View.VISIBLE);
                yourGroupGidTextView.setVisibility(View.GONE);
            }
                break;
            case GROUP_SELECTED:
            {
                noGroupDetectedLayout.setVisibility(View.GONE);
                groupsDetectedLayout.setVisibility(View.GONE);
                yourGroupGidTextView.setVisibility(View.VISIBLE);

                yourGroupGidTextView.setText(getString(R.string.your_group_gid, selectedGroupGID));
            }
                break;
        }
    }

    private void updateInvitationListUI()
    {
        if (groupInvitationArrayAdapter == null)
        {
            groupInvitationArrayAdapter = new GroupInvitationArrayAdapter(this, groupInvitationList, this);
            groupMembersListView.setAdapter(groupInvitationArrayAdapter);
        }
        else
        {
            groupInvitationArrayAdapter.notifyDataSetChanged();
        }
    }

    // ================================================================================
    // GTGroupInviteResponseListener Implementation
    // ================================================================================

    @Override
    public void onMemberResponse(GTResponse response, long memberGID)
    {
        for (GroupInvitation groupInvitation : groupInvitationList)
        {
            if (groupInvitation.getContact().getGid() == memberGID)
            {
                if (response.getResponseCode() == GTDataTypes.GTCommandResponseCode.POSITIVE)
                {
                    groupInvitation.setGroupInvitationState(GroupInvitationState.RECEIVED);
                }
                else
                {
                    groupInvitation.setGroupInvitationState(GroupInvitationState.NOT_RECEIVED);
                }

                break;
            }
        }

        updateInvitationListUI();
    }

    // ================================================================================
    // GTGroupInviteErrorListener Implementation
    // ================================================================================

    @Override
    public void onError(GTError error, long memberGID)
    {
        Log.w(LOG_TAG, error.toString());

        for (GroupInvitation groupInvitation : groupInvitationList)
        {
            if (groupInvitation.getContact().getGid() == memberGID)
            {
                groupInvitation.setGroupInvitationState(GroupInvitationState.NOT_RECEIVED);
                break;
            }
        }

        updateInvitationListUI();
    }

    // ================================================================================
    // ResendInviteListener Implementation
    // ================================================================================

    @Override
    public void onResendInviteButtonClicked(GroupInvitation groupInvitationClicked)
    {
        try
        {
            // Send an individual group invite to this person again
            long contactGID = groupInvitationClicked.getContact().getGid();

            for (GroupInvitation groupInvitation : groupInvitationList)
            {
                if (groupInvitation.getContact().getGid() == contactGID)
                {
                    groupInvitation.setGroupInvitationState(GroupInvitationState.SENDING);
                    break;
                }
            }

            updateInvitationListUI();

            GTCommandCenter.getInstance().sendIndividualGroupInvite(selectedGroupGID,
                                                                    groupMembersList,
                                                                    contactGID,
                                                                    this,
                                                                    this);
        }
        catch (GTDataMissingException e)
        {
            Log.w(LOG_TAG, e);
        }
    }
}
