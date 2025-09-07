import VideoLab from './NativeVideoLab';

export function trim(
  path: string,
  start: number,
  end: number
): Promise<string> {
  return VideoLab.trim(path, start, end);
}

export function merge(paths: string[]): Promise<string> {
  return VideoLab.merge(paths);
}

export function addAudio(
  videoPath: string,
  audioPath: string
): Promise<string> {
  return VideoLab.addAudio(videoPath, audioPath);
}

export function applyFilter(
  path: string,
  filter: 'sepia' | 'mono' | 'invert'
): Promise<string> {
  return VideoLab.applyFilter(path, filter);
}
