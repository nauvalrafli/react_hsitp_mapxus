# react-native-mapxus-hsitp.podspec

Pod::Spec.new do |s|
  s.name         = "react-native-mapxus-hsitp"
  s.version      = "0.0.1"
  # ... metadata ...

  s.source_files = "ios/**/*.{h,m,mm,swift}"

  # 🚀 Mapxus Dependencies
  s.dependency "MapxusMapSDK"
  s.dependency "MapxusBaseSDK"
  s.dependency "MapxusVisualSDK"
  s.dependency "MapxusComponentKit"
  s.dependency "AFNetworking"
  s.dependency "SDWebImageSwiftUI"

  s.dependency "React-Core"
end