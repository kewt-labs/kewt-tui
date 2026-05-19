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

public enum class ColorMode {
    NoColor,
    Basic,
    Extended,
    TrueColor,
    ;

    public companion object {
        public fun detect(): ColorMode {
            val colorTerm = getEnv("COLORTERM") ?: ""
            if (colorTerm == "truecolor" || colorTerm == "24bit") return TrueColor
            val term = getEnv("TERM") ?: ""
            if (term == "dumb") return NoColor
            if ("256color" in term) return Extended
            return TrueColor
        }
    }
}

internal expect fun getEnv(name: String): String?
