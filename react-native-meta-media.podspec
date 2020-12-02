package = JSON.parse(File.read(File.join(__dir__, "package.json")))
version = package['version']

Pod::Spec.new do |s|
  s.name             = "react-native-meta-media"
  s.version          = version
  s.summary          = package["description"]
  s.requires_arc = true
  s.license      = 'MIT'
  s.homepage     = 'n/a'
  s.authors      = { "ivpusic" => "" }
  s.source       = { :git => "https://github.com/mybigday/react-native-media-meta", :tag => 'v#{version}'}
  s.source_files = 'ios/RNMediaMeta/*.{h,m}'
  s.platform     = :ios, "8.0"
  s.dependency 'React-Core'
end
