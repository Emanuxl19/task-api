export const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
export const DEFAULT_PASSWORD = __ENV.TEST_PASSWORD || "Str0ng@Test1";

const RUN_ID = __ENV.RUN_ID || `${Date.now()}`;

export function envInt(name, fallback) {
  const value = __ENV[name];

  if (value === undefined || value === "") {
    return fallback;
  }

  const parsed = parseInt(value, 10);
  if (Number.isNaN(parsed)) {
    throw new Error(`${name} must be an integer`);
  }

  return parsed;
}

export function envFloat(name, fallback) {
  const value = __ENV[name];

  if (value === undefined || value === "") {
    return fallback;
  }

  const parsed = parseFloat(value);
  if (Number.isNaN(parsed)) {
    throw new Error(`${name} must be a number`);
  }

  return parsed;
}

export function jsonParams(token) {
  const headers = {
    "Content-Type": "application/json",
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return { headers };
}

export function futureDate(daysAhead = 7) {
  const dueDate = new Date();
  dueDate.setUTCDate(dueDate.getUTCDate() + daysAhead);
  return dueDate.toISOString().slice(0, 10);
}

export function uniqueUser(index, prefix = "load") {
  return {
    name: `${prefix}-user-${index}`,
    email: `${prefix}.${RUN_ID}.${index}@example.com`,
    password: DEFAULT_PASSWORD,
  };
}
