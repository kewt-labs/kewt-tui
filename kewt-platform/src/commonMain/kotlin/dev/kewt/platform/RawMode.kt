/*
* Copyright 2026 Kewt Labs
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* */
package dev.kewt.platform

/**
 * Manages the terminal's raw mode state.
 *
 * Raw mode disables line buffering and local echo, allowing the application
 * to process individual key presses as they occur.
 */
public expect object RawMode {
    /**
     * Enters terminal raw mode.
     */
    public fun enter()

    /**
     * Exits terminal raw mode and restores the original terminal state.
     */
    public fun exit()
}
