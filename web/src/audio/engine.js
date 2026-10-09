import { stations } from '../data/stations.js';

export function createAudioEngine({ mood = 'calm', volume = 20, onStateChange = () => {} } = {}) {
  let currentMood = Object.hasOwn(stations, mood) ? mood : 'calm';
  let currentVolume = normalizeVolume(volume);
  let context;
  let masterGain;
  let playing = false;
  let starting = false;
  let scheduleInterval;
  let suspendTimeout;
  let operationId = 0;
  let nextChordAt = 0;
  let nextAccentAt = 0;
  let chordIndex = 0;
  const voices = new Set();

  function normalizeVolume(value) {
    const numeric = Number(value);
    return Math.max(0, Math.min(100, Number.isFinite(numeric) ? numeric : 20));
  }

  function state() {
    return { playing, starting, mood: currentMood, volume: currentVolume };
  }

  function publishState() {
    onStateChange(state());
  }

  function updateGain() {
    if (context && masterGain) {
      masterGain.gain.setTargetAtTime(playing ? 0.06 * currentVolume / 100 : 0, context.currentTime, 0.05);
    }
  }

  function playNote(note, startAt, duration, amplitude, wave, attack = 0.08, detune = 0) {
    if (!context || context.state !== 'running') return;
    const oscillator = context.createOscillator();
    const envelope = context.createGain();
    const releaseAt = startAt + Math.max(attack, duration - Math.min(0.8, duration / 2));
    oscillator.type = wave;
    oscillator.frequency.setValueAtTime(440 * (2 ** ((note - 69) / 12)), startAt);
    oscillator.detune.setValueAtTime(detune, startAt);
    envelope.gain.setValueAtTime(0, startAt);
    envelope.gain.linearRampToValueAtTime(amplitude, startAt + attack);
    envelope.gain.setValueAtTime(amplitude, releaseAt);
    envelope.gain.linearRampToValueAtTime(0, startAt + duration);
    oscillator.connect(envelope);
    envelope.connect(masterGain);
    const voice = { oscillator, envelope };
    voices.add(voice);
    oscillator.onended = () => {
      voices.delete(voice);
      oscillator.disconnect();
      envelope.disconnect();
    };
    oscillator.start(startAt);
    oscillator.stop(startAt + duration + 0.03);
  }

  function fadeVoices() {
    if (!context) return;
    const now = context.currentTime;
    voices.forEach(({ oscillator, envelope }) => {
      envelope.gain.cancelScheduledValues(now);
      envelope.gain.setTargetAtTime(0, now, 0.035);
      try { oscillator.stop(now + 0.18); } catch { /* Already finished. */ }
    });
  }

  function refresh() {
    if (!playing || !context || context.state !== 'running') return;
    const now = context.currentTime;
    const station = stations[currentMood];
    const horizon = now + 0.35;
    if (nextChordAt < now - 0.1) nextChordAt = now + 0.05;
    if (nextAccentAt < now - 0.1) nextAccentAt = now + 0.05;
    while (nextChordAt <= horizon) {
      const chord = station.chords[chordIndex % station.chords.length];
      chord.forEach((note, index) => {
        playNote(note, nextChordAt, station.chordSeconds + 0.5, 0.14, station.wave, 0.7, index % 2 ? 2 : -2);
      });
      chordIndex += 1;
      nextChordAt += station.chordSeconds;
    }
    if (currentMood === 'groove') {
      while (nextAccentAt <= horizon) {
        const chord = station.chords[Math.max(0, chordIndex - 1) % station.chords.length];
        playNote(chord[0] - 12, nextAccentAt, 0.36, 0.2, 'sine', 0.025);
        nextAccentAt += 2 / 3;
      }
    } else if (currentMood === 'focus') {
      while (nextAccentAt <= horizon) {
        const chord = station.chords[Math.max(0, chordIndex - 1) % station.chords.length];
        playNote(chord[2] + 12, nextAccentAt, 0.7, 0.07, 'sine', 0.05);
        nextAccentAt += 1.5;
      }
    }
  }

  function restartPattern() {
    fadeVoices();
    chordIndex = 0;
    nextChordAt = context.currentTime + 0.2;
    nextAccentAt = nextChordAt;
    refresh();
  }

  function setMood(nextMood) {
    if (!Object.hasOwn(stations, nextMood) || nextMood === currentMood) return;
    currentMood = nextMood;
    if (playing) restartPattern();
  }

  function setVolume(value) {
    currentVolume = normalizeVolume(value);
    updateGain();
    return currentVolume;
  }

  async function start() {
    if (playing || starting) return 'busy';
    const Context = globalThis.AudioContext || globalThis.webkitAudioContext;
    if (!Context) return 'unsupported';
    const requestId = ++operationId;
    starting = true;
    publishState();
    clearTimeout(suspendTimeout);
    try {
      if (!context || context.state === 'closed') {
        context = new Context();
        masterGain = context.createGain();
        const filter = context.createBiquadFilter();
        filter.type = 'lowpass';
        filter.frequency.value = 1700;
        masterGain.gain.value = 0;
        masterGain.connect(filter);
        filter.connect(context.destination);
      }
      await context.resume();
      if (requestId !== operationId) {
        if (context.state === 'running') context.suspend().catch(() => {});
        return 'cancelled';
      }
      if (context.state !== 'running') throw new Error('Audio did not start.');
      playing = true;
      updateGain();
      restartPattern();
      clearInterval(scheduleInterval);
      scheduleInterval = setInterval(refresh, 120);
      return 'started';
    } catch {
      playing = false;
      return 'failed';
    } finally {
      if (requestId === operationId) {
        starting = false;
        publishState();
      }
    }
  }

  function stop() {
    operationId += 1;
    playing = false;
    starting = false;
    clearInterval(scheduleInterval);
    if (context && masterGain) {
      updateGain();
      fadeVoices();
      clearTimeout(suspendTimeout);
      suspendTimeout = setTimeout(() => {
        if (!playing && context.state === 'running') context.suspend().catch(() => {});
      }, 220);
    }
    publishState();
  }

  function completionTone() {
    if (!playing || !context || context.state !== 'running') return;
    const now = context.currentTime + 0.05;
    playNote(76, now, 0.35, 0.16, 'sine', 0.025);
    playNote(79, now + 0.28, 0.45, 0.16, 'sine', 0.025);
  }

  return { start, stop, setMood, setVolume, refresh, completionTone, get state() { return state(); } };
}
