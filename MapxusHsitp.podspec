require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))
def min_ios_version_supported
  '18.0'
end

Pod::Spec.new do |s|
  s.name         = "MapxusHsitp"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => min_ios_version_supported }
  s.source       = { :git => "https://github.com/nauvalrafli/react-native-mapxus-hsitp.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,mm,cpp,swift}"
  s.private_header_files = "ios/**/*.h"
  s.header_mappings_dir = "ios"

  s.resources = ['ios/Assets.xcassets']
  # s.resources = "ios/**/*.{xcassets,json,png}"

  s.dependency "MapxusMapSDK"
  s.dependency "MapxusComponentKit"
  s.dependency "MapxusVisualSDK"
  s.dependency "SDWebImageSwiftUI"
  s.dependency "Flow"
  s.dependency "AFNetworking"

  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'HEADER_SEARCH_PATHS' => '$(inherited) "${PODS_CONFIGURATION_BUILD_DIR}/MapxusMapSDK/MapxusMapSDK.framework/Headers"',
    'FRAMEWORK_SEARCH_PATHS' => '$(inherited) "${PODS_ROOT}/MapxusMapSDK" "${PODS_ROOT}/MapxusVisualSDK"'
  }

  install_modules_dependencies(s)
end
