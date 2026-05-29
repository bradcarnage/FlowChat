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
exports.createDummyWin32DeviceToken = void 0;
const XSAPIFetchClient_1 = __importDefault(require("../../../../../classes/Fetch/Clients/XSAPIFetchClient"));
const config_1 = require("../../../config");
/**
 * Creates a dummy Win32 device token for Xbox Network authentication (experimental)
 * @warning This is a workaround for the Xbox Network authentication and the associated device ID may be banned by Xbox Network in the future
 * @returns {Promise<XNETDummyDeviceTokenResponse>} The dummy device token response
 */
const createDummyWin32DeviceToken = async () => {
    const signature = 'AAAAAQHcFbBVEuAAHfvqYcbt4rhMgxAKtPiOJgct4UTCX2HqbQNLTHsnwjp9zcYNZMKHEknpyGWNqsIhyXaAd2v8ADmGrfh11oMS1g==';
    const properties = {
        AuthMethod: 'ProofOfPossession',
        Id: '91dc36cd-080a-4493-8234-3b585c78b0d5',
        DeviceType: 'Win32',
        Version: '10.0.19042',
        ProofKey: {
            crv: 'P-256',
            alg: 'ES256',
            use: 'sig',
            kty: 'EC',
            x: 'qMKczrK1b5opLCIX-tzyqOWztlbERh1i5sxDzdHrdxs',
            y: '23uwwgd2oSnWzyjHflRKaLxFsxX0-oE-mECf6c0gOaE',
        },
    };
    return XSAPIFetchClient_1.default.post(config_1.config.urls.deviceAuthenticate, {
        RelyingParty: 'http://auth.xboxlive.com',
        TokenType: 'JWT',
        Properties: properties,
    }, { options: { signature } }).then(res => res.data);
};
exports.createDummyWin32DeviceToken = createDummyWin32DeviceToken;
