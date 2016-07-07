//
// Created by Julietta Yaunches on 5/06/2014.
// Copyright (c) 2014 goTenna. All rights reserved.
//


#import <Foundation/Foundation.h>
#import "GTDataTypes.h"
#import "GTResponse.h"
#import "GTCommand.h"
#import "GTGroupCreationMessageData.h"

@class SystemInfoResponseData;
@class GTError;
@class FrequencyMode;
@class BinaryLogResponseData;
@class GTCommandArray;

/**
 * `GTCommandCenter` is the lifeblood of the goTenna economy, all commands to be sent to the goTenna must go through this class. This is the way in which one is to communicate with a goTenna
 */

@interface GTCommandCenter : NSObject
@property(nonatomic, copy) void (^onIncomingMessage)(GTMessageData *);

/**
 *  This class is a singleton. Use this method to get the global instance.
 *
 *  @return A shared instance
 */
+ (GTCommandCenter *)shared;

/**
 *  Sends an echo command to the connected goTenna.
 *  Upon receiving an echo the goTenna's LED will flash.
 *
 *  No actual message is transmitted when an echo is sent.
 *
 *  @param onResponse The response listener callback for the command.
 *  @param onError The error listener callback for the command.
 */
- (void)sendEchoCommand:(void (^)(GTResponse *))onResponse onError:(void (^)(NSError *))onError;

/**
 *  This method is used to set a goTenna's unique GID. This GID is used for one-to-one messaging.
 *  When this gets set, the previous one-to-one GID for the connected goTenna will be erased and the new
 *  GID set.
 *
 *  @param number   must be an NSNumber 15 digits or less
 *  @param username registers username for the user object in use (this can be empty if you wish)
 *  @param onError  required, called when an error occurs (See error code for details)
 */
- (void)setgoTennaGID:(NSNumber *)number withUsername:(NSString *)username onError:(void (^)(NSError *))onError;

/**
 *  With this you send a single message to another goTenna user. This is the only means of sending a message where you'll receive negative or positive confirmation that the receiver received your message. NOTE: must have set goTenna GID before calling this. NOTE: if your receiver needs to know the sender's GID, you'll need to send it in the payload

 *  @param messageData       must be 160 characters or less
 *  @param encryptionEnabled encryption is set here for the given message
 *  @param destinationGID    must be an NSNumber 15 digits or less, cannot be 111-111-1111
 *  @param senderGID         sender's gid to be put in here (it can be accessed through `UserDataStore` to retrieve the current user and then using -gID property on `User`)
 *  @param success           called when your goTenna responds, responseCode in GTResponse can be used to determine whether receiver received the message
 *  @param onError           required, called when an error occurs (See error code for details)
 */
- (void)sendMessage:(NSData *)messageData encrypt:(BOOL)encryptionEnabled toGID:(NSNumber *)destinationGID fromGID:(NSNumber *)senderGID onResponse:(void (^)(GTResponse *))success onError:(void (^)(NSError *))onError;

/**
 *  User to delete Group GIDs from your goTenna. AFter calling this, you should receive no further messages for the given GID. NOTE: if you call this with the goTenna's unique GID, you'll need to call setgoTennaGID before continueing to use your goTenna. You do NOT need to call this before updating your goTenna's unique GID
 *
 *  @param gidToDelete the GID that should be removed from
 *  @param onError     required, called when an error occurs (See error code for details)
 */
- (void)deleteGID:(NSNumber *)gidToDelete onError:(void (^)(NSError *))onError;

/**
 *  Data sent using this is sent to all goTennas in range. There is no confirmation of receipt of message by any goTennas
 *
 *  @param messageData    must be 160 characters or less
 *  @param destinationGID must be NSNumber 111-111-1111 (which can be accessed from the `GIDManager` method +shoutGID)
 *  @param success        called when your goTenna responds, responseCode in GTResponse can be used to determine whether receiver received the message
 *  @param onError        required, called when an error occurs (See error code for details)
 */
- (void)sendBroadcast:(NSData *)messageData toGID:(NSNumber*)destinationGID onResponse:(void (^)(GTResponse *))success onError:(void (^)(NSError *))onError;

/**
 *  Call this method to create a group. You must have the unique GIDs for all members of the group. When you call this, a one-to-one message is sent to each member of the group notifying them of group creation. NOTE: if the group is large, this method can take a while to process as each message goes out
 *
 *  @param memberGIDs       Cannot include 1111111111, Cannot include any other group GIDs, Can not exceed 10 members
 *  @param willEncrypt      encryption is set here for the given message
 *  @param onMemberResponse called for each member response to the group create message, responseCode in GTResponse can be used to determine whether receiver received the message (and subsequently knows about the group)
 *  @param senderGID        sender's gid to be put in here (it can be accessed through `UserDataStore` to retrieve the current user and then using -gID property on `User`)
 *  @param onError          required, called when an error occurs (See error code for details)
 *
 *  @return created groupGID
 */
- (NSNumber *)createGroupWithGIDs:(NSArray *)memberGIDs encrypt:(BOOL)willEncrypt onMemberResponse:(void (^)(GTResponse *, NSNumber *memberGID))onMemberResponse fromGID:(NSNumber*)senderGID onError:(void (^)(NSError *, NSNumber *))onError;

/**
 *  Set block to perform when receiving a group creation message. As a member of a created group, you need to know when you've been added to a group. Here, set the block to be called when you are added to a group. NOTE: you must have called setGotennaGID with your unique GID to receive these messages
 *
 *  @param externalOnGroupCreate called when you are added to a group, `GTGroupCreationMessageData` contains information about the group you've been added to
 */
- (void)setOnGroupCreated:(void (^)(GTGroupCreationMessageData *))externalOnGroupCreate;

/**
 *  Call this to retrieve goTenna system information
 *
 *  @param onSuccess called when your goTenna responds, responseCode in GTResponse can be used to determine whether receiver received the message, `SystemInfoResponseData` contains information about the goTenna
 *  @param onError   required, called when an error occurs (See error code for details)
 */
- (void)sendGetSystemInfoOnSuccess:(void (^)(SystemInfoResponseData *))onSuccess onError:(void (^)(NSError *))onError;

/**
 *  Send off app token that was set earlier
 *
 *  @param onResponse called when your goTenna responds, responseCode in GTResponse can be used to determine whether receiver received the message
 *  @param error      required, called when an error occurs (See error code for details)
 */
- (void)sendSetAppTokenOnResponse:(void (^)(GTResponse *))onResponse onError:(void (^)(NSError *))error;

/**
 *  Call to send a disconnect message to the goTenna
 */
- (void)sendDisconnectGotenna;

/**
 *  Resets the goTenna. NOTE: this tends to happen automatically at periods of time
 */
- (void)resetGotenna;

/**
 *  Sets the public key on the goTenna. NOTE: this happens behind the scenes automatically
 *
 *  @param publicKey public key data passed in
 */
- (void)setPublicKey:(NSData *)publicKey;

/**
 *  Queues up a command
 *
 *  @param command `GTCommand` gets queued up
 */
- (void)queueCommand:(GTCommand *)command;

/**
 *  Resets the internal queue
 */
- (void)resetQueue;

/**
 *  Handles incoming bluetooth transferred data. NOTE: this happens behind the scenes automatically
 *
 *  @param response data to be handled
 */
- (void)dispatchResponse:(NSMutableData *)response;

/**
 *  Pulls in any messages that are on the goTenna. NOTE: called automatically at important intervals
 */
- (void)sendGetMessageRequest;

/**
 *  Removes the current command (to be executed), command retrieved from the `CommandQueue`
 */
- (void)abortCurrentCommand;

/**
 *  Sends date and time to the goTenna. NOTE: handled automatically
 */
- (void)sendStoreDateTime;

/**
 *  Stops activity involving the sending of command objects/data in the queue
 */
- (void)pauseQueue;

/**
 *  Queues an array of priority commands
 *
 *  @param array array of commands that will be set to the front of the queue
 */
- (void)queuePriorityCommands:(GTCommandArray *)array;

/**
 *  Executes the next command in the queue. NOTE: handled automatically, no immediate requirement to call this method
 */
- (void)executeUpcomingQueueCommand;

/**
 *  Sends a reset command to the goTenna
 */
- (void)sendHardReset;

/**
 *  Sends off a public key request, to be responded with a public key
 *
 *  @param destinationGID requesting public key from this GID
 */
- (void)sendPublicKeyRequestToGID:(NSNumber *)destinationGID;

/**
 *  Sends off a public key, often as a response to a public key request
 *
 *  @param destinationGID sending public key to this GID
 */
- (void)sendPublicKeyResponseToGID:(NSNumber *)destinationGID;

@end
