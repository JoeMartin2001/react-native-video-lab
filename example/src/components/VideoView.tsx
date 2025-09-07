import {
  Alert,
  StyleSheet,
  type StyleProp,
  type ViewStyle,
} from 'react-native';
import Video from 'react-native-video';

type Props = {
  src: string;
  style?: StyleProp<ViewStyle>;
};

export const VideoView = (props: Props) => {
  const { src, style } = props;

  return (
    <Video
      source={{ uri: src }}
      style={[styles.video, style]}
      controls
      resizeMode="contain"
      paused
      onError={(error) => {
        Alert.alert('Video error:', error.error.errorString);
        console.error('Video error:', error);
      }}
    />
  );
};

const styles = StyleSheet.create({
  video: {
    width: '100%',
    height: 200,
  },
});
