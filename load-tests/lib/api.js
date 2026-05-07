import encoding from "k6/encoding";
import http from "k6/http";

import { BASE_URL, DEFAULT_PASSWORD, jsonParams } from "./config.js";

export function postJson(path, payload, token) {
  return http.post(`${BASE_URL}${path}`, JSON.stringify(payload), jsonParams(token));
}

export function getJson(path, token) {
  return http.get(`${BASE_URL}${path}`, jsonParams(token));
}

export function registerUser(user) {
  return postJson("/api/v1/auth/register", user);
}

export function login(email, password = DEFAULT_PASSWORD) {
  return postJson("/api/v1/auth/login", { email, password });
}

export function refresh(refreshToken) {
  return postJson("/api/v1/auth/refresh", { refreshToken });
}

export function logout(refreshToken) {
  return postJson("/api/v1/auth/logout", { refreshToken });
}

export function createTask(userId, accessToken, task) {
  return postJson(`/api/v1/users/${userId}/tasks`, task, accessToken);
}

export function listTasks(userId, accessToken) {
  return getJson(`/api/v1/users/${userId}/tasks?page=0&size=10&sort=createdAt,desc`, accessToken);
}

export function getTask(taskId, accessToken) {
  return getJson(`/api/v1/tasks/${taskId}`, accessToken);
}

export function extractUserIdFromAccessToken(accessToken) {
  const payload = extractAccessTokenPayload(accessToken);
  return Number(payload.sub);
}

function extractAccessTokenPayload(accessToken) {
  if (!accessToken) {
    throw new Error("Access token is required");
  }

  const parts = accessToken.split(".");
  if (parts.length !== 3) {
    throw new Error("Invalid JWT format");
  }

  const normalized = normalizeBase64Url(parts[1]);
  const decoded = encoding.b64decode(normalized, "std", "s");
  return JSON.parse(decoded);
}

function normalizeBase64Url(value) {
  const replaced = value.replace(/-/g, "+").replace(/_/g, "/");
  const padding = replaced.length % 4 === 0 ? "" : "=".repeat(4 - (replaced.length % 4));
  return `${replaced}${padding}`;
}
