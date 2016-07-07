package com.gotenna.sdk.sample.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gotenna.sdk.sample.R;
import com.gotenna.sdk.sample.models.Message;
import com.gotenna.sdk.user.User;
import com.gotenna.sdk.user.UserDataStore;

import java.util.ArrayList;

/**
 * Created on 2/10/16
 *
 * @author ThomasColligan
 */
public class MessagesArrayAdapter extends ArrayAdapter<Message>
{
    // ================================================================================
    // Class Properties
    // ================================================================================

    private static final int CHAT_MINE_TYPE = 0;
    private static final int CHAT_OTHER_TYPE = 1;
    private static final int TYPE_MAX_COUNT = CHAT_OTHER_TYPE  + 1;

    private ArrayList<Message> dataList;
    private boolean willDisplayMessageStatus;

    // ================================================================================
    // Constructor
    // ================================================================================

    public MessagesArrayAdapter(Context context, ArrayList<Message> dataList)
    {
        super(context, R.layout.cell_chat_mine, dataList);

        this.dataList = dataList;
    }

    // ================================================================================
    // Overridden ArrayAdapter Methods
    // ================================================================================

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View row = convertView;
        ChatCell cell = null;
        int type = getItemViewType(position);

        if (row == null)
        {
            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
            row = inflater.inflate(getLayoutForViewType(type), parent, false);

            cell = new ChatCell();
            cell.cellMessageTextView = (TextView) row.findViewById(R.id.cellMessageTextView);
            cell.cellInfoTextView = (TextView) row.findViewById(R.id.cellInfoTextView);
            cell.messageStatusImageView = (ImageView) row.findViewById(R.id.messageStatusImageView);

            row.setTag(cell);
        }
        else
        {
            cell = (ChatCell) row.getTag();
        }

        // Update the cell UI
        final Message messageToDisplay = dataList.get(position);

        cell.cellMessageTextView.setText(messageToDisplay.getText());
        cell.cellInfoTextView.setText(messageToDisplay.getDetailInfo() == null ?
                messageToDisplay.getSentDate().toString() :
                getContext().getString(R.string.chat_cell_info, messageToDisplay.getDetailInfo(), messageToDisplay.getSentDate().toString()));

        if (messageToDisplay.currentUserIsSender())
        {
            switch (messageToDisplay.getMessageStatus())
            {
                case SENDING:
                    cell.messageStatusImageView.setImageResource(R.drawable.sending_animation);
                    AnimationDrawable animationDrawable = (AnimationDrawable) cell.messageStatusImageView.getDrawable();
                    animationDrawable.start();
                    break;
                case SENT_SUCCESSFULLY:
                    cell.messageStatusImageView.setImageResource(R.drawable.ic_success);
                    break;
                case ERROR_SENDING:
                    cell.messageStatusImageView.setImageResource(R.drawable.ic_failed);
                    break;
            }

            cell.messageStatusImageView.setVisibility(willDisplayMessageStatus ? View.VISIBLE : View.GONE);
        }
        else
        {
            cell.messageStatusImageView.setVisibility(View.GONE);
        }


        return row;
    }

    @Override
    public int getViewTypeCount()
    {
        return TYPE_MAX_COUNT;
    }

    @Override
    public int getItemViewType(int position)
    {
        Message message = dataList.get(position);

        return messageIsMine(message) ? CHAT_MINE_TYPE : CHAT_OTHER_TYPE;
    }

    @Override
    public int getCount()
    {
        return dataList.size();
    }

    // ================================================================================
    // Class Instance Methods
    // ================================================================================

    public void setWillDisplayMessageStatus(boolean willDisplayMessageStatus)
    {
        this.willDisplayMessageStatus = willDisplayMessageStatus;
    }

    // ================================================================================
    // Class Helper Methods
    // ================================================================================

    private static int getLayoutForViewType(int type)
    {
        switch (type)
        {
            case CHAT_MINE_TYPE:
                return R.layout.cell_chat_mine;
            case CHAT_OTHER_TYPE:
                return R.layout.cell_chat_other;
        }

        return -1;
    }

    private static boolean messageIsMine(Message message)
    {
        User currentUser = UserDataStore.getInstance().getCurrentUser();

        if (currentUser != null)
        {
            if (message.getSenderGID() == currentUser.getGID())
            {
                return true;
            }
        }

        return false;
    }

    // ================================================================================
    // ChatCell Class
    // ================================================================================

    static class ChatCell
    {
        TextView cellMessageTextView;
        TextView cellInfoTextView;
        ImageView messageStatusImageView;
    }
}
