import { stations } from '../data/stations.js';

const moodKey = 'kitchen-mood-radio:mood';

export function readMood(fallback = 'calm', storage) {
  const defaultMood = Object.hasOwn(stations, fallback) ? fallback : 'calm';
  try {
    const saved = (storage ?? globalThis.localStorage)?.getItem(moodKey);
    return Object.hasOwn(stations, saved) ? saved : defaultMood;
  } catch {
    return defaultMood;
  }
}

export function writeMood(mood, storage) {
  if (!Object.hasOwn(stations, mood)) return false;
  try {
    const preferences = storage ?? globalThis.localStorage;
    if (!preferences) return false;
    preferences.setItem(moodKey, mood);
    return true;
  } catch {
    return false;
  }
}
