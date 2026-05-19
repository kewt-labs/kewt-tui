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
package dev.kewt.terminal

public sealed class Key {
    public data class Char(val char: kotlin.Char) : Key()

    public data object Enter : Key()
    public data object Escape : Key()
    public data object Backspace : Key()
    public data object Delete : Key()
    public data object Tab : Key()
    public data object BackTab : Key()
    public data object Up : Key()
    public data object Down : Key()
    public data object Left : Key()
    public data object Right : Key()
    public data object Home : Key()
    public data object End : Key()
    public data object PageUp : Key()
    public data object PageDown : Key()
    public data object Insert : Key()

    /**
     * A function key (F1-F12)
     *
     * @property n the function key number (1-12)
     */
    public data class F(val n: Int) : Key()
}
