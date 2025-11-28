import {
  codegenNativeComponent,
  type HostComponent,
  type ViewProps,
} from 'react-native';

export interface NativeProps extends ViewProps {
  color?: string;
  customLocale?:  'en-US' | 'zh-HK' | 'zh-CN' | undefined;
}

export default codegenNativeComponent<NativeProps>(
  'MapxusHsitpView'
) as HostComponent<NativeProps>;
