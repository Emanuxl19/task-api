import { check } from "k6";
import { Rate } from "k6/metrics";

import { envInt, uniqueUser } from "../lib/config.js";
import { refresh, registerUser } from "../lib/api.js";

const attempts = envInt("ATTEMPTS", 10);

const refreshSuccessRate = new Rate("refresh_success_rate");
const refreshRejectedRate = new Rate("refresh_rejected_rate");
const refreshUnexpectedRate = new Rate("refresh_unexpected_rate");

export const options = {
  scenarios: {
    refresh_race: {
      executor: "shared-iterations",
      vus: attempts,
      iterations: attempts,
      maxDuration: "1m",
    },
  },
  thresholds: {
    refresh_success_rate: ["rate>0", "rate<0.30"],
    refresh_rejected_rate: ["rate>0.70"],
    refresh_unexpected_rate: ["rate==0"],
  },
};

export function setup() {
  const user = uniqueUser(0, "refresh");
  const registerResponse = registerUser(user);

  if (registerResponse.status !== 201) {
    throw new Error(`Expected register to return 201, got ${registerResponse.status}`);
  }

  return {
    refreshToken: registerResponse.json().refreshToken,
  };
}

export default function (data) {
  const response = refresh(data.refreshToken);

  refreshSuccessRate.add(response.status === 200);
  refreshRejectedRate.add(response.status === 401);
  refreshUnexpectedRate.add(response.status !== 200 && response.status !== 401);

  check(response, {
    "refresh returns 200 or 401": (res) => res.status === 200 || res.status === 401,
  });
}
