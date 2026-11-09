fun main() {
    val randomize = true

    val x = if (randomize) {
        listOf(1, 2, 3)
    } else {
        listOf(3, 2, 1)
    }.let { list ->
        list.map { it * 10 }
    }

    println(x)
}

main()
