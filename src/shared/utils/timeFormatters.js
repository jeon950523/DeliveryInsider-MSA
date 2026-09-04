const SERVER_TIME_ZONE = 'Asia/Seoul';
const TIMEZONE_SUFFIX_PATTERN = /(Z|[+-]\d{2}:?\d{2})$/i;

export const parseServerDateTime = (value) => {
  if (!value) {
    return null;
  }

  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : value;
  }

  const text = String(value).trim();
  const normalized = TIMEZONE_SUFFIX_PATTERN.test(text)
    ? text
    : `${text}Z`;

  const parsed = new Date(normalized);

  return Number.isNaN(parsed.getTime())
    ? null
    : parsed;
};


export const formatKstDate = (value, fallback = '-') => {
  const date = parseServerDateTime(value);

  if (!date) {
    return fallback;
  }

  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: SERVER_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(date);

  const values = Object.fromEntries(
    parts
      .filter((part) => part.type !== 'literal')
      .map((part) => [part.type, part.value])
  );

  return `${values.year}-${values.month}-${values.day}`;
};

export const formatKstTime = (value, fallback = '-') => {
  const date = parseServerDateTime(value);

  if (!date) {
    return fallback;
  }

  return new Intl.DateTimeFormat('ko-KR', {
    timeZone: SERVER_TIME_ZONE,
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date);
};

export const formatKstDateTime = (value, fallback = '-') => {
  const date = parseServerDateTime(value);

  if (!date) {
    return fallback;
  }

  return new Intl.DateTimeFormat('ko-KR', {
    timeZone: SERVER_TIME_ZONE,
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date);
};

export const diffSeconds = (startValue, endValue = new Date()) => {
  const start = parseServerDateTime(startValue);
  const end = parseServerDateTime(endValue) || new Date();

  if (!start || !end) {
    return null;
  }

  const milliseconds = Math.max(0, end.getTime() - start.getTime());

  return Math.floor(milliseconds / 1_000);
};

export const diffMinutes = (startValue, endValue = new Date()) => {
  const seconds = diffSeconds(startValue, endValue);

  if (seconds === null) {
    return null;
  }

  return Math.floor(seconds / 60);
};

export const formatDurationMinutes = (value, options = {}) => {
  const { zeroAsLessThanMinute = false, fallback = '-' } = options;

  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return fallback;
  }

  const minutes = Math.max(0, Math.floor(Number(value)));

  if (minutes === 0) {
    return zeroAsLessThanMinute
      ? '1분 미만'
      : '0분';
  }

  if (minutes < 60) {
    return `${minutes}분`;
  }

  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;

  if (remainingMinutes === 0) {
    return `${hours}시간`;
  }

  return `${hours}시간 ${remainingMinutes}분`;
};


export const formatDurationSeconds = (value, options = {}) => {
  const { zeroAsLessThanSecond = false, fallback = '-' } = options;

  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return fallback;
  }

  const seconds = Math.max(0, Math.floor(Number(value)));

  if (seconds === 0) {
    return zeroAsLessThanSecond
      ? '1초 미만'
      : '0초';
  }

  if (seconds < 60) {
    return `${seconds}초`;
  }

  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;

  if (minutes < 60) {
    return remainingSeconds === 0
      ? `${minutes}분`
      : `${minutes}분 ${remainingSeconds}초`;
  }

  return formatDurationMinutes(
    minutes,
    { fallback }
  );
};

export const formatElapsedSince = (startValue, nowValue = new Date()) => {
  const minutes = diffMinutes(startValue, nowValue);

  return formatDurationMinutes(minutes, {
    zeroAsLessThanMinute: true,
  });
};

export const getCurrentStageLabel = (status) => ({
  WAITING: '접수 대기',
  COOKING: '조리',
  READY_FOR_PICKUP: '픽업 대기',
  DELIVERING: '배달',
}[status] || '처리');
