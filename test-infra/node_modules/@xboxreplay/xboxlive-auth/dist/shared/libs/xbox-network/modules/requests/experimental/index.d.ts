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
import type { XNETDummyDeviceTokenResponse } from '../requests.types';
/**
 * Creates a dummy Win32 device token for Xbox Network authentication (experimental)
 * @warning This is a workaround for the Xbox Network authentication and the associated device ID may be banned by Xbox Network in the future
 * @returns {Promise<XNETDummyDeviceTokenResponse>} The dummy device token response
 */
export declare const createDummyWin32DeviceToken: () => Promise<XNETDummyDeviceTokenResponse>;
