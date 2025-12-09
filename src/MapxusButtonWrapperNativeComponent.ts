import {
  codegenNativeComponent,
  type HostComponent,
  type ViewProps,
} from 'react-native';

export interface MapxusButtonWrapperNativeProps extends ViewProps {
  customLocale?: string;
}

export default codegenNativeComponent<MapxusButtonWrapperNativeProps>(
  'MapxusButtonWrapperView',
  {
    interfaceOnly: false,
    paperComponentName: 'RCTMapxusButtonWrapperView',
  }
) as HostComponent<MapxusButtonWrapperNativeProps>;
