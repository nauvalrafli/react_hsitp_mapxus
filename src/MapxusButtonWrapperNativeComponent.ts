import {
  codegenNativeComponent,
  type HostComponent,
  type ViewProps,
} from 'react-native';

export const CustomLocale = {
  EN_US: 'en-US',
  ZH_TW: 'zh-TW',
  ZH_CN: 'zh-CN',
} as const;

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
