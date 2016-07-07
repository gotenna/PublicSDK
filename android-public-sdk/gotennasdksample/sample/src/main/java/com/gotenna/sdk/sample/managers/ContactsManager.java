package com.gotenna.sdk.sample.managers;

import com.gotenna.sdk.sample.models.Contact;
import com.gotenna.sdk.user.User;
import com.gotenna.sdk.user.UserDataStore;

import java.util.ArrayList;
import java.util.List;

/**
 * A singleton with fake contacts that the user can select from for the demo.
 *
 * Created on 2/10/16
 *
 * @author ThomasColligan
 */
public class ContactsManager 
{
    // ================================================================================
    // Class Properties
    // ================================================================================

    private ArrayList<Contact> contactArrayList;

    // ================================================================================
    // Singleton Methods
    // ================================================================================

    private ContactsManager()
    {
        contactArrayList = new ArrayList<>();

        // Add demo contacts
        contactArrayList.add(new Contact("Alice", 123456789));
        contactArrayList.add(new Contact("Bob", 987654321));
        contactArrayList.add(new Contact("Carol", 1123581321));
        contactArrayList.add(new Contact("Doug", 314159265));
    }

    private static class SingletonHelper
    {
        private static final ContactsManager INSTANCE = new ContactsManager();
    }

    public static ContactsManager getInstance()
    {
        return SingletonHelper.INSTANCE;
    }

    // ================================================================================
    // Class Instance Methods
    // ================================================================================

    public List<Contact> getAllDemoContacts()
    {
        return contactArrayList;
    }

    public List<Contact> getDemoContactsExcludingSelf()
    {
        List<Contact> contactsExcludingSelfList = new ArrayList<>();

        User currentUser = UserDataStore.getInstance().getCurrentUser();
        long currentUserGid = currentUser == null ? 0 : currentUser.getGID();

        for (Contact contact : contactArrayList)
        {
            if (contact.getGid() != currentUserGid)
            {
                contactsExcludingSelfList.add(contact);
            }
        }

        return contactsExcludingSelfList;
    }

    public Contact findContactWithGid(long gid)
    {
        for (Contact contact : contactArrayList)
        {
            if (contact.getGid() == gid)
            {
                return contact;
            }
        }

        return null;
    }
}
