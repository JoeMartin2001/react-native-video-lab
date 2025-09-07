import {
  Text,
  View,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
} from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import { useState } from 'react';
import { UploadVideoView } from './components/UploadVideoView';
import { TrimVideoView } from './components/trim';

export default function App() {
  const [videoUri, setVideoUri] = useState<string | null>(null);
  const [trimmedVideoUri, setTrimmedVideoUri] = useState<string | null>(null);

  const resetApp = () => {
    setVideoUri(null);
    setTrimmedVideoUri(null);
  };

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.container}>
        <ScrollView
          contentContainerStyle={styles.scrollContainer}
          showsVerticalScrollIndicator={false}
        >
          <View style={styles.header}>
            <Text style={styles.title}>Video Lab</Text>
            <Text style={styles.subtitle}>Trim your videos with ease</Text>
          </View>

          {/* Upload Section */}
          <UploadVideoView videoUri={videoUri} onUpload={setVideoUri} />

          {/* Trim Section */}
          <TrimVideoView
            videoUri={videoUri}
            trimmedVideoUri={trimmedVideoUri}
            setTrimmedVideoUri={setTrimmedVideoUri}
          />

          {/* Reset Button */}
          {(videoUri || trimmedVideoUri) && (
            <TouchableOpacity style={styles.resetButton} onPress={resetApp}>
              <Text style={styles.resetButtonText}>🔄 Start Over</Text>
            </TouchableOpacity>
          )}
        </ScrollView>
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa',
  },
  scrollContainer: {
    flexGrow: 1,
    padding: 20,
  },
  header: {
    alignItems: 'center',
    marginBottom: 30,
    paddingTop: 20,
  },
  title: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#2c3e50',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    color: '#7f8c8d',
    textAlign: 'center',
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
