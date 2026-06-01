/**
 * @typedef {'user' | 'assistant' | string} MessageRole
 */

/**
 * @typedef {Object} ChatMessage
 * @property {MessageRole} role
 * @property {string} content
 */

/**
 * @typedef {Object} ChatRequest
 * @property {string} query
 * @property {number|string=} conversationId
 * @property {ChatMessage[]=} history
 */

/**
 * @typedef {Object} Conversation
 * @property {number|string} id
 * @property {string=} title
 */

/**
 * @typedef {Object} ChatSource
 * @property {string=} file
 */

/**
 * @typedef {Object} ChatResponse
 * @property {string=} answer
 * @property {number|string=} conversationId
 * @property {ChatSource[]=} sources
 */

export {};
