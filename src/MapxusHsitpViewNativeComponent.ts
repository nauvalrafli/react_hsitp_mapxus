import {
  codegenNativeComponent,
  type HostComponent,
  type ViewProps,
} from 'react-native';
import type { WithDefault } from 'react-native/Libraries/Types/CodegenTypesNamespace';

export interface NativeProps extends ViewProps {
  color?: string;

  // default to none to avoid defaulting to en-US
  customLocale?:  WithDefault<'en-US' | 'zh-HK' | 'zh-CN' | 'none', 'none'>;
}

export default (codegenNativeComponent<NativeProps>(
  'MapxusHsitpView'
) as HostComponent<NativeProps>);
