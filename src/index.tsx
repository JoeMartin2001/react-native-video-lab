import VideoLab, { type AudioMode, type Filter } from './NativeVideoLab';

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
  audioPath: string,
  mode: AudioMode
): Promise<string> {
  return VideoLab.addAudio(videoPath, audioPath, mode);
}

export function applyFilter(path: string, filter: Filter): Promise<string> {
  return VideoLab.applyFilter(path, filter);
}

export { type Filter, type AudioMode };
