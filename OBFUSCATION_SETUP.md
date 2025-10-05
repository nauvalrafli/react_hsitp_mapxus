# Obfuscation Setup for npm Publishing

This document explains the obfuscation setup implemented for publishing the `react-native-mapxus-hsitp` module to npm with obfuscated JavaScript code.

## Overview

The module is configured to:
1. **Obfuscate JavaScript code** using `javascript-obfuscator`
2. **Only publish compiled code** to npm (source code excluded)
3. **Maintain TypeScript definitions** for proper type checking
4. **Clean up source maps** from the published package

## Configuration Files

### 1. `obfuscator.config.js`
Contains the obfuscation configuration with settings optimized for React Native:
- **Control flow flattening** to make code harder to follow
- **String array encoding** with base64 encoding
- **Identifier name obfuscation** using hexadecimal names
- **Reserved names** for React Native compatibility
- **Dead code injection** to add complexity

### 2. `.npmignore`
Excludes source files and development artifacts from the npm package:
- Source files (`src/`, `example/`)
- Build configurations
- Development tools
- Test files
- Source maps

### 3. Updated `package.json`
Modified build scripts and files configuration:
- `files` array includes only compiled code
- Build scripts for obfuscation workflow
- `prepublishOnly` hook ensures obfuscation before publishing

## Build Process

The build process follows these steps:

1. **Clean** previous builds
2. **Build Android AAR** (`yarn build:android`)
3. **Build TypeScript definitions** (`yarn build:types`)
4. **Build JavaScript modules** (`yarn build:js`)
5. **Obfuscate JavaScript** (`yarn obfuscate`)
6. **Clean source maps** (`yarn clean:source-maps`)

### Available Scripts

```bash
# Build everything with obfuscation
yarn build:production

# Individual build steps
yarn build:android    # Build Android AAR
yarn build:types      # Generate TypeScript definitions
yarn build:js         # Compile JavaScript modules
yarn obfuscate        # Obfuscate JavaScript code
yarn clean:source-maps # Remove source maps

# Test package before publishing
yarn test:package

# Publish to npm
npm publish
```

## Package Structure

The published package contains:

```
react-native-mapxus-hsitp/
├── lib/
│   ├── module/
│   │   ├── index.js (obfuscated)
│   │   └── MapxusHsitpViewNativeComponent.ts
│   └── typescript/
│       └── src/
│           ├── index.d.ts
│           └── MapxusHsitpViewNativeComponent.d.ts
├── android/
│   └── libs/
│       └── react-native-mapxus-hsitp-release.aar (compiled)
├── ios/
├── MapxusHsitp.podspec
├── README.md
├── LICENSE
└── package.json
```

## Obfuscation Features

The obfuscated code includes:

- **Mangled variable names** (e.g., `a0_0x3722`)
- **String array encoding** with base64
- **Control flow flattening** to disrupt execution flow
- **Dead code injection** to add complexity
- **Number expressions** instead of literal numbers
- **Reserved React Native APIs** to maintain compatibility

## Publishing Workflow

1. **Development**: Work on source files in `src/`
2. **Build**: Run `yarn build:production` to create obfuscated build
3. **Test**: Run `yarn test:package` to verify package structure
4. **Publish**: Run `npm publish` (automatically runs build via `prepublishOnly`)

## Testing the Package

Before publishing, you can test the package locally:

```bash
# Create a package tarball
npm pack

# Install locally in another project
npm install /path/to/react-native-mapxus-hsitp-0.1.0.tgz
```

## Security Considerations

- **JavaScript source code is protected** from easy reverse engineering
- **Android source code is compiled** into AAR (no Kotlin source included)
- **TypeScript definitions** are still available for proper type checking
- **React Native compatibility** is maintained through reserved names
- **No sensitive data** should be hardcoded in the source code

## Android AAR Benefits

- **Source code protection**: Kotlin source files are compiled and not included
- **Smaller package size**: Only compiled artifacts are distributed
- **Faster builds**: Users don't need to compile Android code
- **Consistent builds**: Pre-compiled AAR ensures consistent behavior

## Customization

To modify obfuscation settings:

1. Edit `obfuscator.config.js`
2. Adjust obfuscation levels and features
3. Test with `yarn build:production`
4. Verify compatibility with React Native

## Troubleshooting

### Build Issues
- Ensure all dependencies are installed: `yarn install`
- Check TypeScript compilation: `yarn typecheck`
- Verify obfuscator configuration: `yarn obfuscate`

### Package Issues
- Run package test: `yarn test:package`
- Check `.npmignore` excludes source files
- Verify `files` array in `package.json`

### React Native Compatibility
- Check reserved names in obfuscator config
- Test in React Native project after obfuscation
- Ensure no React Native APIs are obfuscated
