# react-native-mapxus-hsitp

React Native native module and Fabric component bindings for Mapxus HSITP features (iOS).

This repository provides the native iOS implementation and example project used to expose a Fabric view component to React Native JavaScript. The native component is currently a lightweight placeholder that demonstrates how to wire props from JS to native (for example, a `color` prop that controls the view background).

## Contents

- ios/ - iOS native source and the Xcode example app (MapxusHsitpExample).
- build/generated/ios/... - Codegen-generated bindings and Props for Fabric components (do not edit manually).
- example/ - (If present) React Native example app wrapper used by the Xcode project.

## Quick overview

- Podspec: `example/ios/Pods/Local Podspecs/MapxusHsitp.podspec.json` (version 0.1.21 in the example).
- Platforms: iOS 15.1+
- License: MIT

## Installation

This package is distributed as a native iOS pod. There are two common ways to use it in a React Native project:

1) Add it from the remote Git repository (recommended for consumers):

```ruby
# In your ios/Podfile
pod 'MapxusHsitp', :git => 'https://github.com/nauvalrafli/react-native-mapxus-hsitp.git', :tag => '0.1.21'
```

2) Use the local path for development (when working on the library locally):

```ruby
# In your ios/Podfile (adjust the path to where you cloned this repo)
pod 'MapxusHsitp', :path => '../path-to/react-native-mapxus-hsitp/ios'
```

After adding the pod, run:

```bash
cd ios
pod install
```

Open the generated workspace (xcworkspace) in Xcode and build.

Important: This library enables the new React Native architecture flags (Fabric/codegen). Make sure your app is configured for the new architecture if you plan to use these components.

## Usage (JavaScript)

If the library's JS module is available via codegen, you can import the component directly. If not, you can use `requireNativeComponent` as a fallback:

```jsx
import React from 'react';
import {requireNativeComponent} from 'react-native';

const MapxusHsitpView = requireNativeComponent('MapxusHsitpView');

export default function App() {
  return (
    <MapxusHsitpView style={{width: 300, height: 200}} color="#FF5733" customLocale="en-US" />
  );
}
```

## API / Props

Based on the generated props (see `build/generated/ios/.../Props.h`), the following props are available:

- MapxusHsitpView
  - color (string): Hex color string such as `#RRGGBB`. The native iOS implementation converts this to a UIColor and applies it to the view background.
  - customLocale (string): Locale identifier used by the native module for localization (implementation-specific).

- MapxusButtonWrapperView
  - customLocale (string)
  - name (string)

Note: The native implementation in `ios/MapxusHsitpView.mm` currently displays a placeholder label "Under Development" — the native behavior can be extended as needed.

## Building and running the iOS example app

The repository includes an Xcode example project under `example/ios/MapxusHsitpExample`. To run it:

1. (Optional) If the example has a JavaScript layer with dependencies, from the repo root or example folder run:

```bash
# If example contains a package.json
# cd example && npm install   or   yarn install
```

2. Install pods for the example iOS project and open the workspace:

```bash
cd example/ios
pod install
open MapxusHsitpExample.xcworkspace
```

3. Build & Run from Xcode on a simulator or device.

If Metro bundler is needed by the example app, start it from the example root:

```bash
npx react-native start
```

## Development notes

- Native view implementation:
  - `ios/MapxusHsitpView.mm` — Objective-C++ implementation of the Fabric component. The file demonstrates how props are parsed and used to update native view state (color conversion helper included).
- Generated code:
  - `build/generated/ios/react/renderer/components/MapxusHsitpSpec/...` — codegen-generated Props / ShadowNodes / ComponentDescriptors. Do not edit these files manually.
- Podspec:
  - `example/ios/Pods/Local Podspecs/MapxusHsitp.podspec.json` contains the pod metadata used in the example workspace.

## Contributing

Contributions are welcome. Suggested workflow:

1. Fork the repo.
2. Create a feature branch.
3. Run and update the example app to ensure native changes behave as expected.
4. Open a PR and describe the changes.

Please update the README when adding public props or changing behaviors.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
