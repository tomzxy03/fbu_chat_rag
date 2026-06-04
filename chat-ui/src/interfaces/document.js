/**
 * @typedef {Object} DocumentItem
 * @property {string} filename
 * @property {number|string=} year
 * @property {string=} docType
 * @property {number=} chunkCount
 */

/**
 * @typedef {Object} DocumentImageItem
 * @property {string} id
 * @property {string} url
 * @property {string=} caption
 * @property {string=} tags
 * @property {string=} category
 * @property {string=} uploadedAt
 */

/**
 * @typedef {Object} UploadPayload
 * @property {File} file
 * @property {string|number} year
 * @property {string} docType
 */

/**
 * @typedef {Object} ImageUploadPayload
 * @property {File} file
 * @property {string=} caption
 * @property {string=} tags
 * @property {string=} category
 */

export {};
