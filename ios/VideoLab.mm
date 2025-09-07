#import "VideoLab.h"
#import <React/RCTUtils.h>
#import <AVFoundation/AVFoundation.h>

@implementation VideoLab

// Register the module for both old & new arch
RCT_EXPORT_MODULE()

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
  return std::make_shared<facebook::react::NativeVideoLabSpecJSI>(params);
}

#pragma mark - Spec methods

// TS: trim(path: string, start: number, end: number): Promise<string>
- (void)trim:(NSString *)path
       start:(double)start   // must be double (not NSNumber)
         end:(double)end     // must be double
     resolve:(RCTPromiseResolveBlock)resolve
      reject:(RCTPromiseRejectBlock)reject
{
  NSLog(@"[VideoLab] trim called with path: %@, start: %f, end: %f", path, start, end);

//   // Placeholder: return fake output path
//   NSString *outputPath = [NSTemporaryDirectory() stringByAppendingPathComponent:@"trimmed.mp4"];
//   resolve(outputPath);

  // Convert JS uri into NSURL
  NSURL *sourceURL = [NSURL URLWithString:path];
  if (![sourceURL isFileURL]) {
    // react-native-image-picker often returns "file://..." which works
    // but if it’s "ph://..." (PhotoKit), you need to export that first
    sourceURL = [NSURL fileURLWithPath:path];
  }

  AVAsset *asset = [AVAsset assetWithURL:sourceURL];
  if (!asset) {
    reject(@"LOAD_ERROR", @"Failed to load video asset", nil);
    return;
  }

  // Configure export session
  AVAssetExportSession *exportSession =
    [[AVAssetExportSession alloc] initWithAsset:asset presetName:AVAssetExportPresetHighestQuality];

  NSString *outputPath = [NSTemporaryDirectory() stringByAppendingPathComponent:@"trimmed.mp4"];
  NSURL *outputURL = [NSURL fileURLWithPath:outputPath];

  // Remove existing file
  [[NSFileManager defaultManager] removeItemAtURL:outputURL error:nil];

  exportSession.outputURL = outputURL;
  exportSession.outputFileType = AVFileTypeMPEG4;

  // Define the time range
  CMTime startTime = CMTimeMakeWithSeconds(start, asset.duration.timescale);
  CMTime endTime = CMTimeMakeWithSeconds(end, asset.duration.timescale);
  exportSession.timeRange = CMTimeRangeFromTimeToTime(startTime, endTime);

  [exportSession exportAsynchronouslyWithCompletionHandler:^{
    dispatch_async(dispatch_get_main_queue(), ^{
      if (exportSession.status == AVAssetExportSessionStatusCompleted) {
        NSLog(@"[VideoLab] ✅ Trim success: %@", outputPath);
        resolve(outputPath);
      } else {
        NSLog(@"[VideoLab] ❌ Trim failed: %@", exportSession.error);
        reject(@"EXPORT_FAILED", @"Failed to trim video", exportSession.error);
      }
    });
  }];
}

// TS: merge(paths: string[]): Promise<string>
- (void)merge:(NSArray *)paths
      resolve:(RCTPromiseResolveBlock)resolve
       reject:(RCTPromiseRejectBlock)reject
{
  NSLog(@"[VideoLab] merge called with paths: %@", paths);

  NSString *outputPath = [NSTemporaryDirectory() stringByAppendingPathComponent:@"merged.mp4"];
  resolve(outputPath);
}

// TS: addAudio(videoPath: string, audioPath: string): Promise<string>
- (void)addAudio:(NSString *)videoPath
       audioPath:(NSString *)audioPath
        resolve:(RCTPromiseResolveBlock)resolve
         reject:(RCTPromiseRejectBlock)reject
{
  NSLog(@"[VideoLab] addAudio called with videoPath: %@, audioPath: %@", videoPath, audioPath);

  NSString *outputPath = [NSTemporaryDirectory() stringByAppendingPathComponent:@"with-audio.mp4"];
  resolve(outputPath);
}

// TS: applyFilter(path: string, filter: 'sepia' | 'mono' | 'invert'): Promise<string>
- (void)applyFilter:(NSString *)path
             filter:(NSString *)filter
            resolve:(RCTPromiseResolveBlock)resolve
             reject:(RCTPromiseRejectBlock)reject
{
  NSLog(@"[VideoLab] applyFilter called with path: %@, filter: %@", path, filter);

  NSString *outputPath = [NSTemporaryDirectory() stringByAppendingPathComponent:@"filtered.mp4"];
  resolve(outputPath);
}

@end
