import { check } from "k6";
import { Rate } from "k6/metrics";

import { envInt, uniqueUser } from "../lib/config.js";
import { extractUserIdFromAccessToken, listTasks, login, registerUser } from "../lib/api.js";

const login429Rate = new Rate("login_429_rate");
const general429Rate = new Rate("general_429_rate");
const unexpectedStatusRate = new Rate("unexpected_status_rate");

const loginRequests = envInt("LOGIN_REQUESTS", 20);
const generalRequests = envInt("GENERAL_REQUESTS", 80);

export const options = {
  scenarios: {
    login_limit: {
      executor: "shared-iterations",
      exec: "loginLimit",
      vus: envInt("LOGIN_VUS", 10),
      iterations: loginRequests,
      maxDuration: "1m",
    },
    general_limit: {
      executor: "shared-iterations",
      exec: "generalLimit",
      startTime: "15s",
      vus: envInt("GENERAL_VUS", 10),
      iterations: generalRequests,
      maxDuration: "2m",
    },
  },
  thresholds: {
    login_429_rate: ["rate>0.50"],
    general_429_rate: ["rate>0.20"],
    unexpected_status_rate: ["rate==0"],
  },
};

export function setup() {
  const user = uniqueUser(0, "ratelimit");
  const registerResponse = registerUser(user);

  if (registerResponse.status !== 201) {
    throw new Error(`Expected register to return 201, got ${registerResponse.status}`);
  }

  const registerBody = registerResponse.json();
  return {
    user: {
      email: user.email,
      password: user.password,
      accessToken: registerBody.accessToken,
      userId: extractUserIdFromAccessToken(registerBody.accessToken),
    },
  };
}

export function loginLimit(data) {
  const response = login(data.user.email, data.user.password);

  login429Rate.add(response.status === 429);
  unexpectedStatusRate.add(response.status !== 200 && response.status !== 429);

  check(response, {
    "login returns 200 or 429": (res) => res.status === 200 || res.status === 429,
  });
}

export function generalLimit(data) {
  const response = listTasks(data.user.userId, data.user.accessToken);

  general429Rate.add(response.status === 429);
  unexpectedStatusRate.add(response.status !== 200 && response.status !== 429);

  check(response, {
    "general endpoint returns 200 or 429": (res) => res.status === 200 || res.status === 429,
  });
}
