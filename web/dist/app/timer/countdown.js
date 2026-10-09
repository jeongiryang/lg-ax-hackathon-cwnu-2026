export class Countdown {
  constructor({ minutes = 3, onTick = () => {}, onStateChange = () => {}, onComplete = () => {}, now = () => Date.now() } = {}) {
    this.minutes = 3;
    this.minutes = this.normalizeMinutes(minutes);
    this.remainingMs = this.minutes * 60000;
    this.deadline = null;
    this.running = false;
    this.completed = false;
    this.interval = undefined;
    this.now = now;
    this.onTick = onTick;
    this.onStateChange = onStateChange;
    this.onComplete = onComplete;
  }

  get snapshot() {
    return {
      minutes: this.minutes,
      remainingMs: this.remainingMs,
      running: this.running,
      completed: this.completed
    };
  }

  normalizeMinutes(value) {
    const numeric = Number(value);
    const minutes = String(value).trim() !== '' && Number.isFinite(numeric) ? numeric : this.minutes;
    return Math.max(1, Math.min(60, Math.round(minutes)));
  }

  setMinutes(value) {
    this.minutes = this.normalizeMinutes(value);
    this.reset();
    return this.minutes;
  }

  reset() {
    this.clearInterval();
    this.remainingMs = this.minutes * 60000;
    this.deadline = null;
    this.running = false;
    this.completed = false;
    this.onTick(this.snapshot);
    this.onStateChange('ready', this.snapshot);
  }

  start() {
    if (this.running) return false;
    if (this.remainingMs <= 0) this.remainingMs = this.minutes * 60000;
    this.deadline = this.now() + this.remainingMs;
    this.running = true;
    this.completed = false;
    this.interval = setInterval(() => this.tick(), 250);
    this.onStateChange('running', this.snapshot);
    this.tick();
    return true;
  }

  pause() {
    if (!this.running) return false;
    this.tick();
    if (!this.running) return false;
    this.clearInterval();
    this.running = false;
    this.deadline = null;
    this.onStateChange('paused', this.snapshot);
    return true;
  }

  tick() {
    if (!this.running) return this.snapshot;
    this.remainingMs = Math.max(0, this.deadline - this.now());
    this.onTick(this.snapshot);
    if (this.remainingMs === 0) {
      this.clearInterval();
      this.running = false;
      this.completed = true;
      this.deadline = null;
      this.onStateChange('complete', this.snapshot);
      this.onComplete(this.snapshot);
    }
    return this.snapshot;
  }

  clearInterval() {
    globalThis.clearInterval(this.interval);
    this.interval = undefined;
  }
}
