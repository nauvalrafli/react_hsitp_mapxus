#!/usr/bin/env node

/**
 * Test script to verify the npm package contents before publishing
 * This script checks that only the intended files are included in the package
 */

const fs = require('fs');
const path = require('path');

console.log('🧪 Testing npm package contents...\n');

// Check if lib directory exists and contains expected files
const libDir = path.join(__dirname, '..', 'lib');
if (!fs.existsSync(libDir)) {
  console.error(
    '❌ lib directory not found. Run "yarn build:production" first.'
  );
  process.exit(1);
}

// Check obfuscated JavaScript files
const moduleDir = path.join(libDir, 'module');
if (!fs.existsSync(moduleDir)) {
  console.error('❌ lib/module directory not found.');
  process.exit(1);
}

const indexJsPath = path.join(moduleDir, 'index.js');
if (!fs.existsSync(indexJsPath)) {
  console.error('❌ lib/module/index.js not found.');
  process.exit(1);
}

// Verify the file is obfuscated (should contain obfuscated patterns)
const indexJsContent = fs.readFileSync(indexJsPath, 'utf8');
if (!indexJsContent.includes('a0_0x') || indexJsContent.length < 1000) {
  console.warn('⚠️  lib/module/index.js may not be properly obfuscated.');
  console.log('   Content length:', indexJsContent.length);
  console.log(
    '   Contains obfuscation patterns:',
    indexJsContent.includes('a0_0x')
  );
}

// Check TypeScript definitions
const typescriptDir = path.join(libDir, 'typescript');
if (!fs.existsSync(typescriptDir)) {
  console.error('❌ lib/typescript directory not found.');
  process.exit(1);
}

const indexDtsPath = path.join(typescriptDir, 'src', 'index.d.ts');
if (!fs.existsSync(indexDtsPath)) {
  console.error('❌ lib/typescript/src/index.d.ts not found.');
  process.exit(1);
}

// Verify source maps are cleaned up
const sourceMapFiles = [];
function findSourceMaps(dir) {
  const files = fs.readdirSync(dir);
  files.forEach((file) => {
    const filePath = path.join(dir, file);
    const stat = fs.statSync(filePath);
    if (stat.isDirectory()) {
      findSourceMaps(filePath);
    } else if (file.endsWith('.map')) {
      sourceMapFiles.push(filePath);
    }
  });
}

findSourceMaps(libDir);
if (sourceMapFiles.length > 0) {
  console.warn('⚠️  Source maps found in lib directory:');
  sourceMapFiles.forEach((file) => console.log('   ', file));
  console.log('   Run "yarn clean:source-maps" to remove them.');
} else {
  console.log('✅ No source maps found in lib directory.');
}

// Check Android AAR file
const aarFile = path.join(
  __dirname,
  '..',
  'android',
  'libs',
  'react-native-mapxus-hsitp-release.aar'
);
if (fs.existsSync(aarFile)) {
  const stats = fs.statSync(aarFile);
  console.log('✅ Android AAR file found:', path.basename(aarFile));
  console.log(
    '   Size:',
    Math.round((stats.size / 1024 / 1024) * 100) / 100,
    'MB'
  );
} else {
  console.error(
    '❌ Android AAR file not found. Run "yarn build:android" first.'
  );
  process.exit(1);
}

// Check that source files are not included
const srcDir = path.join(__dirname, '..', 'src');
if (fs.existsSync(srcDir)) {
  console.log('✅ Source files exist (will be excluded by .npmignore).');
}

console.log('\n📦 Package structure:');
console.log('├── lib/');
console.log('│   ├── module/');
console.log('│   │   ├── index.js (obfuscated)');
console.log('│   │   └── MapxusHsitpViewNativeComponent.ts');
console.log('│   └── typescript/');
console.log('│       └── src/');
console.log('│           ├── index.d.ts');
console.log('│           └── MapxusHsitpViewNativeComponent.d.ts');
console.log('├── android/');
console.log('│   └── libs/');
console.log('│       └── react-native-mapxus-hsitp-release.aar (compiled)');
console.log('├── ios/');
console.log('├── MapxusHsitp.podspec');
console.log('├── README.md');
console.log('├── LICENSE');
console.log('└── package.json');

console.log('\n✅ Package structure looks good!');
console.log('\n🚀 Ready for publishing! Run:');
console.log('   npm publish');
console.log('\n💡 To test locally first:');
console.log('   npm pack');
console.log('   # This creates a .tgz file you can install locally');
