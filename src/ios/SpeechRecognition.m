//
//  Created by jcesarmobile on 30/11/14.
//
//

#import "SpeechRecognition.h"
#import "ISpeechSDK.h"


@implementation SpeechRecognition


- (void) init:(CDVInvokedUrlCommand*)command {
    NSLog(@"init");
    NSString* key = @"developerdemokeydeveloperdemokey";
    iSpeechSDK *sdk = [iSpeechSDK sharedSDK];
    sdk.APIKey = key;
}


- (void) start:(CDVInvokedUrlCommand*)command {
    
    self.command = command;
    
    NSMutableDictionary * event = [[NSMutableDictionary alloc]init];
    [event setValue:@"start" forKey:@"type"];
    self.pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:event];
    [self.pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:self.pluginResult callbackId:self.command.callbackId];
    [self recognize];
    
}

- (void)recognize {
    ISSpeechRecognition *recognition = [[ISSpeechRecognition alloc] init];
    [recognition setDelegate:self];
    [recognition setFreeformType:ISFreeFormTypeDictation];
    
    NSError *error;
    
    if(![recognition listenAndRecognizeWithTimeout:10 error:&error]) {
        NSLog(@"ERROR: %@", error);
    }
}

- (void)recognition:(ISSpeechRecognition *)speechRecognition didGetRecognitionResult:(ISSpeechRecognitionResult *)result {
    
    NSMutableDictionary * resultDict = [[NSMutableDictionary alloc]init];
    [resultDict setValue:result.text forKey:@"transcript"];
    [resultDict setValue:[NSNumber numberWithBool:YES] forKey:@"final"];
    [resultDict setValue:[NSNumber numberWithFloat:result.confidence]forKey:@"confidence"];
    NSArray * alternatives = @[resultDict];
    NSArray * results = @[alternatives];
    
    NSMutableDictionary * event = [[NSMutableDictionary alloc]init];
    [event setValue:@"result" forKey:@"type"];
    [event setValue:nil forKey:@"emma"];
    [event setValue:nil forKey:@"interpretation"];
    [event setValue:results forKey:@"results"];
    
    self.pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:event];
    [self.pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:self.pluginResult callbackId:self.command.callbackId];
    
}

@end