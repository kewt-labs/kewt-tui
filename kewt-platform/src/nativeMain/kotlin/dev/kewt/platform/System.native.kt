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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.O_RDWR
import platform.posix.STDERR_FILENO
import platform.posix.STDIN_FILENO
import platform.posix.STDOUT_FILENO
import platform.posix.getenv
import platform.posix.isatty
import platform.posix.open

@OptIn(ExperimentalForeignApi::class)
public actual fun currentTimeMs(): Long = currentTimeMsInternal()

@OptIn(ExperimentalForeignApi::class)
public actual fun getEnv(name: String): String? = getenv(name)?.toKString()

/**
 * Global cache for the TTY file descriptor to ensure consistency across modules.
 */
private var cachedTtyFd: Int = -1

/**
 * Finds the best file descriptor for TTY operations, caching it for subsequent calls.
 *
 * For TUIs, we prefer a descriptor that supports both read and write.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun findTtyFd(): Int {
    if (cachedTtyFd >= 0) return cachedTtyFd

    // Try to open /dev/tty directly first for bidirectional communication
    // This is most reliable when standard streams are redirected (e.g. by Gradle)
    val directTty = open("/dev/tty", O_RDWR)
    if (directTty >= 0) {
        cachedTtyFd = directTty
        return cachedTtyFd
    }

    // Fallback to standard streams if /dev/tty is not available
    cachedTtyFd = when {
        isatty(STDOUT_FILENO) != 0 -> STDOUT_FILENO
        isatty(STDIN_FILENO) != 0 -> STDIN_FILENO
        isatty(STDERR_FILENO) != 0 -> STDERR_FILENO
        else -> -1
    }

    return cachedTtyFd
}

internal expect fun currentTimeMsInternal(): Long
