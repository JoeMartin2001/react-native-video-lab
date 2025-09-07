import { Text, View, StyleSheet, ScrollView } from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import { useState } from 'react';
import { TrimVideoView } from './components/trim';
import { RadioButton } from './components/RadioButton';
import { MergeVideoView } from './components/merge';
import { AddAudioView } from './components/add-audio';

const VideoLabMode = {
  Trim: 'Trim',
  Merge: 'Merge',
  AddAudio: 'AddAudio',
} as const;

export default function App() {
  const [videoLabMode, setVideoLabMode] = useState<keyof typeof VideoLabMode>(
    VideoLabMode.Trim
  );

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.container}>
        <ScrollView
          contentContainerStyle={styles.scrollContainer}
          showsVerticalScrollIndicator={false}
        >
          <View style={styles.header}>
            <Text style={styles.title}>Video Lab</Text>
            <Text style={styles.subtitle}>Edit your videos with ease</Text>
          </View>

          <View style={styles.modeSelector}>
            {Object.values(VideoLabMode).map((mode) => (
              <RadioButton
                key={mode}
                title={mode}
                isActive={videoLabMode === mode}
                onPress={() => setVideoLabMode(mode)}
              />
            ))}
          </View>

          {/* Trim Section */}
          {videoLabMode === VideoLabMode.Trim && <TrimVideoView />}

          {/* Merge Section */}
          {videoLabMode === VideoLabMode.Merge && <MergeVideoView />}

          {/* Add Audio Section */}
          {videoLabMode === VideoLabMode.AddAudio && <AddAudioView />}
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
  modeSelector: {
    marginBottom: 20,
  },
});
