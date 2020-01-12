package ru.skillbranch.kotlinexample.extensions


fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T> {
    return if (isNotEmpty()) {
        val iterator = listIterator(size)
        while (iterator.hasPrevious()) {
            if (predicate(iterator.previous())) {
                return slice(0..iterator.previousIndex())
            }
        }
        emptyList()
    } else {
        emptyList()
    }
}