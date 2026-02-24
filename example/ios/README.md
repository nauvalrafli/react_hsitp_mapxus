# Mapxus HSITP iOS Example

This repository contains an iOS example app demonstrating the Mapxus HSITP integration and related Mapxus SDKs (MapxusBaseSDK, MapxusMapSDK, MapxusRenderSDK, MapxusVisualSDK, MapxusComponentKit).

Status: Generated README (Feb 24, 2026)

## Table of contents

- Project overview
- Requirements
- Installation
- Running the example
- Project structure
- Troubleshooting
- Contributing
- License

## Project overview

This project is an example iOS app (MapxusHsitpExample) showing how to integrate Mapxus HSITP and supporting Mapxus SDKs into an Xcode project using CocoaPods.

It includes pre-vendored Pod dependencies under the `Pods/` folder (this workspace includes a full Pod installation).

## Requirements

- macOS
- Xcode (recommend latest stable; the project was last inspected on Feb 24, 2026)
- CocoaPods (if you need to run `pod install` yourself)

## Installation

1. Clone the repository.

2. Open the workspace in Xcode:

    - If using CocoaPods (recommended):

      ```bash
      open MapxusHsitpExample.xcworkspace
      ```

    - If you prefer the project file (not recommended because Pods are used):

      ```bash
      open MapxusHsitpExample.xcodeproj
      ```

3. If you need to (re)install pods:

```bash
cd ios
pod install
```

Note: This workspace already contains a `Pods/` directory and `Podfile.lock` so pods appear vendored in the repository.

## Running the example

- Select the `MapxusHsitpExample` scheme in Xcode and run on your desired simulator or device.

- Or use xcodebuild from the command line:

```bash
xcodebuild -workspace MapxusHsitpExample.xcworkspace -scheme MapxusHsitpExample -configuration Debug -sdk iphonesimulator
```

## Project structure

- `MapxusHsitpExample/` — app sources (AppDelegate.swift, Info.plist, LaunchScreen.storyboard, Assets)
- `MapxusHsitpExample.xcodeproj/` and `MapxusHsitpExample.xcworkspace/` — Xcode project and workspace
- `Pods/` — CocoaPods dependencies (vendored)
- `Podfile`, `Podfile.lock` — CocoaPods manifest
- `build/`, `*.log` — build artifacts and logs included in the repo

## Troubleshooting

- If you encounter CocoaPods or build issues, try removing `Pods/` and `Podfile.lock` and running `pod install` again.
- Check the included `xcodebuild*.log` and `pod_install_attempt*.log` files in the `ios/` folder for diagnostic output.

## Contributing

- Create a branch: `git checkout -b feature/xyz`
- Open a pull request describing your change

## License

Check the repository root for a `LICENSE` file or add one if missing.
