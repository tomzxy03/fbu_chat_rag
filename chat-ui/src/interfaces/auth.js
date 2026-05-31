/**
 * @typedef {'ADMIN' | 'USER' | string} UserRole
 */

/**
 * @typedef {Object} User
 * @property {string} username
 * @property {UserRole} role
 */

/**
 * @typedef {Object} AuthResponse
 * @property {string} token
 * @property {string} username
 * @property {UserRole} role
 */

/**
 * @typedef {Object} AuthCredentials
 * @property {string} username
 * @property {string} password
 */

export {};
