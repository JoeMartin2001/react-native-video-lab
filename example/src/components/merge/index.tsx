import { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
  StyleSheet,
} from 'react-native';
import { VideoView } from '../VideoView';
import { UploadVideoView } from '../UploadVideoView';
import * as VideoLab from 'react-native-video-lab';

export const MergeVideoView = () => {
  const [video1Uri, setVideo1Uri] = useState<string | null>(null);
  const [video2Uri, setVideo2Uri] = useState<string | null>(null);
  const [mergedVideoUri, setMergedVideoUri] = useState<string | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const handleMerge = () => {
    if (!video1Uri || !video2Uri) {
      Alert.alert('Missing Videos', 'Please select both videos to merge');
      return;
    }

    setIsProcessing(true);

    // Note: This is a placeholder - you'll need to implement the actual merge function
    VideoLab.merge([video1Uri, video2Uri])
      .then((outputPath) => {
        setMergedVideoUri(outputPath);
        Alert.alert('Success', 'Videos merged successfully!');
      })
      .catch((err) => {
        Alert.alert('Error', 'Failed to merge videos. Please try again.');
        console.error('Merge failed:', err);
      })
      .finally(() => {
        setIsProcessing(false);
      });
  };

  const onUpload1 = (uri: string | null, _duration: number | undefined) => {
    setVideo1Uri(uri);
  };

  const onUpload2 = (uri: string | null, _duration: number | undefined) => {
    setVideo2Uri(uri);
  };

  const resetApp = () => {
    setVideo1Uri(null);
    setVideo2Uri(null);
    setMergedVideoUri(null);
  };

  return (
    <>
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>First Video</Text>
        <UploadVideoView videoUri={video1Uri} onUpload={onUpload1} />
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Second Video</Text>
        <UploadVideoView videoUri={video2Uri} onUpload={onUpload2} />
      </View>

      {video1Uri && video2Uri && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Merge Videos</Text>
          <Text style={styles.mergeDescription}>
            Combine both videos into one
          </Text>
          <TouchableOpacity
            style={[
              styles.mergeButton,
              isProcessing && styles.mergeButtonDisabled,
            ]}
            onPress={handleMerge}
            disabled={isProcessing}
          >
            {isProcessing ? (
              <ActivityIndicator color="#fff" size="small" />
            ) : (
              <Text style={styles.mergeButtonText}>🔗 Merge Videos</Text>
            )}
          </TouchableOpacity>
        </View>
      )}

      {/* Result Section */}
      {mergedVideoUri && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Merged Result</Text>
          <View style={styles.videoContainer}>
            <VideoView src={mergedVideoUri} />
          </View>
        </View>
      )}

      {/* Reset Button */}
      {(video1Uri || video2Uri || mergedVideoUri) && (
        <TouchableOpacity style={styles.resetButton} onPress={resetApp}>
          <Text style={styles.resetButtonText}>🔄 Start Over</Text>
        </TouchableOpacity>
      )}
    </>
  );
};

const styles = StyleSheet.create({
  section: {
    backgroundColor: 'white',
    borderRadius: 16,
    padding: 20,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 3,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: '600',
    color: '#2c3e50',
    marginBottom: 16,
  },
  mergeDescription: {
    fontSize: 14,
    color: '#7f8c8d',
    marginBottom: 16,
    textAlign: 'center',
  },
  mergeButton: {
    backgroundColor: '#9b59b6',
    borderRadius: 12,
    paddingVertical: 16,
    paddingHorizontal: 24,
    alignItems: 'center',
    shadowColor: '#9b59b6',
    shadowOffset: {
      width: 0,
      height: 4,
    },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 4,
  },
  mergeButtonDisabled: {
    backgroundColor: '#bdc3c7',
    shadowOpacity: 0,
    elevation: 0,
  },
  mergeButtonText: {
    color: 'white',
    fontSize: 18,
    fontWeight: '600',
  },
  videoContainer: {
    alignItems: 'center',
  },
  resetButton: {
    backgroundColor: '#95a5a6',
    borderRadius: 12,
    paddingVertical: 14,
    paddingHorizontal: 24,
    alignItems: 'center',
    marginTop: 10,
  },
  resetButtonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '500',
  },
});
