@file:JvmName("Log")

package android.util

fun isLoggable(tag: String, level: Int): Boolean = true

fun d(tag: String, msg: String, tr: Throwable?): Int = 0