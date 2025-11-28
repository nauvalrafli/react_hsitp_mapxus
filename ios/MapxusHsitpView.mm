#import "MapxusHsitpView.h"

#import <react/renderer/components/MapxusHsitpViewSpec/ComponentDescriptors.h>
#import <react/renderer/components/MapxusHsitpViewSpec/EventEmitters.h>
#import <react/renderer/components/MapxusHsitpViewSpec/Props.h>
#import <react/renderer/components/MapxusHsitpViewSpec/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"

using namespace facebook::react;

@interface MapxusHsitpView () <RCTMapxusHsitpViewViewProtocol>

@end

@implementation MapxusHsitpView {
    UIView * _view;
}

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
    return concreteComponentDescriptorProvider<MapxusHsitpViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const MapxusHsitpViewProps>();
    _props = defaultProps;

    _view = [[UIView alloc] init];

    // -------------------------------
    // Add "Under Development" label
    // -------------------------------
    UILabel *label = [[UILabel alloc] init];
    label.text = @"Under Development";
    label.textColor = UIColor.blackColor;     // customize as needed
    label.textAlignment = NSTextAlignmentCenter;
    label.font = [UIFont boldSystemFontOfSize:16];

    // Enable auto-layout
    label.translatesAutoresizingMaskIntoConstraints = NO;
    [_view addSubview:label];

    // Center the text
    [NSLayoutConstraint activateConstraints:@[
        [label.centerXAnchor constraintEqualToAnchor:_view.centerXAnchor],
        [label.centerYAnchor constraintEqualToAnchor:_view.centerYAnchor]
    ]];

    self.contentView = _view;
  }

  return self;
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
    const auto &oldViewProps = *std::static_pointer_cast<MapxusHsitpViewProps const>(_props);
    const auto &newViewProps = *std::static_pointer_cast<MapxusHsitpViewProps const>(props);

    if (oldViewProps.color != newViewProps.color) {
        NSString * colorToConvert = [[NSString alloc] initWithUTF8String: newViewProps.color.c_str()];
        [_view setBackgroundColor:[self hexStringToColor:colorToConvert]];
    }

    [super updateProps:props oldProps:oldProps];
}

Class<RCTComponentViewProtocol> MapxusHsitpViewCls(void)
{
    return MapxusHsitpView.class;
}

- hexStringToColor:(NSString *)stringToConvert
{
    NSString *noHashString = [stringToConvert stringByReplacingOccurrencesOfString:@"#" withString:@""];
    NSScanner *stringScanner = [NSScanner scannerWithString:noHashString];

    unsigned hex;
    if (![stringScanner scanHexInt:&hex]) return nil;
    int r = (hex >> 16) & 0xFF;
    int g = (hex >> 8) & 0xFF;
    int b = (hex) & 0xFF;

    return [UIColor colorWithRed:r / 255.0f green:g / 255.0f blue:b / 255.0f alpha:1.0f];
}

@end
