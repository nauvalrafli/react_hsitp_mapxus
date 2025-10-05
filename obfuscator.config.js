module.exports = {
  // Basic obfuscation settings
  compact: true,
  controlFlowFlattening: true,
  controlFlowFlatteningThreshold: 0.75,
  deadCodeInjection: true,
  deadCodeInjectionThreshold: 0.4,
  debugProtection: false, // Disable for React Native compatibility
  debugProtectionInterval: 0,
  disableConsoleOutput: true,
  // Identifier obfuscation
  identifierNamesGenerator: 'hexadecimal',
  identifiersPrefix: '',
  inputFileName: '',
  log: false,
  // Numbers and strings
  numbersToExpressions: true,
  simplify: true,
  splitStrings: true,
  splitStringsChunkLength: 10,
  stringArray: true,
  stringArrayCallsTransform: true,
  stringArrayEncoding: ['base64'],
  stringArrayIndexShift: true,
  stringArrayRotate: true,
  stringArrayShuffle: true,
  stringArrayWrappersCount: 2,
  stringArrayWrappersChainedCalls: true,
  stringArrayWrappersParametersMaxCount: 4,
  stringArrayWrappersType: 'variable',
  stringArrayThreshold: 0.75,
  // Transform object keys
  transformObjectKeys: true,
  // Unicode escape sequence
  unicodeEscapeSequence: false,
  // Source map settings (disable for production)
  sourceMap: false,
  sourceMapMode: 'separate',
  // Reserved names for React Native compatibility
  reservedNames: [
    'React',
    'ReactNative',
    'Component',
    'View',
    'Text',
    'StyleSheet',
    'require',
    'module',
    'exports',
    '__dirname',
    '__filename',
    'global',
    'process',
    'Buffer',
    'console',
    'setTimeout',
    'setInterval',
    'clearTimeout',
    'clearInterval',
    'setImmediate',
    'clearImmediate',
    'requestAnimationFrame',
    'cancelAnimationFrame',
  ],
  // Reserved strings for React Native
  reservedStrings: [
    'react-native',
    'react',
    'native',
    'ios',
    'android',
    'fabric',
    'turbo',
  ],
  // Target specific to React Native
  target: 'node',
  // Exclude certain files/patterns
  exclude: [
    '**/__tests__/**',
    '**/*.test.*',
    '**/*.spec.*',
    '**/node_modules/**',
  ],
};
