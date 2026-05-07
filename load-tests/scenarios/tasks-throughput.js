import { check, sleep } from "k6";
import { Rate, Trend } from "k6/metrics";

import { createTask, extractUserIdFromAccessToken, getTask, listTasks, registerUser } from "../lib/api.js";
import { envFloat, envInt, futureDate, uniqueUser } from "../lib/config.js";

const vus = envInt("VUS", 10);
const iterations = envInt("ITERATIONS", 10);
const pauseSeconds = envFloat("SLEEP_SECONDS", 0.2);

const taskCreateSuccess = new Rate("task_create_success");
const taskListSuccess = new Rate("task_list_success");
const taskGetSuccess = new Rate("task_get_success");
const rateLimitHit = new Rate("rate_limit_hit");
const taskFlowDuration = new Trend("task_flow_duration");

export const options = {
  scenarios: {
    task_flow: {
      executor: "per-vu-iterations",
      vus,
      iterations,
      maxDuration: "10m",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.02"],
    http_req_duration: ["p(95)<1000"],
    task_create_success: ["rate>0.99"],
    task_list_success: ["rate>0.99"],
    task_get_success: ["rate>0.99"],
    rate_limit_hit: ["rate==0"],
    task_flow_duration: ["p(95)<1500"],
  },
};

export function setup() {
  const users = [];

  for (let i = 0; i < vus; i += 1) {
    const credentials = uniqueUser(i, "throughput");
    const registerResponse = registerUser(credentials);

    if (registerResponse.status !== 201) {
      throw new Error(`Expected register to return 201, got ${registerResponse.status}`);
    }

    const registerBody = registerResponse.json();
    users.push({
      accessToken: registerBody.accessToken,
      userId: extractUserIdFromAccessToken(registerBody.accessToken),
    });
  }

  return { users };
}

export default function (data) {
  const startedAt = Date.now();
  const user = data.users[(__VU - 1) % data.users.length];

  const createResponse = createTask(user.userId, user.accessToken, {
    title: `k6-task-${__VU}-${__ITER}`,
    description: "Task created by the k6 throughput scenario",
    priority: "HIGH",
    dueDate: futureDate(7),
  });

  taskCreateSuccess.add(createResponse.status === 201);
  rateLimitHit.add(createResponse.status === 429);

  check(createResponse, {
    "task create returns 201": (res) => res.status === 201,
  });

  if (createResponse.status !== 201) {
    return;
  }

  const createdTask = createResponse.json();

  const listResponse = listTasks(user.userId, user.accessToken);
  taskListSuccess.add(listResponse.status === 200);
  rateLimitHit.add(listResponse.status === 429);

  check(listResponse, {
    "task list returns 200": (res) => res.status === 200,
  });

  const getResponse = getTask(createdTask.id, user.accessToken);
  taskGetSuccess.add(getResponse.status === 200);
  rateLimitHit.add(getResponse.status === 429);

  check(getResponse, {
    "task get returns 200": (res) => res.status === 200,
  });

  taskFlowDuration.add(Date.now() - startedAt);
  sleep(pauseSeconds);
}
