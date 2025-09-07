import { useState } from 'react';
import { Alert, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { VideoView } from './VideoView';
import { launchImageLibrary } from 'react-native-image-picker';

type Props = {
  videoUri: string | null;
  onUpload: (uri: string | null) => void;
};

export const UploadVideoView = (props: Props) => {
  const { videoUri, onUpload } = props;

  const [isUploading, setIsUploading] = useState(false);

  const handleUpload = () => {
    setIsUploading(true);

    launchImageLibrary(
      {
        mediaType: 'video',
        quality: 0.8,
      },
      (response) => {
        if (response.assets && response.assets[0]) {
          console.log('Uploaded video:', response.assets[0].uri);
          onUpload(response.assets[0].uri || null);
          setIsUploading(false);

          return;
        }

        if (response.didCancel) {
          setIsUploading(false);

          return;
        }

        Alert.alert('Error', 'Failed to upload video. Please try again.');
        console.error('Upload failed:', response);
        setIsUploading(false);
      }
    );
  };

  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>Select Video</Text>
      {!videoUri ? (
        <TouchableOpacity
          style={styles.uploadButton}
          onPress={handleUpload}
          disabled={isUploading}
        >
          <Text style={styles.uploadButtonText}>
            {isUploading ? 'Uploading...' : '📹 Choose Video'}
          </Text>
        </TouchableOpacity>
      ) : (
        <View style={styles.videoContainer}>
          <Text style={styles.videoLabel}>Original Video</Text>

          <VideoView src={videoUri} />

          <TouchableOpacity
            style={styles.changeButton}
            onPress={handleUpload}
            disabled={isUploading}
          >
            <Text style={styles.changeButtonText}>
              {isUploading ? 'Uploading...' : 'Change Video'}
            </Text>
          </TouchableOpacity>
        </View>
      )}
    </View>
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

  videoContainer: {
    alignItems: 'center',
  },
  videoLabel: {
    fontSize: 16,
    fontWeight: '500',
    color: '#34495e',
    marginBottom: 12,
  },
  video: {
    width: '100%',
    height: 200,
    borderRadius: 12,
    backgroundColor: '#ecf0f1',
  },

  uploadButton: {
    backgroundColor: '#3498db',
    borderRadius: 12,
    paddingVertical: 16,
    paddingHorizontal: 24,
    alignItems: 'center',
    shadowColor: '#3498db',
    shadowOffset: {
      width: 0,
      height: 4,
    },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 4,
  },
  uploadButtonText: {
    color: 'white',
    fontSize: 18,
    fontWeight: '600',
  },

  changeButton: {
    backgroundColor: '#95a5a6',
    borderRadius: 8,
    paddingVertical: 10,
    paddingHorizontal: 20,
    marginTop: 12,
  },
  changeButtonText: {
    color: 'white',
    fontSize: 14,
    fontWeight: '500',
  },
});
