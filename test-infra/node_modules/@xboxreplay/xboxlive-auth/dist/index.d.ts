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
import HTTPClient from './shared/classes/Fetch/Clients';
import XSAPIClient from './shared/classes/Fetch/Clients/XSAPIFetchClient';
import type { XNETTokens } from './shared/libs/xbox-network/modules/requests/requests.types';
import type { AuthenticateOptions, AuthenticateRawResponse, AuthenticateResponse, Email } from './types/lib.types';
type AuthenticateFn = {
    (email: Email, password: string, options: AuthenticateOptions & {
        raw: true;
    }): Promise<AuthenticateRawResponse>;
    (email: Email, password: string, options?: AuthenticateOptions & {
        raw?: false;
    }): Promise<AuthenticateResponse>;
};
/**
 * Authenticates a user with Microsoft Account credentials and returns user and token information
 * @param {Email} email - The user's email address
 * @param {string} password - The user's password
 * @param {AuthenticateOptions} [options] - Optional authentication options
 * @returns {Promise<AuthenticateRawResponse | AuthenticateResponse>} The authentication result, either raw responses or a simplified object
 */
export declare const authenticate: AuthenticateFn;
export declare const live: {
    preAuth: (options?: import("./shared/libs/live/modules/requests/requests.types").LivePreAuthOptions) => Promise<import("./shared/libs/live/modules/requests/requests.types").LivePreAuthResponse>;
    getAuthorizeUrl: (clientId?: string, scope?: string, responseType?: "token" | "code", redirectUri?: string) => string;
    refreshAccessToken: (refreshToken: string, clientId?: string, scope?: string, clientSecret?: string) => Promise<import("./shared/libs/live/modules/requests/requests.types").LiveAuthResponse>;
    authenticateWithCredentials: (credentials: import("./shared/libs/live/modules/requests/requests.types").LiveCredentials) => Promise<import("./shared/libs/live/modules/requests/requests.types").LiveAuthResponse>;
    exchangeCodeForAccessToken: (code: string, clientId: string, scope: string, redirectUri: string, clientSecret?: string) => Promise<import("./shared/libs/live/modules/requests/requests.types").LiveAuthResponse>;
};
export declare const xnet: {
    exchangeTokenForXSTSToken: (userToken: string, options?: import("./shared/libs/xbox-network/modules/requests/requests.types").XNETExchangeTokensOptions, additionalHeaders?: Record<string, string>) => Promise<import("./shared/libs/xbox-network/modules/requests/requests.types").XNETExchangeTokensResponse>;
    exchangeTokensForXSTSToken: (tokens: XNETTokens, options?: import("./shared/libs/xbox-network/modules/requests/requests.types").XNETExchangeTokensOptions, additionalHeaders?: Record<string, string>) => Promise<import("./shared/libs/xbox-network/modules/requests/requests.types").XNETExchangeTokensResponse>;
    exchangeCodeForAccessToken: (code: string, clientId: string, scope: string, redirectUri: string, clientSecret?: string) => Promise<import("./shared/libs/live/modules/requests/requests.types").LiveAuthResponse>;
    exchangeRpsTicketForUserToken: (rpsTicket: string, preamble?: import("./shared/libs/xbox-network/modules/requests/requests.types").Preamble, additionalHeaders?: Record<string, string>) => Promise<import("./shared/libs/xbox-network/modules/requests/requests.types").XNETExchangeRpsTicketResponse>;
    experimental: {
        createDummyWin32DeviceToken: () => Promise<import("./shared/libs/xbox-network/modules/requests/requests.types").XNETDummyDeviceTokenResponse>;
    };
};
export * from './types/lib.types';
export * from './shared/classes/Fetch/Fetch.types';
export * from './shared/libs/live/modules/requests/requests.types';
export * from './shared/libs/xbox-network/modules/requests/requests.types';
export { HTTPClient, XSAPIClient };
