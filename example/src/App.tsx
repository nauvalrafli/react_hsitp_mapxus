import { View, StyleSheet } from 'react-native';
import { MapxusHsitpView } from 'react-native-mapxus-hsitp';

export default function App() {
  return (
    <View style={styles.container}>
      <MapxusHsitpView color="#32a852" style={styles.box} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: '100%',
    height: '100%',
  },
});
