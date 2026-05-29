"use strict";
/**
 * Copyright 2025 Alexis Bize
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.authenticate = exports.preAuth = exports.refreshAccessToken = exports.exchangeCodeForAccessToken = exports.getAuthorizeUrl = void 0;
const Clients_1 = __importDefault(require("../../../../classes/Fetch/Clients"));
const XRLiveLibraryException_1 = __importDefault(require("../../classes/Exceptions/XRLiveLibraryException"));
const config_1 = require("../../config");
/**
 * Returns login.live.com authorize URL
 * @param {string} [clientId] - Client ID
 * @param {string} [scope] - OAuth scope
 * @param {'token'|'code'} [responseType] - Response type
 * @param {string} [redirectUri] - Redirect URI
 *
 * @example
 * // Using defaults
 * getAuthorizeUrl();
 *
 * @example
 * // Custom parameters
 * getAuthorizeUrl('xxxxxx', 'XboxLive.signin', 'code', 'https://xxxxxx');
 *
 * @returns {string} Authorize URL with query parameters
 */
const getAuthorizeUrl = (clientId = config_1.config.clients.xboxApp.id, scope = config_1.config.clients.xboxApp.scope, responseType = config_1.config.clients.xboxApp.responseType, redirectUri = config_1.config.clients.xboxApp.redirectUri) => `${config_1.config.urls.authorize}?${new URLSearchParams({
    client_id: clientId,
    redirect_uri: redirectUri,
    response_type: responseType,
    scope: scope,
}).toString()}`;
exports.getAuthorizeUrl = getAuthorizeUrl;
/**
 * Exchange returned code for a valid access token
 * @param {string} code - Authorization code
 * @param {string} clientId - Client ID
 * @param {string} scope - OAuth scope
 * @param {string} redirectUri - Redirect URI
 * @param {string} [clientSecret] - Client secret
 * @throws {XRFetchClientException} If the request fails
 * @returns {Promise<LiveAuthResponse>} OAuth token response
 *
 * @example
 * // Exchange code for access token
 * const token = await exchangeCodeForAccessToken('code', 'clientId', 'scope', 'redirectUri');
 */
const exchangeCodeForAccessToken = async (code, clientId, scope, redirectUri, clientSecret) => {
    const payload = {
        code,
        client_id: clientId,
        grant_type: 'authorization_code',
        redirect_uri: redirectUri,
        scope,
    };
    if (clientSecret !== void 0) {
        payload.client_secret = clientSecret;
    }
    return Clients_1.default.post(config_1.config.urls.token, new URLSearchParams(payload).toString(), {
        headers: { Accept: 'application/json', 'Content-Type': 'application/x-www-form-urlencoded' },
    }).then(res => res.data);
};
exports.exchangeCodeForAccessToken = exchangeCodeForAccessToken;
/**
 * Refresh an expired token
 * @param {string} refreshToken - The refresh token
 * @param {string} [clientId] - Client ID
 * @param {string} [scope] - OAuth scope
 * @param {string} [clientSecret] - Client secret
 * @throws {XRFetchClientException} If the request fails
 * @returns {Promise<LiveAuthResponse>} Refresh token response
 *
 * @example
 * // Using defaults
 * await refreshAccessToken('M.R3_B.xxxxxx');
 *
 * @example
 * // Custom parameters
 * await refreshAccessToken('M.R3_B.xxxxxx', 'xxxxxx', 'XboxLive.signin', 'xxxxxx');
 */
const refreshAccessToken = async (refreshToken, clientId = config_1.config.clients.xboxApp.id, scope = config_1.config.clients.xboxApp.scope, clientSecret) => {
    const payload = {
        client_id: clientId,
        scope,
        grant_type: 'refresh_token',
        refresh_token: refreshToken,
    };
    if (clientSecret !== void 0) {
        payload.client_secret = clientSecret;
    }
    return Clients_1.default.post(config_1.config.urls.token, new URLSearchParams(payload).toString(), {
        headers: { Accept: 'application/json', 'Content-Type': 'application/x-www-form-urlencoded' },
    }).then(res => res.data);
};
exports.refreshAccessToken = refreshAccessToken;
/**
 * Retrieve required cookies and parameters before authentication
 * @param {LivePreAuthOptions} [options] - Pre-auth options
 * @throws {XRFetchClientException} If the request fails
 * @throws {XRLiveLibraryException} If parameters can't be matched
 * @returns {Promise<LivePreAuthResponse>} Required cookies and parameters
 */
const preAuth = async (options) => {
    const url = (0, exports.getAuthorizeUrl)(options?.clientId, options?.scope, options?.responseType, options?.redirectUri);
    const resp = await Clients_1.default.get(url, {
        options: { parseJson: false },
    });
    const body = resp.data;
    const cookies = resp.headers['set-cookie'] || '';
    // Extract cookies from set-cookie header
    const cookie = cookies
        .split(',')
        .map((c) => c.trim().split(';')[0])
        .filter(Boolean)
        .join('; ');
    const matches = {
        PPFT: extractPPFT(body) ?? void 0,
        urlPost: extractUrlPost(body) ?? void 0,
    };
    if (matches.PPFT !== void 0 && matches.urlPost !== void 0) {
        return { cookie, matches: matches };
    }
    throw new XRLiveLibraryException_1.default(`Could not match required "preAuth" parameters`, {
        attributes: { code: 'PRE_AUTH_ERROR', extra: { matches } },
    });
};
exports.preAuth = preAuth;
/**
 * Authenticates with Microsoft Account using credentials
 * @param {LiveCredentials} credentials - Email and password credentials
 * @throws {XRFetchClientException} If the request fails
 * @throws {XRLiveLibraryException} If the authentication has failed
 * @returns {Promise<LiveAuthResponse>} Authentication response with tokens
 *
 * @example
 * const tokens = await authenticate({ email: 'user@example.com', password: 'password' });
 */
const authenticate = async (credentials) => {
    const preAuthResponse = await (0, exports.preAuth)();
    const resp = await Clients_1.default.post(preAuthResponse.matches.urlPost, new URLSearchParams({
        login: credentials.email,
        loginfmt: credentials.email,
        passwd: credentials.password,
        PPFT: preAuthResponse.matches.PPFT,
    }).toString(), {
        headers: {
            ['Content-Type']: 'application/x-www-form-urlencoded',
            ['Cookie']: preAuthResponse.cookie,
        },
        redirect: 'manual',
        options: { parseJson: false },
    });
    if (resp.statusCode !== 302) {
        throw new XRLiveLibraryException_1.default(`The authentication has failed`, {
            attributes: { code: 'INVALID_CREDENTIALS_OR_2FA_ENABLED' },
        });
    }
    const hash = (resp.headers.location || '').split('#')[1] || null;
    if (hash === null) {
        throw new XRLiveLibraryException_1.default(`The authentication has failed`, {
            attributes: { code: 'MISSING_HASH_PARAMETERS' },
        });
    }
    const params = new URLSearchParams(hash);
    const formatted = {};
    for (const [key, value] of params.entries()) {
        if (key === 'expires_in') {
            formatted[key] = Number(value);
        }
        else
            formatted[key] = value;
    }
    const output = formatted;
    if (output.refresh_token === void 0 || output.refresh_token === '') {
        output.refresh_token = null;
    }
    return output;
};
exports.authenticate = authenticate;
/**
 * Extracts the PPFT value from page
 * @param htmlContent - The HTML content containing the PPFT input field
 * @returns {string | null} The PPFT token value if found, null otherwise
 */
const extractPPFT = (htmlContent) => {
    const ppftRegex = /name=\\?"PPFT\\?"[^>]*value=\\?"([^"\\]+)\\?"/i;
    const match = htmlContent.match(ppftRegex);
    return match !== null && match[1] !== void 0 ? match[1] : null;
};
/**
 * Extracts the urlPost value from the page
 * @param htmlContent - The HTML content containing the ServerData JavaScript object
 * @returns {string | null} The urlPost URL if found, null otherwise
 */
const extractUrlPost = (htmlContent) => {
    const urlPostRegex = /\\?['"]?urlPost\\?['"]?:\s*\\?['"]([^'"\\]+)\\?['"]/i;
    const match = htmlContent.match(urlPostRegex);
    return match !== null && match[1] !== void 0 ? match[1] : null;
};
