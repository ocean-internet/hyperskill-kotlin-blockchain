package blockchain

import blockchain.Block.Companion.INITIAL_HASH
import java.util.concurrent.ConcurrentLinkedDeque

private const val MINER_REWARD = 100u
private const val MIN_ZEROS = 1u
private const val MAX_ZEROS = 3u

private const val TIMESTAMP_LEEWAY = 10

class Chain {

    private val startTime = System.currentTimeMillis()

    @Volatile
    private var _numZeros: UInt = 1u
    private val numZeroes: UInt
        @Synchronized
        get() = _numZeros

    @Synchronized
    private fun updateNumZeroes(block: Block) {

        val mineTime = block.timeStamp - block.previousTimestamp
        when {
            mineTime > 2000 && _numZeros > MIN_ZEROS -> _numZeros--
            mineTime < 1000 && _numZeros < MAX_ZEROS -> _numZeros++
            else -> {}
        }
    }

    private val _blocks: ConcurrentLinkedDeque<Block> = ConcurrentLinkedDeque()
    val lastBlock: Block?
        @Synchronized
        get() = _blocks.peekLast()
    val lastId: UInt
        @Synchronized
        get() = lastBlock?.id ?: 0u
    val lastHash: String
        @Synchronized
        get() = lastBlock?.hash ?: INITIAL_HASH
    val lastTimestamp: Long
        @Synchronized
        get() = lastBlock?.timeStamp ?: startTime

    private val _transactions: ConcurrentLinkedDeque<User.Transaction> = ConcurrentLinkedDeque()
    var transactions: List<User.Transaction> = emptyList()

    val blocks: List<Block>
        @Synchronized
        get() = _blocks.toList()

    val size: Int
        @Synchronized
        get() = blocks.size

    val numZeros: UInt
        @Synchronized
        get() = _numZeros

    @Synchronized
    fun addBlock(block: Block) {
        assert(block.id.toInt() == lastId.toInt() + 1) { "Invalid ID" }
        assert(block.previousHash == lastHash) { "Invalid previousHash" }
        assert(block.hash.startsWith("0".repeat(numZeros.toInt()))) { "Invalid numZeros" }
        block.transactions.forEach {
            assert(USERS[it.from.name]?.verify(it) ?: false) { "Invalid signature" }
            assert(it.timestamp <= block.previousTimestamp + TIMESTAMP_LEEWAY) { "Invalid timestamp - after previousTimestamp" }
            assert(
                it.timestamp >= (lastBlock?.previousTimestamp ?: 0) - TIMESTAMP_LEEWAY
            ) { "Invalid timestamp - before lastBlock.previousTimestamp - ${it.timestamp} >= ${lastBlock?.previousTimestamp ?: 0}" }
        }
        assert(block.transactions.map { it.toString() }
            .toSet().size == block.transactions.size) { "Invalid - messages not unique" }

        _blocks.add(block)

        transactions = _transactions.toList()
        _transactions.clear()

        USERS[block.miner]?.receiveMiningReward(block, MINER_REWARD)
        block.transactions.forEach { USERS[it.to.name]?.receiveVirtualCoins(it) }

        updateNumZeroes(block)
        debug(block)
    }

    @Synchronized
    fun addTransaction(transaction: User.Transaction) {
        _transactions.add(transaction)
    }

    private fun debug(block: Block) {
        val zeroCount = "^(0+)".toRegex().find(block.hash)?.value?.length ?: 0
        val generatingSeconds = (block.timeStamp - block.previousTimestamp).toFloat() / 1000
        println(
            """
        |Block:
        |Created by: ${block.miner}
        |${block.miner} gets $MINER_REWARD VC
        |Id: ${block.id}
        |Timestamp: ${block.timeStamp}
        |Magic number: ${block.magicNumber}
        |Hash of the previous block:
        |${block.previousHash}
        |Hash of the block:
        |${block.hash}
        |Block data:
        |${
                when {
                    block.transactions.isNotEmpty() -> "    ${block.transactions.joinToString("\n    ")}"
                    else -> "    No transactions"
                }
            }
        |Block was generating for ${generatingSeconds.toString().format("%.3f")} seconds
        |N ${
                when {
                    numZeroes > zeroCount.toUInt() -> "was increased to $numZeroes"
                    numZeroes < zeroCount.toUInt() -> "was decreased to $numZeroes"
                    else -> "stays the same"
                }
            }

                """.trimMargin()
        )
    }
}