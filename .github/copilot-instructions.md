# Copilot Instructions for react-native-mapxus-hsitp

## Project Overview

A React Native library wrapper for Mapxus HSITP (indoor positioning service). This is a **Yarn workspace monorepo** containing the main library (root) and an example app (`example/`). Native code (iOS/Android) interfaces with React via the New Architecture's Fabric renderer.

## Key Architecture Patterns

### Monorepo Structure
- **Root package**: The library source with native modules
- **`example/`**: Demo app showing library usage; uses local library version
- **`lib/`**: Build output (not committed) - JavaScript, TypeScript definitions, modules
- **Build chain**: `src/` (TypeScript) → compiled `lib/module/` → obfuscated for npm

### Native Component Bridge
- Uses **TurboModule + Codegen** (New Architecture)
- TypeScript spec files define native interfaces: [MapxusHsitpViewNativeComponent.ts](../src/MapxusHsitpViewNativeComponent.ts)
- **iOS**: Objective-C++ implementation ([MapxusHsitpView.mm](../ios/MapxusHsitpView.mm))
- **Android**: Java/Kotlin (built via Gradle to AAR)
- Export constant locales via `CustomLocale` object

### JavaScript Output Handling
- All JS **must be obfuscated before npm publishing** via `javascript-obfuscator`
- Config: [obfuscator.config.js](../obfuscator.config.js) - preserves React Native identifiers
- TypeScript definitions (.d.ts) are **never obfuscated** - critical for type checking
- Source maps cleaned before publication

## Development Workflow

### Setup & Installation
```bash
yarn          # Install workspace dependencies (requires Yarn 4+, not npm)
```

### Development Commands
```bash
yarn example start                  # Start Metro packager
yarn example android                # Run on Android device/emulator
yarn example ios                    # Run on iOS simulator/device
yarn typecheck                      # TypeScript validation
yarn lint                           # ESLint + Prettier check
yarn lint --fix                     # Auto-fix formatting
yarn test                           # Jest unit tests
```

### Building & Publishing
```bash
yarn build              # Full build: Android AAR + JS module + TS types + obfuscation
yarn build:android      # Build Android AAR only (calls scripts/build-android.sh)
yarn build:types        # Generate TypeScript definitions in lib/typescript/
yarn build:js           # Compile JS to lib/module/ via bob
yarn obfuscate          # Run JavaScript obfuscator on lib/module/
yarn build:production   # build + clean:source-maps (used by prepublishOnly)
```

### Android Build Details
- Script: [scripts/build-android.sh](../scripts/build-android.sh)
- Runs `./gradlew :react-native-mapxus-hsitp:assembleRelease` from `example/android/`
- Copies AAR output → `android/libs/react-native-mapxus-hsitp-release.aar`
- Required: `ANDROID_HOME` environment variable

## Code Conventions

### TypeScript & Linting
- **Strict mode enabled**: no implicit any, unused variables/params, unreachable code
- **Target**: ESNext, module resolution: bundler
- **Babel preset**: `react-native-builder-bob` (special preset for library cross-compilation)
- **ESLint**: Flat config (v9+) with React Native + Prettier; **no console.log** in production builds

### React Native New Architecture
- Components use **Fabric renderer** (not legacy renderer)
- Metro logs should show: `"fabric":true,"concurrentRoot":true`
- Check [turbo.json](../turbo.json) for build caching inputs/outputs

### Testing
- Jest configuration: [jest.config.js](../example/jest.config.js)
- Pre-commit hooks validate tests, linting, and type checking

### Commit Messages
Follow [Conventional Commits](https://www.conventionalcommits.org/):
- `fix:` bug fixes
- `feat:` new features
- `refactor:` code reorganization
- `docs:` documentation updates
- `test:` test additions/updates
- `chore:` tooling/CI changes

## Critical Integration Points

### Native Code Changes
When modifying iOS or Android code, **rebuild the example app** to test:
```bash
yarn example android    # Rebuilds native code for Android
yarn example ios        # Rebuilds native code for iOS
```

### JavaScript Changes
Changes to `src/` reflect in the example app **without rebuild** (Metro hot reload), but:
- Native code changes **require** rebuild
- After main build, JavaScript is **obfuscated**, making debugging harder

### Library vs. Example Dependencies
- Update library deps in **root `package.json`**
- Update example app deps in **`example/package.json`**
- Both use shared workspace packages

## Common Tasks & Gotchas

### Publishing to npm
1. Run `yarn build:production` to ensure clean build
2. Pre-commit hooks check everything automatically
3. Use `npm publish` or `yarn npm publish` (publishes from `lib/` output only)
4. `.npmignore` prevents `src/`, tests, and configs from reaching npm

### Debugging
- **JavaScript**: Metro dev server shows source code; production uses obfuscated code
- **Native**: Use Android Studio / Xcode for native debugging (see CONTRIBUTING.md)
- **TypeScript**: Errors caught at compile time (strict mode enabled)

### Dependency Management
- Node >= 20 required (see `.nvmrc`)
- Yarn workspace, no npm in development
- react-native-builder-bob used for cross-platform JS compilation

## File Organization

| Path | Purpose |
|------|---------|
| `src/` | TypeScript source (main entry: `index.tsx`) |
| `src/*NativeComponent.ts` | Fabric codegen specs defining native interfaces |
| `lib/` | Build output (gitignored) |
| `ios/`, `android/` | Native implementation |
| `example/` | Demo app for testing (uses local library) |
| `scripts/` | Build helpers (Android AAR script) |
| `.github/workflows/` | CI/CD pipelines |

## Key Files to Understand

1. **[src/index.tsx](../src/index.tsx)** - Library exports
2. **[src/MapxusHsitpViewNativeComponent.ts](../src/MapxusHsitpViewNativeComponent.ts)** - Component spec with `CustomLocale` enum
3. **[CONTRIBUTING.md](../CONTRIBUTING.md)** - Detailed dev setup, New Architecture details
4. **[obfuscator.config.js](../obfuscator.config.js)** - Obfuscation rules for production
5. **[turbo.json](../turbo.json)** - Workspace build caching strategy
