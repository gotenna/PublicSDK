package com.gotenna.sdk.sample.models;

import android.util.Log;

import com.gotenna.sdk.exceptions.GTDataMissingException;
import com.gotenna.sdk.messages.GTBaseMessageData;
import com.gotenna.sdk.messages.GTTextOnlyMessageData;
import com.gotenna.sdk.sample.managers.ContactsManager;
import com.gotenna.sdk.user.User;
import com.gotenna.sdk.user.UserDataStore;

import java.util.Date;

/**
 * A model class that represent a sent or received message.
 *
 * Created on 2/10/16
 *
 * @author ThomasColligan
 */
public class Message
{
    // ================================================================================
    // Class Properties
    // ================================================================================

    private static final String LOG_TAG = "Message";

    private long senderGID;
    private long receiverGID;
    private Date sentDate;
    private String text;
    private MessageStatus messageStatus;
    private String detailInfo;

    public enum MessageStatus
    {
        SENDING,
        SENT_SUCCESSFULLY,
        ERROR_SENDING
    }

    // ================================================================================
    // Constructor
    // ================================================================================

    public Message(long senderGID, long receiverGID, Date sentDate, String text, MessageStatus messageStatus, String detailInfo)
    {
        this.senderGID = senderGID;
        this.receiverGID = receiverGID;
        this.sentDate = sentDate;
        this.text = text;
        this.messageStatus = messageStatus;
        this.detailInfo = detailInfo;
    }

    // ================================================================================
    // Class Instance Methods
    // ================================================================================

    public long getSenderGID()
    {
        return senderGID;
    }

    public long getReceiverGID()
    {
        return receiverGID;
    }

    public Date getSentDate()
    {
        return sentDate;
    }

    public String getText()
    {
        return text;
    }

    public MessageStatus getMessageStatus()
    {
        return messageStatus;
    }

    public void setMessageStatus(MessageStatus messageStatus)
    {
        this.messageStatus = messageStatus;
    }

    public String getDetailInfo()
    {
        return detailInfo;
    }

    public byte[] toBytes()
    {
        // Use the goTenna SDK's helper classes to format the text data
        // in a way that is easily parsable
        GTTextOnlyMessageData gtTextOnlyMessageData = null;

        try
        {
            gtTextOnlyMessageData = new GTTextOnlyMessageData(text);
        }
        catch (GTDataMissingException e)
        {
            Log.w(LOG_TAG, e);
        }

        if (gtTextOnlyMessageData == null)
        {
            return null;
        }

        return gtTextOnlyMessageData.serializeToBytes();
    }

    public boolean currentUserIsSender()
    {
        User currentUser = UserDataStore.getInstance().getCurrentUser();

        if (currentUser != null)
        {
            return senderGID == currentUser.getGID();
        }

        return false;
    }

    // ================================================================================
    // Static Helper Methods
    // ================================================================================

    public static Message createReadyToSendMessage(long senderGID, long receiverGID, String text)
    {
        return new Message(senderGID, receiverGID, new Date(), text, MessageStatus.SENDING, null);
    }

    public static Message createMessageFromData(GTTextOnlyMessageData gtTextOnlyMessageData)
    {
        return new Message(gtTextOnlyMessageData.getSenderGID(),
                            gtTextOnlyMessageData.getRecipientGID(),
                            gtTextOnlyMessageData.getMessageSentDate(),
                            gtTextOnlyMessageData.getText(),
                            MessageStatus.SENT_SUCCESSFULLY,
                            getDetailInfo(gtTextOnlyMessageData));
    }

    private static String getDetailInfo(GTBaseMessageData gtBaseMessageData)
    {
        Contact contact = ContactsManager.getInstance().findContactWithGid(gtBaseMessageData.getSenderGID());
        String senderInitials = gtBaseMessageData.getSenderInitials();

        if (contact != null)
        {
            return contact.getName();
        }
        else if (senderInitials != null)
        {
            return senderInitials;
        }
        else
        {
            return Long.toString(gtBaseMessageData.getSenderGID());
        }
    }
}
