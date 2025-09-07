import { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
  StyleSheet,
  ScrollView,
} from 'react-native';
import { UploadVideoView } from '../UploadVideoView';
import { VideoView } from '../VideoView';
import { applyFilter, type Filter } from 'react-native-video-lab';

const FILTERS: {
  key: Filter;
  name: string;
  description: string;
  emoji: string;
}[] = [
  {
    key: 'sepia',
    name: 'Sepia',
    description: 'Vintage brown tone',
    emoji: '🟤',
  },
  {
    key: 'mono',
    name: 'Monochrome',
    description: 'Black and white',
    emoji: '⚫',
  },
  {
    key: 'invert',
    name: 'Invert',
    description: 'Inverted colors',
    emoji: '🔄',
  },
];

export const ApplyFilterView = () => {
  const [videoUri, setVideoUri] = useState<string | null>(null);
  const [selectedFilter, setSelectedFilter] = useState<Filter>('sepia');
  const [resultVideoUri, setResultVideoUri] = useState<string | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const handleApplyFilter = () => {
    if (!videoUri) {
      Alert.alert('No Video', 'Please select a video first');
      return;
    }

    setIsProcessing(true);

    applyFilter(videoUri, selectedFilter)
      .then((outputPath) => {
        setResultVideoUri(outputPath);
        Alert.alert('Success', 'Filter applied successfully!');
      })
      .catch((err) => {
        Alert.alert('Error', 'Failed to apply filter. Please try again.');
        console.error('Apply filter failed:', err);
      })
      .finally(() => {
        setIsProcessing(false);
      });
  };

  const onVideoUpload = (uri: string | null, _duration: number | undefined) => {
    setVideoUri(uri);
    setResultVideoUri(null); // Reset result when new video is selected
  };

  const resetApp = () => {
    setVideoUri(null);
    setResultVideoUri(null);
  };

  return (
    <>
      <UploadVideoView
        label="Select Video"
        videoUri={videoUri}
        onUpload={onVideoUpload}
      />

      {videoUri && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Choose Filter</Text>
          <Text style={styles.description}>
            Select a filter to apply to your video
          </Text>

          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            style={styles.filterScroll}
          >
            {FILTERS.map((filter) => (
              <TouchableOpacity
                key={filter.key}
                style={[
                  styles.filterOption,
                  selectedFilter === filter.key && styles.filterOptionActive,
                ]}
                onPress={() => setSelectedFilter(filter.key)}
              >
                <Text style={styles.filterEmoji}>{filter.emoji}</Text>
                <Text
                  style={[
                    styles.filterName,
                    selectedFilter === filter.key && styles.filterNameActive,
                  ]}
                >
                  {filter.name}
                </Text>
                <Text
                  style={[
                    styles.filterDescription,
                    selectedFilter === filter.key &&
                      styles.filterDescriptionActive,
                  ]}
                >
                  {filter.description}
                </Text>
              </TouchableOpacity>
            ))}
          </ScrollView>

          <TouchableOpacity
            style={[
              styles.applyButton,
              isProcessing && styles.applyButtonDisabled,
            ]}
            onPress={handleApplyFilter}
            disabled={isProcessing}
          >
            {isProcessing ? (
              <ActivityIndicator color="#fff" size="small" />
            ) : (
              <Text style={styles.applyButtonText}>🎨 Apply Filter</Text>
            )}
          </TouchableOpacity>
        </View>
      )}

      {/* Result Section */}
      {resultVideoUri && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>
            Result with {FILTERS.find((f) => f.key === selectedFilter)?.name}{' '}
            Filter
          </Text>
          <View style={styles.videoContainer}>
            <VideoView src={resultVideoUri} />
          </View>
        </View>
      )}

      {/* Reset Button */}
      {(videoUri || resultVideoUri) && (
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
    marginBottom: 20,
    textAlign: 'center',
  },
  filterScroll: {
    marginBottom: 20,
  },
  filterOption: {
    backgroundColor: '#f8f9fa',
    borderRadius: 12,
    padding: 16,
    marginRight: 12,
    alignItems: 'center',
    minWidth: 100,
    borderWidth: 2,
    borderColor: 'transparent',
  },
  filterOptionActive: {
    backgroundColor: '#e8f4fd',
    borderColor: '#3498db',
  },
  filterEmoji: {
    fontSize: 24,
    marginBottom: 8,
  },
  filterName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#2c3e50',
    marginBottom: 4,
  },
  filterNameActive: {
    color: '#3498db',
  },
  filterDescription: {
    fontSize: 12,
    color: '#7f8c8d',
    textAlign: 'center',
  },
  filterDescriptionActive: {
    color: '#5a6c7d',
  },
  applyButton: {
    backgroundColor: '#8e44ad',
    borderRadius: 12,
    paddingVertical: 16,
    paddingHorizontal: 24,
    alignItems: 'center',
    shadowColor: '#8e44ad',
    shadowOffset: {
      width: 0,
      height: 4,
    },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 4,
  },
  applyButtonDisabled: {
    backgroundColor: '#bdc3c7',
    shadowOpacity: 0,
    elevation: 0,
  },
  applyButtonText: {
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
