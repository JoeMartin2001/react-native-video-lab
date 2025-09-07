import { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
  StyleSheet,
} from 'react-native';
import { UploadVideoView } from '../UploadVideoView';
import { UploadAudioView } from '../UploadAudioView';
import { VideoView } from '../VideoView';
import * as VideoLab from 'react-native-video-lab';

export const AddAudioView = () => {
  const [videoUri, setVideoUri] = useState<string | null>(null);
  const [audioUri, setAudioUri] = useState<string | null>(null);
  const [resultVideoUri, setResultVideoUri] = useState<string | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const handleAddAudio = () => {
    if (!videoUri || !audioUri) {
      Alert.alert('Missing Files', 'Please select both video and audio files');
      return;
    }

    setIsProcessing(true);

    VideoLab.addAudio(videoUri, audioUri, 'replace')
      .then((outputPath) => {
        setResultVideoUri(outputPath);
        Alert.alert('Success', 'Audio added to video successfully!');
      })
      .catch((err) => {
        Alert.alert('Error', 'Failed to add audio to video. Please try again.');
        console.error('Add audio failed:', err);
      })
      .finally(() => {
        setIsProcessing(false);
      });
  };

  const onVideoUpload = (uri: string | null, _duration: number | undefined) => {
    setVideoUri(uri);
  };

  const onAudioUpload = (uri: string | null) => {
    setAudioUri(uri);
  };

  const resetApp = () => {
    setVideoUri(null);
    setAudioUri(null);
    setResultVideoUri(null);
  };

  return (
    <>
      <UploadVideoView
        label="Select Video"
        videoUri={videoUri}
        onUpload={onVideoUpload}
      />

      <UploadAudioView
        label="Select Audio"
        audioUri={audioUri}
        onUpload={onAudioUpload}
      />

      {videoUri && audioUri && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Add Audio to Video</Text>
          <Text style={styles.description}>
            Combine the selected audio with the video
          </Text>
          <TouchableOpacity
            style={[
              styles.addAudioButton,
              isProcessing && styles.addAudioButtonDisabled,
            ]}
            onPress={handleAddAudio}
            disabled={isProcessing}
          >
            {isProcessing ? (
              <ActivityIndicator color="#fff" size="small" />
            ) : (
              <Text style={styles.addAudioButtonText}>🎵 Add Audio</Text>
            )}
          </TouchableOpacity>
        </View>
      )}

      {/* Result Section */}
      {resultVideoUri && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Result with Audio</Text>
          <View style={styles.videoContainer}>
            <VideoView src={resultVideoUri} />
          </View>
        </View>
      )}

      {/* Reset Button */}
      {(videoUri || audioUri || resultVideoUri) && (
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
  description: {
    fontSize: 14,
    color: '#7f8c8d',
    marginBottom: 16,
    textAlign: 'center',
  },
  addAudioButton: {
    backgroundColor: '#f39c12',
    borderRadius: 12,
    paddingVertical: 16,
    paddingHorizontal: 24,
    alignItems: 'center',
    shadowColor: '#f39c12',
    shadowOffset: {
      width: 0,
      height: 4,
    },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 4,
  },
  addAudioButtonDisabled: {
    backgroundColor: '#bdc3c7',
    shadowOpacity: 0,
    elevation: 0,
  },
  addAudioButtonText: {
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
