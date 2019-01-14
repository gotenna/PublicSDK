//
//  QueueManager.m
//  HelloGoTenna
//
//  Created by GoTenna on 8/3/17.
//  Copyright © 2018 goTenna. All rights reserved.
//

#import "QueueManager.h"
#import "MessagingManager.h"
#import "QueueItem.h"
#import "Message.h"
@import GoTennaSDK;

const float MESSAGE_RESEND_DELAY = 5.f;

@interface QueueManager ()

@property (nonatomic, strong) NSMutableArray<QueueItem *> *queue;
@property (nonatomic, strong) NSTimer *timer;
@property (nonatomic, assign) BOOL isSending;

@end

@implementation QueueManager

//MARK:- init

+ (instancetype)sharedManager {
    static QueueManager *manager = nil;
    static dispatch_once_t onceToken;
    
    dispatch_once(&onceToken, ^{
        manager = [self new];
    });
    
    return manager;
}

- (instancetype)init {
    self = [super init];
    
    if (self) {
        self.queue = [NSMutableArray array];
    }
    
    return self;
}

//MARK:- functions

- (void)addMessageToQueue:(Message *)message {
    QueueItem *item = [[QueueItem alloc] initWithMessage:message];
    
    if (![self.queue containsObject:item]) {
        [self.queue addObject:item];
        
        if (![self.timer isValid]) {
            [self startTimer];
        }
    }
}

- (void)removeMessageFromQueue:(Message *)message {
    QueueItem *item = [self queueItemForMessage:message];
    
    if ([self.queue containsObject:item]) {
        [self.queue removeObject:item];
    }
}

- (void)attemptToSendMessage {
    if (!self.isSending && [self.queue count] > 0) {
        [self.timer invalidate];
        self.isSending = YES;
        
        QueueItem *item = [self.queue firstObject];
        [self sendMessageFromQueue:item];
    } else {
        if ([self.queue count] < 1) {
            [self.timer invalidate];
            self.timer = nil;
        }
    }
}

- (void)attemptResendWithDelay {
    self.isSending = NO;
    
    [self.timer invalidate];
    [self startTimer];
}

- (void)markMessageAsSentAndSendNext:(QueueItem *)item {
    self.isSending = NO;
    
    [self removeMessageFromQueue:item.message];
    [self attemptToSendMessage];
}

//MARK:- send message

- (void)sendMessageFromQueue:(QueueItem *)item {
    Message *message = item.message;
    
    void (^onResponse)(GTResponse *) = ^(GTResponse *response) {
        self.isSending = NO;
        
        if ([response responsePositive]) {
            GTGIDType type = [GIDManager gidTypeForGID:message.receiverGID];
            
            if (type == OneToOneGID) {
                [message setHopCount:response.hopCount];
            }
            
            if ([self.delegate respondsToSelector:@selector(queueManager:didResendMessage:)]) {
                [self.delegate queueManager:self didResendMessage:message];
            }
            
            [self markMessageAsSentAndSendNext:item];
        }
    };
    
    void (^onError)(NSError *) = ^(NSError *error) {
        if (error) {
            if ([error isEqual:[GTError dataRateLimitExceeded]]) {
                [self attemptResendWithDelay];
            } else {
                [self markMessageAsSentAndSendNext:item];
            }
        }
    };
    
    if ([item isBroadcast]) {
        [[GTCommandCenter shared] sendBroadcast:[message toBytes] toGID:[GIDManager shoutGID] onResponse:onResponse onError:onError];
    } else {
        [[GTCommandCenter shared] sendMessage:[message toBytes] toGID:message.receiverGID fromGID:message.senderGID onResponse:onResponse onError:onError];
    }
}

# pragma mark - Timer

- (void)startTimer {
    self.timer = [NSTimer scheduledTimerWithTimeInterval:MESSAGE_RESEND_DELAY target:self selector:@selector(timerElapsed:) userInfo:nil repeats:YES];
    [[NSRunLoop mainRunLoop] addTimer:self.timer forMode:NSDefaultRunLoopMode];
}

- (void)timerElapsed:(NSTimer *)timer {
    [self attemptToSendMessage];
}

# pragma mark - Helpers

- (QueueItem *)queueItemForMessage:(Message *)message {
    for (QueueItem *item in [self.queue copy]) {
        if ([item.message isEqual:message]) {
            return item;
        }
    }
    
    return nil;
}

@end
