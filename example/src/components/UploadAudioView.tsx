import { useState } from 'react';
import { Alert, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { pick, types } from '@react-native-documents/picker';

type Props = {
  label?: string;
  audioUri?: string | null;
  onUpload?: (uri: string | null) => void;
};

export const UploadAudioView = (props: Props) => {
  const { label = 'Select Audio', audioUri: propAudioUri, onUpload } = props;

  const [audioUri, setAudioUri] = useState<string | null>(propAudioUri || null);
  const [isUploading, setIsUploading] = useState(false);

  async function handleUpload() {
    setIsUploading(true);

    try {
      const [res] = await pick({
        type: types.audio,
      });

      const ALLOWED_EXTENSIONS = ['audio/mp4'];

      if (!ALLOWED_EXTENSIONS.includes(res.type!)) {
        return Alert.alert(
          `Only "${ALLOWED_EXTENSIONS.join(', ')}" audio files are allowed`
        );
      }

      setAudioUri(res.uri);
      onUpload?.(res.uri);

      console.log('Picked audio:', res.uri);
    } catch (err) {
      console.error('Error picking audio:', err);
    } finally {
      setIsUploading(false);
    }
  }

  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>{label}</Text>
      {!audioUri ? (
        <TouchableOpacity
          style={styles.uploadButton}
          onPress={handleUpload}
          disabled={isUploading}
        >
          <Text style={styles.uploadButtonText}>
            {isUploading ? 'Uploading...' : '📹 Choose Audio'}
          </Text>
        </TouchableOpacity>
      ) : (
        <View style={styles.audioContainer}>
          <Text style={styles.audioLabel}>Selected Audio</Text>
          <Text style={styles.audioUri} numberOfLines={2}>
            {audioUri?.split('/').pop() || 'Unknown file'}
          </Text>

          <TouchableOpacity
            style={styles.changeButton}
            onPress={handleUpload}
            disabled={isUploading}
          >
            <Text style={styles.changeButtonText}>
              {isUploading ? 'Uploading...' : 'Change Audio'}
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

  audioContainer: {
    alignItems: 'center',
  },
  audioLabel: {
    fontSize: 16,
    fontWeight: '500',
    color: '#34495e',
    marginBottom: 8,
  },
  audioUri: {
    fontSize: 14,
    color: '#7f8c8d',
    textAlign: 'center',
    marginBottom: 12,
    paddingHorizontal: 16,
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
