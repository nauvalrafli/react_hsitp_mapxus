import { View, StyleSheet, Text, Button } from 'react-native';
import { CustomLocale, MapxusButtonWrapperView, MapxusHsitpView } from 'react-native-mapxus-hsitp';
import { PermissionsAndroid, Platform, Alert } from 'react-native';
import { useEffect, useRef } from 'react';
import {
  createStaticNavigation,
  useNavigation,
} from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';

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

function HomeScreen() {
  const navigation = useNavigation();
  return (
    <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
      <Text>Home Screen</Text>
      <Button
        title="Go to Broken Screen"
        onPress={() => navigation.navigate('Broken' as never)}
      />

      <MapxusButtonWrapperView customLocale={CustomLocale.ZH_TW}>
        <Text>Open Map</Text>
      </MapxusButtonWrapperView>
    </View>
  );
}

function BrokenScreen() {
  const navigation = useNavigation();
  const ref = useRef(null);
  const localeKey = 'en-US';

  useEffect(() => {
    if (ref.current) {
      (ref.current as any).setNativeProps({ customLocale: 'zh-TW' });
    }
  }, []);

  return (
    <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
      <MapxusHsitpView
        key={Date.now()}
        color="#ababab"
        customLocale={localeKey}
        style={styles.box}
        ref={ref}
      />
      <Button
        title="Go to Home Screen"
        onPress={() => navigation.navigate('Home' as never)}
      />
    </View>
  );
}

const RootStack = createNativeStackNavigator({
  screens: {
    Home: HomeScreen,
    Broken: BrokenScreen,
  },
});

const Navigation = createStaticNavigation(RootStack);

export default function App() {
  useEffect(() => {
    requestLocationPermissions();
  }, []);

  return <Navigation />;
  // return (
  //   <View style={styles.container}>
  //     <MapxusHsitpView color="#32a852" style={styles.box} />
  //   </View>
  // );
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
