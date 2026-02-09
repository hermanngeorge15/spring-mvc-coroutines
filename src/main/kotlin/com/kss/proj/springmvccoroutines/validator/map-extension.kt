package com.kss.proj.springmvccoroutines.validator

fun Map<*,*>.getByPath(path: String): Any? {
    val segments = path.split(".")
    var current: Any? = this
    for (segment in segments) {
        if (current !is Map<*, *>) return null
        current = current[segment]
    }
    return current
}

fun Map<*, *>.toStringKeyMap(): MutableMap<String, Any?> =
    entries.associate { (k, v) -> k.toString() to v }.toMutableMap()

fun Map<*, *>.setByPath(path: String, value: Any?): Map<String, Any?> {
    val segments = path.split(".")
    val mutable = toStringKeyMap()
    if (segments.size == 1) {
        mutable[path] = value
        return mutable
    }

    val head = segments.first()
    val rest = segments.drop(1).joinToString(".")
    val child = (this[head] as? Map<*, *>) ?: emptyMap<String, Any?>()
    return mutable.apply {
        put(head, child.setByPath(rest, value))
    }
}

fun isBlankOrEmpty(value: Any?): Boolean = when (value) {
    null -> true
    is String -> value.isBlank()
    else -> false
}