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

export interface NativeProps extends ViewProps {
  color?: string;
  customLocale?: string;
}

export default codegenNativeComponent<NativeProps>(
  'MapxusHsitpView'
) as HostComponent<NativeProps>;
