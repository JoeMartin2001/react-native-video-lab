import { TouchableOpacity, Text, View, StyleSheet } from 'react-native';

type Props = {
  title: string;
  isActive: boolean;
  onPress: () => void;
};

export const RadioButton = (props: Props) => {
  const { title, isActive, onPress } = props;

  return (
    <TouchableOpacity
      style={[styles.container, isActive && styles.containerActive]}
      onPress={onPress}
    >
      <View style={styles.content}>
        <Text style={[styles.title, isActive && styles.titleActive]}>
          {title}
        </Text>
        <Text
          style={[styles.description, isActive && styles.descriptionActive]}
        >
          {title === 'Trim'
            ? 'Cut video to specific duration'
            : title === 'Merge'
              ? 'Combine multiple videos'
              : 'Add audio track to video'}
        </Text>
      </View>
      <View style={[styles.radioButton, isActive && styles.radioButtonActive]}>
        {isActive && <View style={styles.radioButtonInner} />}
      </View>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'white',
    borderRadius: 16,
    padding: 20,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 3,
    borderWidth: 2,
    borderColor: 'transparent',
  },
  containerActive: {
    borderColor: '#3498db',
    backgroundColor: '#f8f9ff',
  },
  content: {
    flex: 1,
    marginRight: 16,
  },
  title: {
    fontSize: 18,
    fontWeight: '600',
    color: '#2c3e50',
    marginBottom: 4,
  },
  titleActive: {
    color: '#3498db',
  },
  description: {
    fontSize: 14,
    color: '#7f8c8d',
    lineHeight: 20,
  },
  descriptionActive: {
    color: '#5a6c7d',
  },
  radioButton: {
    width: 24,
    height: 24,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: '#bdc3c7',
    alignItems: 'center',
    justifyContent: 'center',
  },
  radioButtonActive: {
    borderColor: '#3498db',
  },
  radioButtonInner: {
    width: 12,
    height: 12,
    borderRadius: 6,
    backgroundColor: '#3498db',
  },
});
