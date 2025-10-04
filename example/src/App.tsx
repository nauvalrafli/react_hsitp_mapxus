import { View, StyleSheet } from 'react-native';
import { MapxusHsitpView } from 'react-native-mapxus-hsitp';
import { PermissionsAndroid, Platform, Alert } from 'react-native';
import { useEffect } from 'react';

async function requestLocationPermissions() {
  if (Platform.OS === 'android') {
    try {
      const granted = await PermissionsAndroid.requestMultiple([
        PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
        PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION,
      ]);

      const fineGranted =
        granted['android.permission.ACCESS_FINE_LOCATION'] === 'granted';
      const coarseGranted =
        granted['android.permission.ACCESS_COARSE_LOCATION'] === 'granted';

      if (!fineGranted && !coarseGranted) {
        Alert.alert(
          'Permission required',
          'Location permission is needed to use the map'
        );
      } else {
        console.log('Location permissions granted');
      }
    } catch (err) {
      console.warn('Failed to request location permissions', err);
    }
  }
}

export default function App() {
  useEffect(() => {
    requestLocationPermissions();
  }, []);

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
