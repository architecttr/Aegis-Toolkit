package com.d4rk.cleaner.core.utils.helpers

import java.io.File

/**
 * Returns true if this file is located under a protected Android directory
 * such as `/Android/data` or `/Android/obb`.
 */
fun File.isProtectedAndroidDir(): Boolean {
    val segments = absolutePath.split(File.separatorChar).filter { it.isNotEmpty() }
    segments.forEachIndexed { index, segment ->
        if (segment == "Android") {
            val next = segments.getOrNull(index + 1)
            if (next == "data" || next == "obb") {
                return true
            }
        }
    }
    return false
}
