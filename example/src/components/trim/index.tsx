import {
  ActivityIndicator,
  Alert,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { VideoView } from '../VideoView';
import { useState } from 'react';
import * as VideoLab from 'react-native-video-lab';

type Props = {
  videoUri: string | null;
  trimmedVideoUri: string | null;
  setTrimmedVideoUri: (uri: string | null) => void;
};

export const TrimVideoView = (props: Props) => {
  const { videoUri, trimmedVideoUri, setTrimmedVideoUri } = props;

  const [isProcessing, setIsProcessing] = useState(false);

  const handleTrim = () => {
    if (!videoUri) {
      Alert.alert('No Video', 'Please select a video first');
      return;
    }

    setIsProcessing(true);

    VideoLab.trim(videoUri, 0, 5)
      .then((outputPath) => {
        console.log('Trimmed video saved at:', outputPath);
        setTrimmedVideoUri(outputPath);
        Alert.alert('Success', 'Video trimmed successfully!');
      })
      .catch((err) => {
        Alert.alert('Error', 'Failed to trim video. Please try again.');
        console.error('Trim failed:', err);
      })
      .finally(() => {
        setIsProcessing(false);
      });
  };

  return (
    <>
      {videoUri && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Trim Video</Text>
          <Text style={styles.trimDescription}>
            Trim video to first 5 seconds
          </Text>
          <TouchableOpacity
            style={[
              styles.trimButton,
              isProcessing && styles.trimButtonDisabled,
            ]}
            onPress={handleTrim}
            disabled={isProcessing}
          >
            {isProcessing ? (
              <ActivityIndicator color="#fff" size="small" />
            ) : (
              <Text style={styles.trimButtonText}>✂️ Trim Video</Text>
            )}
          </TouchableOpacity>
        </View>
      )}

      {/* Result Section */}
      {trimmedVideoUri && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Trimmed Result</Text>
          <View style={styles.videoContainer}>
            <VideoView src={trimmedVideoUri} />
          </View>
        </View>
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

  trimDescription: {
    fontSize: 14,
    color: '#7f8c8d',
    marginBottom: 16,
    textAlign: 'center',
  },
  trimButton: {
    backgroundColor: '#e74c3c',
    borderRadius: 12,
    paddingVertical: 16,
    paddingHorizontal: 24,
    alignItems: 'center',
    shadowColor: '#e74c3c',
    shadowOffset: {
      width: 0,
      height: 4,
    },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 4,
  },
  trimButtonDisabled: {
    backgroundColor: '#bdc3c7',
    shadowOpacity: 0,
    elevation: 0,
  },
  trimButtonText: {
    color: 'white',
    fontSize: 18,
    fontWeight: '600',
  },

  videoContainer: {
    alignItems: 'center',
  },
});
