import { TurboModuleRegistry, type TurboModule } from 'react-native';

export type Filter = 'sepia' | 'mono' | 'invert';
export type AudioMode = 'replace' | 'mix';

export interface Spec extends TurboModule {
  trim(path: string, start: number, end: number): Promise<string>;
  merge(paths: string[]): Promise<string>;
  addAudio(
    videoPath: string,
    audioPath: string,
    mode: AudioMode
  ): Promise<string>;
  applyFilter(path: string, filter: Filter): Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('VideoLab');
