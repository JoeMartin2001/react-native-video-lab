import { TurboModuleRegistry, type TurboModule } from 'react-native';

export interface Spec extends TurboModule {
  trim(path: string, start: number, end: number): Promise<string>;
  merge(paths: string[]): Promise<string>;
  addAudio(videoPath: string, audioPath: string): Promise<string>;
  applyFilter(
    path: string,
    filter: 'sepia' | 'mono' | 'invert'
  ): Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('VideoLab');
