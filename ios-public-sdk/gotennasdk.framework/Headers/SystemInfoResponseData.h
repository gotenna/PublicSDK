#import <Foundation/Foundation.h>


@interface SystemInfoResponseData : NSObject
@property(nonatomic) double firmwareVersion;
@property(nonatomic) NSNumber *majorRevision;
@property(nonatomic) NSNumber *minorRevision;
@property(nonatomic) NSNumber *buildRevision;
@property(nonatomic) NSNumber *batteryLevel;
@property(nonatomic) NSNumber *paTemp;
@property(nonatomic) NSNumber *siLabsTemp;
@property(nonatomic) BOOL flashStatus;
@property(nonatomic, strong) NSData *rawData;
@property(nonatomic, strong) NSDate *dateCreated;
@property(nonatomic, copy) NSString *goTennaSerialNumber;

- (NSString *)asString;
- (NSNumber*)batteryLevelPercentage;
@end