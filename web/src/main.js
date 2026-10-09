import { stations } from './data/stations.js';
import { createAudioEngine } from './audio/engine.js';
import { Countdown } from './timer/countdown.js';
import { readMood, writeMood } from './storage/preferences.js';

function initialize() {
  const ids = [
    'mood-label', 'mood-copy', 'mood-tag', 'mood-art', 'session-title',
    'session-note', 'sound-toggle', 'sound-state', 'volume', 'volume-value',
    'timer-display', 'timer-status', 'timer-minutes', 'timer-start',
    'timer-reset', 'scene-shuffle', 'scene-line', 'scene-count', 'toast'
  ];
  const elements = Object.fromEntries(ids.map(id => [id, document.getElementById(id)]));
  if (ids.some(id => !elements[id])) {
    console.warn('화면의 필수 요소를 찾을 수 없습니다.');
    return;
  }

  const moodButtons = Array.from(document.querySelectorAll('button[data-mood]'));
  const presetButtons = Array.from(document.querySelectorAll('button[data-minutes]'));
  let currentMood = readMood();
  let currentLine = 0;
  let toastTimeout;

  function showToast(message) {
    clearTimeout(toastTimeout);
    elements.toast.textContent = message;
    toastTimeout = setTimeout(() => { elements.toast.textContent = ''; }, 4500);
  }

  function updateSoundControls({ playing, starting }) {
    document.body.dataset.playing = String(playing);
    elements['sound-toggle'].textContent = starting ? '소리 준비 중…' : playing ? '소리 멈추기' : '소리 재생';
    elements['sound-toggle'].setAttribute('aria-pressed', String(playing));
    elements['sound-toggle'].disabled = starting;
    elements['sound-state'].textContent = playing ? '재생 중 · 볼륨을 편하게 조절해요' : '소리 꺼짐 · 재생을 누르면 시작해요';
  }

  const audio = createAudioEngine({
    mood: currentMood,
    volume: elements.volume.value,
    onStateChange: updateSoundControls
  });

  function updateVolume() {
    const level = audio.setVolume(elements.volume.value);
    elements.volume.value = String(level);
    elements['volume-value'].textContent = `${level}%`;
  }

  function updateLine() {
    const lines = stations[currentMood].lines;
    elements['scene-line'].textContent = lines[currentLine];
    elements['scene-count'].textContent = `${currentLine + 1} / ${lines.length}`;
  }

  function setMood(mood, announce = true) {
    if (!Object.hasOwn(stations, mood)) return;
    const changed = currentMood !== mood;
    currentMood = mood;
    const station = stations[mood];
    document.body.dataset.mood = mood;
    elements['mood-label'].textContent = station.label;
    elements['mood-copy'].textContent = station.copy;
    elements['mood-tag'].textContent = station.tag;
    elements['mood-art'].textContent = station.art;
    elements['session-title'].textContent = station.title;
    elements['session-note'].textContent = station.note;
    moodButtons.forEach(button => {
      const active = button.dataset.mood === mood;
      button.setAttribute('aria-pressed', String(active));
      button.classList.toggle('is-active', active);
    });
    if (changed) currentLine = 0;
    updateLine();
    audio.setMood(mood);
    if (announce) {
      writeMood(mood);
      showToast(`${station.label} 분위기로 바꿨어요.`);
    }
  }

  function renderTimer({ remainingMs }) {
    const seconds = Math.max(0, Math.ceil(remainingMs / 1000));
    const minutesText = String(Math.floor(seconds / 60)).padStart(2, '0');
    const secondsText = String(seconds % 60).padStart(2, '0');
    elements['timer-display'].textContent = `${minutesText}:${secondsText}`;
  }

  function updateTimerState(state, snapshot) {
    elements['timer-minutes'].value = String(snapshot.minutes);
    presetButtons.forEach(button => {
      const active = Number(button.dataset.minutes) === snapshot.minutes;
      button.setAttribute('aria-pressed', String(active));
      button.classList.toggle('is-active', active);
    });
    const labels = {
      ready: ['시작', `준비됐어요 · ${snapshot.minutes}분`],
      running: ['일시정지', '시간이 흐르고 있어요.'],
      paused: ['계속하기', '잠시 멈췄어요. 준비되면 계속해요.'],
      complete: ['다시 시작', '시간이 끝났어요. 요리 상태를 확인해 주세요.']
    };
    const [buttonText, statusText] = labels[state];
    elements['timer-start'].textContent = buttonText;
    elements['timer-status'].textContent = statusText;
  }

  const timer = new Countdown({
    minutes: elements['timer-minutes'].value,
    onTick: renderTimer,
    onStateChange: updateTimerState,
    onComplete: () => {
      audio.completionTone();
      showToast('타이머가 끝났어요. 요리 상태를 확인해 주세요.');
    }
  });

  function prepareTimer(value, announce = true) {
    const minutes = timer.setMinutes(value);
    if (announce) showToast(`타이머를 ${minutes}분으로 준비했어요.`);
  }

  function toggleTimer() {
    if (timer.snapshot.running) {
      if (timer.pause()) showToast('타이머를 잠시 멈췄어요.');
      return;
    }
    const minutes = timer.normalizeMinutes(elements['timer-minutes'].value);
    if (minutes !== timer.snapshot.minutes) prepareTimer(minutes, false);
    if (timer.start()) showToast('타이머를 시작했어요.');
  }

  moodButtons.forEach(button => button.addEventListener('click', () => setMood(button.dataset.mood)));
  elements['scene-shuffle'].addEventListener('click', () => {
    const count = stations[currentMood].lines.length;
    const offset = 1 + Math.floor(Math.random() * (count - 1));
    currentLine = (currentLine + offset) % count;
    updateLine();
    showToast('새로운 한마디를 준비했어요.');
  });
  elements['sound-toggle'].addEventListener('click', async () => {
    if (audio.state.starting) return;
    if (audio.state.playing) {
      audio.stop();
      showToast('소리를 잠시 멈췄어요.');
      return;
    }
    const result = await audio.start();
    if (result === 'started') showToast('작은 볼륨으로 소리를 시작했어요.');
    if (result === 'unsupported') {
      elements['sound-state'].textContent = '이 브라우저에서는 소리를 재생할 수 없어요.';
      showToast('소리 재생을 지원하는 브라우저에서 다시 열어주세요. 타이머는 그대로 사용할 수 있어요.');
    }
    if (result === 'failed') showToast('소리를 시작하지 못했어요. 재생 버튼을 다시 눌러주세요.');
  });
  elements.volume.addEventListener('input', updateVolume);
  elements['timer-start'].addEventListener('click', toggleTimer);
  elements['timer-reset'].addEventListener('click', () => prepareTimer(elements['timer-minutes'].value));
  elements['timer-minutes'].addEventListener('change', () => prepareTimer(elements['timer-minutes'].value));
  presetButtons.forEach(button => button.addEventListener('click', () => prepareTimer(button.dataset.minutes)));
  document.addEventListener('visibilitychange', () => {
    timer.tick();
    if (!document.hidden) audio.refresh();
  });
  window.addEventListener('pagehide', () => audio.stop());

  setMood(currentMood, false);
  updateVolume();
  updateSoundControls(audio.state);
  timer.reset();
}

if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', initialize, { once: true });
else initialize();
