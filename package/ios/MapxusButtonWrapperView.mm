#import "MapxusButtonWrapperView.h"

#import <react/renderer/components/MapxusButtonWrapperViewSpec/ComponentDescriptors.h>
#import <react/renderer/components/MapxusButtonWrapperViewSpec/EventEmitters.h>
#import <react/renderer/components/MapxusButtonWrapperViewSpec/Props.h>
#import <react/renderer/components/MapxusButtonWrapperViewSpec/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"

using namespace facebook::react;

@interface MapxusButtonWrapperView () <RCTMapxusButtonWrapperViewViewProtocol>

@end

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
    
    // Create a simple map screen/view controller
    UIViewController *mapViewController = [[UIViewController alloc] init];
    mapViewController.view.backgroundColor = [UIColor whiteColor];
    mapViewController.title = @"Mapxus View";
    
    // Create a label to show the locale and name
    UILabel *infoLabel = [[UILabel alloc] init];
    infoLabel.translatesAutoresizingMaskIntoConstraints = NO;
    infoLabel.text = [NSString stringWithFormat:@"Locale: %@\nName: %@", _customLocale, _name.length > 0 ? _name : @"N/A"];
    infoLabel.numberOfLines = 0;
    infoLabel.textAlignment = NSTextAlignmentCenter;
    infoLabel.font = [UIFont systemFontOfSize:16];
    
    [mapViewController.view addSubview:infoLabel];
    
    [NSLayoutConstraint activateConstraints:@[
        [infoLabel.centerXAnchor constraintEqualToAnchor:mapViewController.view.centerXAnchor],
        [infoLabel.centerYAnchor constraintEqualToAnchor:mapViewController.view.centerYAnchor],
        [infoLabel.leadingAnchor constraintEqualToAnchor:mapViewController.view.leadingAnchor constant:20],
        [infoLabel.trailingAnchor constraintEqualToAnchor:mapViewController.view.trailingAnchor constant:-20]
    ]];
    
    // Create a close button
    UIButton *closeButton = [UIButton buttonWithType:UIButtonTypeSystem];
    closeButton.translatesAutoresizingMaskIntoConstraints = NO;
    [closeButton setTitle:@"Close" forState:UIControlStateNormal];
    closeButton.backgroundColor = [UIColor systemRedColor];
    [closeButton setTitleColor:UIColor.whiteColor forState:UIControlStateNormal];
    closeButton.layer.cornerRadius = 8;
    closeButton.clipsToBounds = YES;
    closeButton.contentEdgeInsets = UIEdgeInsetsMake(10, 16, 10, 16);
    
    [mapViewController.view addSubview:closeButton];
    
    [NSLayoutConstraint activateConstraints:@[
        [closeButton.centerXAnchor constraintEqualToAnchor:mapViewController.view.centerXAnchor],
        [closeButton.bottomAnchor constraintEqualToAnchor:mapViewController.view.bottomAnchor constant:-30],
        [closeButton.widthAnchor constraintEqualToConstant:100]
    ]];
    
    // Close button action
    [closeButton addTarget:mapViewController action:@selector(dismissViewControllerAnimated:completion:) forControlEvents:UIControlEventTouchUpInside];
    
    // Find the navigation controller or use the root view controller
    UIViewController *presentingViewController = rootViewController;
    if ([rootViewController isKindOfClass:[UINavigationController class]]) {
        presentingViewController = rootViewController;
    }
    
    // Present the new screen
    UINavigationController *navigationController = [[UINavigationController alloc] initWithRootViewController:mapViewController];
    [presentingViewController presentViewController:navigationController animated:YES completion:^{
        NSLog(@"[MapxusButtonWrapper] Map screen opened with locale: %@", _customLocale);
    }];
}

Class<RCTComponentViewProtocol> MapxusButtonWrapperViewCls(void)
{
    return MapxusButtonWrapperView.class;
}

@end
