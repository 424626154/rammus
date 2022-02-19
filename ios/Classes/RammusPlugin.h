#import <Flutter/Flutter.h>
#import <UserNotifications/UserNotifications.h>
#import <CloudPushSDK/CloudPushSDK.h>
#define IS_IOS11_LATER ([[UIDevice currentDevice] systemVersion] >= 11)
@interface RammusPlugin : NSObject<FlutterPlugin,UNUserNotificationCenterDelegate>
@end
