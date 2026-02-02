import { View, StyleSheet, Text, Button } from 'react-native';
import { CustomLocale, MapxusButtonWrapperView, MapxusHsitpView } from 'react-native-mapxus-hsitp';
import { PermissionsAndroid, Platform, Alert } from 'react-native';
import { useEffect, useRef } from 'react';
import {
  NavigationContainer,
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
  // typed navigation omitted for brevity in example app
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const navigation: any = null;
  return (
    <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
      <Text>Home Screen</Text>

      <MapxusButtonWrapperView name="My Name" customLocale={CustomLocale.ZH_TW}>
        <Text 
          style={{ marginTop: 20, padding: 10, backgroundColor: '#007AFF', borderRadius: 5, color: 'white' }}
        >
          Go to Map
        </Text>
      </MapxusButtonWrapperView>
    </View>
  );
}

function BrokenScreen() {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const navigation: any = null;
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
        onPress={() => (navigation as any)?.navigate?.('Home')}
      />
    </View>
  );
}

const Stack = createNativeStackNavigator();

export default function App() {
  useEffect(() => {
    requestLocationPermissions();
  }, []);

  return (
    <NavigationContainer>
      <Stack.Navigator initialRouteName="Home">
        <Stack.Screen name="Home" component={HomeScreen} />
        <Stack.Screen name="Broken" component={BrokenScreen} />
      </Stack.Navigator>
    </NavigationContainer>
  );
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
