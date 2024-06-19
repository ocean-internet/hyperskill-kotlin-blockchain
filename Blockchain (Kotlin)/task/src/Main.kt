package blockchain

import kotlin.math.min
import kotlin.random.Random
import kotlin.random.nextUInt

val USERS: MutableMap<String, User> = setOf(
    "Marley Lu",
    "Duncan Abbott",
    "Melany Sexton",
    "Mylo Bryan",
    "Meredith Giles",
    "Kole Atkins",
    "Mina Wyatt",
    "Sam Sexton",
    "Ellen Malone",
    "Ruben Boyd",
).map { User(name = it) }.associateBy { it.name }.toMutableMap()

fun main() {

    val chain = Chain()
    val threads = (1..10).map {
        val name = "miner${it}"
        USERS[name] = User(name)
        MinerThread(chain, name)
    }.toList()
    threads.forEach { it.start() }

    while (chain.blocks.size < MAX_BLOCKS) Thread.sleep(Random.nextInt(from = 1, until = 50).toLong()).run {
        synchronized("sendTransaction") {
            val from = USERS.values.filter { it.virtualCoins > 0u }.random()
            val amount = min(Random.nextUInt(50u), from.virtualCoins)
            val to = USERS.values.filter { it.name != from.name }.random()

            val transaction = from.sendVirtualCoins(to, amount)

            chain.addTransaction(transaction)
        }
    }

    threads.forEach { it.join() }

    //println(USERS.map { "${it.key}: ${it.value.virtualCoins}" }.joinToString("\n"))
}
