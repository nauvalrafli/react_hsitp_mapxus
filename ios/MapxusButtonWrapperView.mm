#import "MapxusButtonWrapperView.h"

// The codegen in the example generates MapxusHsitpSpec for both components;
// import the generated headers from that directory so includes resolve during build.
#import <react/renderer/components/MapxusHsitpSpec/ComponentDescriptors.h>
#import <react/renderer/components/MapxusHsitpSpec/EventEmitters.h>
#import <react/renderer/components/MapxusHsitpSpec/Props.h>
#import <react/renderer/components/MapxusHsitpSpec/RCTComponentViewHelpers.h>
#import <MapxusMapSDK/MapxusMapSDK.h>
#import <MapxusBaseSDK/MapxusBaseSDK.h>

#import "RCTFabricComponentsPlugins.h"

using namespace facebook::react;

//@interface MapxusButtonWrapperView () <RCTMapxusButtonWrapperViewViewProtocol>
//
//@end

@implementation MapxusButtonWrapperView {
    UIButton * _button;
    NSString * _customLocale;
    NSString * _name;
}

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
    return concreteComponentDescriptorProvider<MapxusButtonWrapperViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const MapxusButtonWrapperViewProps>();
    _props = defaultProps;
    
//    if (@available(iOS 16.0, *)) {
//        [NSExpression setAllowInternalSymbolicFunctions:YES];
//    }
    
    [[MXMMapServices sharedServices] registerWithApiKey:@"66bd33b7409c4895862fbd32008acde0"
                                                 secret:@"4631c87ebe5c473f90463a458e6d642c"];

    // Initialize default values
    _customLocale = @"en-US";
    _name = @"";

    // Create a dynamic button wrapper
    _button = [UIButton buttonWithType:UIButtonTypeSystem];
    _button.translatesAutoresizingMaskIntoConstraints = NO;
    
    // Configure button appearance
    [_button setTitle:@"Open Map" forState:UIControlStateNormal];
    _button.backgroundColor = [UIColor systemBlueColor];
    [_button setTitleColor:UIColor.whiteColor forState:UIControlStateNormal];
    _button.layer.cornerRadius = 8;
    _button.clipsToBounds = YES;
    
    // Add padding to button
    _button.contentEdgeInsets = UIEdgeInsetsMake(12, 16, 12, 16);
    _button.titleLabel.font = [UIFont systemFontOfSize:16 weight:UIFontWeightSemibold];
    
    // Add button to view
    [self addSubview:_button];
    
    // Setup constraints to fill the view
    [NSLayoutConstraint activateConstraints:@[
        [_button.leadingAnchor constraintEqualToAnchor:self.leadingAnchor],
        [_button.trailingAnchor constraintEqualToAnchor:self.trailingAnchor],
        [_button.topAnchor constraintEqualToAnchor:self.topAnchor],
        [_button.bottomAnchor constraintEqualToAnchor:self.bottomAnchor]
    ]];
    
    // Add touch handler
    [_button addTarget:self action:@selector(handleButtonPress) forControlEvents:UIControlEventTouchUpInside];
  }

  return self;
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
    const auto &oldViewProps = *std::static_pointer_cast<MapxusButtonWrapperViewProps const>(_props);
    const auto &newViewProps = *std::static_pointer_cast<MapxusButtonWrapperViewProps const>(props);

    // Handle customLocale prop
    if (oldViewProps.customLocale != newViewProps.customLocale) {
        _customLocale = [[NSString alloc] initWithUTF8String: newViewProps.customLocale.c_str()];
    }

    // Handle name prop
    if (oldViewProps.name != newViewProps.name) {
        _name = [[NSString alloc] initWithUTF8String: newViewProps.name.c_str()];
    }

    [super updateProps:props oldProps:oldProps];
}

- (void)handleButtonPress
{
    [self openMapScreen];
}

- (void)openMapScreen
{
    // Get the current view controller from the window
    UIViewController *rootViewController = [UIApplication sharedApplication].windows.firstObject.rootViewController;
    
    if (!rootViewController) {
        NSLog(@"[MapxusButtonWrapper] Could not find root view controller");
        return;
    }
    
    // Try to instantiate HomeViewController from Swift without importing the Swift header.
     Class homeVCClass = NSClassFromString(@"MapxusHsitpViewManager");
     if (!homeVCClass) {
         // Try module-prefixed symbol (common when Swift classes are namespaced)
         homeVCClass = NSClassFromString(@"MapxusHsitp.HomeViewController");
     }

    UIViewController *toPresent = nil;
     if (homeVCClass && [homeVCClass isSubclassOfClass:[UIViewController class]]) {
         // Instantiate and present the Swift-backed view controller
         @try {
             toPresent = [[homeVCClass alloc] init];
         } @catch (NSException *exception) {
             NSLog(@"[MapxusButtonWrapper] Failed to instantiate HomeViewController: %@", exception);
             toPresent = nil;
         }
     }

    if (!toPresent) {
        // Fallback: create a simple placeholder view controller
        UIViewController *mapViewController = [[UIViewController alloc] init];
        mapViewController.view.backgroundColor = [UIColor whiteColor];
        mapViewController.title = @"Map Screen";

        // Add a label to indicate this is the map screen
        UILabel *label = [[UILabel alloc] init];
        label.text = @"Map Screen Placeholder";
        label.textAlignment = NSTextAlignmentCenter;
        label.font = [UIFont systemFontOfSize:18];
        label.translatesAutoresizingMaskIntoConstraints = NO;
        [mapViewController.view addSubview:label];

        NSLayoutConstraint *centerX = [NSLayoutConstraint constraintWithItem:label attribute:NSLayoutAttributeCenterX relatedBy:NSLayoutRelationEqual toItem:mapViewController.view attribute:NSLayoutAttributeCenterX multiplier:1 constant:0];
        NSLayoutConstraint *centerY = [NSLayoutConstraint constraintWithItem:label attribute:NSLayoutAttributeCenterY relatedBy:NSLayoutRelationEqual toItem:mapViewController.view attribute:NSLayoutAttributeCenterY multiplier:1 constant:0];
        [mapViewController.view addConstraints:@[centerX, centerY]];

        toPresent = mapViewController;
    }

    // Present the new screen
    UINavigationController *navigationController = [[UINavigationController alloc] initWithRootViewController:toPresent];
    navigationController.modalPresentationStyle = UIModalPresentationFullScreen;
    [rootViewController presentViewController:navigationController animated:YES completion:^{
        NSLog(@"[MapxusButtonWrapper] Map screen opened with locale: %@", _customLocale);
    }];
}

Class<RCTComponentViewProtocol> MapxusButtonWrapperViewCls(void)
{
    return MapxusButtonWrapperView.class;
}

@end
