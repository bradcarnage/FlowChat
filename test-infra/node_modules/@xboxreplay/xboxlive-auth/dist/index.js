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
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __exportStar = (this && this.__exportStar) || function(m, exports) {
    for (var p in m) if (p !== "default" && !Object.prototype.hasOwnProperty.call(exports, p)) __createBinding(exports, m, p);
};
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.XSAPIClient = exports.HTTPClient = exports.xnet = exports.live = exports.authenticate = void 0;
const Clients_1 = __importDefault(require("./shared/classes/Fetch/Clients"));
exports.HTTPClient = Clients_1.default;
const XSAPIFetchClient_1 = __importDefault(require("./shared/classes/Fetch/Clients/XSAPIFetchClient"));
exports.XSAPIClient = XSAPIFetchClient_1.default;
const experimental_1 = require("./shared/libs/xbox-network/modules/requests/experimental");
const requests_1 = require("./shared/libs/xbox-network/modules/requests");
const requests_2 = require("./shared/libs/live/modules/requests");
/**
 * Authenticates a user with Microsoft Account credentials and returns user and token information
 * @param {Email} email - The user's email address
 * @param {string} password - The user's password
 * @param {AuthenticateOptions} [options] - Optional authentication options
 * @returns {Promise<AuthenticateRawResponse | AuthenticateResponse>} The authentication result, either raw responses or a simplified object
 */
// @ts-expect-error overload
const authenticate = async (email, password, options = {}) => {
    const authResponse = await (0, requests_2.authenticate)({ email, password });
    const userTokenResponse = await (0, requests_1.exchangeRpsTicketForUserToken)(authResponse.access_token, 't');
    const reqTokens = { userTokens: [userTokenResponse.Token] };
    const XSTSResponse = await (0, requests_1.exchangeTokensForXSTSToken)(reqTokens, {
        XSTSRelyingParty: options.XSTSRelyingParty,
        optionalDisplayClaims: options.optionalDisplayClaims,
        sandboxId: options.sandboxId,
    });
    if (options.raw === true) {
        return {
            'login.live.com': authResponse,
            'user.auth.xboxlive.com': userTokenResponse,
            'xsts.auth.xboxlive.com': XSTSResponse,
        };
    }
    return {
        xuid: XSTSResponse.DisplayClaims.xui[0].xid || null,
        user_hash: XSTSResponse.DisplayClaims.xui[0].uhs,
        xsts_token: XSTSResponse.Token,
        display_claims: XSTSResponse.DisplayClaims,
        expires_on: XSTSResponse.NotAfter,
    };
};
exports.authenticate = authenticate;
exports.live = {
    preAuth: requests_2.preAuth,
    getAuthorizeUrl: requests_2.getAuthorizeUrl,
    refreshAccessToken: requests_2.refreshAccessToken,
    authenticateWithCredentials: requests_2.authenticate,
    exchangeCodeForAccessToken: requests_2.exchangeCodeForAccessToken,
};
exports.xnet = {
    exchangeTokenForXSTSToken: requests_1.exchangeTokenForXSTSToken,
    exchangeTokensForXSTSToken: requests_1.exchangeTokensForXSTSToken,
    exchangeCodeForAccessToken: requests_2.exchangeCodeForAccessToken,
    exchangeRpsTicketForUserToken: requests_1.exchangeRpsTicketForUserToken,
    experimental: {
        createDummyWin32DeviceToken: experimental_1.createDummyWin32DeviceToken,
    },
};
__exportStar(require("./types/lib.types"), exports);
__exportStar(require("./shared/classes/Fetch/Fetch.types"), exports);
__exportStar(require("./shared/libs/live/modules/requests/requests.types"), exports);
__exportStar(require("./shared/libs/xbox-network/modules/requests/requests.types"), exports);
