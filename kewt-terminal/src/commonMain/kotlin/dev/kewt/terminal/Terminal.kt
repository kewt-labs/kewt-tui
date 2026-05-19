/*
* Copyright 2026 lscythe
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
package dev.kewt.terminal

import dev.kewt.platform.Size

public interface Terminal {
    public fun enterRawMode()
    public fun exitRawMode()
    public fun size(): Size
    public fun read(): Event?
    public fun write(text: String)
    public fun flush()
    public fun moveCursor(x: Int, y: Int)
    public fun hideCursor()
    public fun showCursor()
    public fun clear()
    public fun clearLine()
}
