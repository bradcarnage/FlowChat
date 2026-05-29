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
exports.exchangeTokenForXSTSToken = exports.exchangeTokensForXSTSToken = exports.exchangeRpsTicketForUserToken = void 0;
const XSAPIFetchClient_1 = __importDefault(require("../../../../classes/Fetch/Clients/XSAPIFetchClient"));
const config_1 = require("../../config");
/**
 * Exchanges an RPS ticket for a user token
 * @param {string} rpsTicket - The RPS ticket to exchange
 * @param {Preamble} [preamble='t'] - The preamble for the ticket
 * @param {Record<string, string>} [additionalHeaders={}] - Additional headers for the request
 * @returns {Promise<XNETExchangeRpsTicketResponse>} The user token response
 *
 * @example
 * const userToken = await exchangeRpsTicketForUserToken('rps-ticket');
 */
const exchangeRpsTicketForUserToken = async (rpsTicket, preamble = 't', additionalHeaders = {}) => {
    const match = rpsTicket.match(/^([d|t]=)/g);
    if (match === null) {
        rpsTicket = `${preamble}=${rpsTicket}`;
    }
    return XSAPIFetchClient_1.default.post(config_1.config.urls.userAuthenticate, {
        RelyingParty: 'http://auth.xboxlive.com',
        TokenType: 'JWT',
        Properties: {
            AuthMethod: 'RPS',
            SiteName: 'user.auth.xboxlive.com',
            RpsTicket: rpsTicket,
        },
    }, { options: { additionalHeaders } }).then(res => res.data);
};
exports.exchangeRpsTicketForUserToken = exchangeRpsTicketForUserToken;
/**
 * Exchanges multiple tokens for an XSTS token
 * @param {XNETTokens} tokens - The tokens to exchange
 * @param {XNETExchangeTokensOptions} [options={}] - Options for the exchange
 * @param {Record<string, string>} [additionalHeaders={}] - Additional headers for the request
 * @returns {Promise<XNETExchangeTokensResponse>} The XSTS token response
 *
 * @example
 * const xstsToken = await exchangeTokensForXSTSToken({ userTokens: ['token'] });
 */
const exchangeTokensForXSTSToken = async (tokens, options = {}, additionalHeaders = {}) => {
    return XSAPIFetchClient_1.default.post(config_1.config.urls.XSTSAuthorize, {
        RelyingParty: options.XSTSRelyingParty || config_1.config.relyingParties.XBOX_LIVE,
        TokenType: 'JWT',
        Properties: {
            UserTokens: tokens.userTokens,
            DeviceToken: tokens.deviceToken,
            TitleToken: tokens.titleToken,
            OptionalDisplayClaims: options.optionalDisplayClaims,
            SandboxId: options.sandboxId || config_1.config.sandboxIds.RETAIL,
        },
    }, { options: { additionalHeaders } }).then(res => res.data);
};
exports.exchangeTokensForXSTSToken = exchangeTokensForXSTSToken;
/**
 * Exchanges a single user token for an XSTS token
 * @param {string} userToken - The user token to exchange
 * @param {XNETExchangeTokensOptions} [options={}] - Options for the exchange
 * @param {Record<string, string>} [additionalHeaders={}] - Additional headers for the request
 * @returns {Promise<XNETExchangeTokensResponse>} The XSTS token response
 *
 * @example
 * const xstsToken = await exchangeTokenForXSTSToken('user-token');
 */
const exchangeTokenForXSTSToken = (userToken, options = {}, additionalHeaders = {}) => (0, exports.exchangeTokensForXSTSToken)({ userTokens: [userToken] }, options, additionalHeaders);
exports.exchangeTokenForXSTSToken = exchangeTokenForXSTSToken;
