// config/index.js
// Environment configuration for the native mini program.
// Base URL is the single source of truth for all API calls.

const ENV = {
  // Development: point at the Spring Boot backend on your LAN.
  // Replace the IP with your own PC's LAN IP when running on a real device.
  development: {
    baseUrl: 'http://127.0.0.1:8080',
    envLabel: 'dev',
  },
  // Production: the deployed backend domain (must be HTTPS + whitelisted in MP backend).
  production: {
    baseUrl: 'https://your-domain.com',
    envLabel: 'prod',
  },
};

// Toggle this flag to switch environments.
const CURRENT_ENV = 'development';

const config = {
  env: CURRENT_ENV,
  ...ENV[CURRENT_ENV],
  // Backend unified response: code 1 = success, 0 = failure, 401 = unauthorized (empty body).
  SUCCESS_CODE: 1,
  FAIL_CODE: 0,
  UNAUTHORIZED_STATUS: 401,
};

module.exports = config;
