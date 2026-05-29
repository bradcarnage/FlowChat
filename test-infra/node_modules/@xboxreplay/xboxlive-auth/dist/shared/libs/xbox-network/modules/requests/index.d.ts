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
import type { Preamble, XNETTokens, XNETExchangeTokensOptions, XNETExchangeTokensResponse, XNETExchangeRpsTicketResponse } from './requests.types';
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
export declare const exchangeRpsTicketForUserToken: (rpsTicket: string, preamble?: Preamble, additionalHeaders?: Record<string, string>) => Promise<XNETExchangeRpsTicketResponse>;
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
export declare const exchangeTokensForXSTSToken: (tokens: XNETTokens, options?: XNETExchangeTokensOptions, additionalHeaders?: Record<string, string>) => Promise<XNETExchangeTokensResponse>;
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
export declare const exchangeTokenForXSTSToken: (userToken: string, options?: XNETExchangeTokensOptions, additionalHeaders?: Record<string, string>) => Promise<XNETExchangeTokensResponse>;
