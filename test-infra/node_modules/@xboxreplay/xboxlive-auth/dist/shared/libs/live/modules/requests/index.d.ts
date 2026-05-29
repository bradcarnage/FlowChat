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
import type { LiveAuthResponse, LiveCredentials, LivePreAuthOptions, LivePreAuthResponse } from './requests.types';
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
export declare const getAuthorizeUrl: (clientId?: string, scope?: string, responseType?: "token" | "code", redirectUri?: string) => string;
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
export declare const exchangeCodeForAccessToken: (code: string, clientId: string, scope: string, redirectUri: string, clientSecret?: string) => Promise<LiveAuthResponse>;
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
export declare const refreshAccessToken: (refreshToken: string, clientId?: string, scope?: string, clientSecret?: string) => Promise<LiveAuthResponse>;
/**
 * Retrieve required cookies and parameters before authentication
 * @param {LivePreAuthOptions} [options] - Pre-auth options
 * @throws {XRFetchClientException} If the request fails
 * @throws {XRLiveLibraryException} If parameters can't be matched
 * @returns {Promise<LivePreAuthResponse>} Required cookies and parameters
 */
export declare const preAuth: (options?: LivePreAuthOptions) => Promise<LivePreAuthResponse>;
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
export declare const authenticate: (credentials: LiveCredentials) => Promise<LiveAuthResponse>;
