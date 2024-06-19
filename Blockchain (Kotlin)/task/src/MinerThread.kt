package blockchain

import kotlin.random.Random
import kotlin.random.nextUInt

const val MAX_BLOCKS = 15

class MinerThread(private val chain: Chain, private val minerName: String) : Thread(minerName) {
    override fun run() {
        while (chain.size < MAX_BLOCKS) mineBlock()
    }

    private fun mineBlock() {

        val block = Block(
            miner = minerName,
            id = (chain.lastId + 1u),
            magicNumber = Random.nextUInt(),
            transactions = chain.transactions,
            chain.lastHash,
            chain.lastTimestamp,
        )

        val isValid = block.hash.startsWith("0".repeat(chain.numZeros.toInt()))

        if (!isValid) return

        try {
            chain.addBlock(block)
        } catch (error: Throwable) {
            //println("[ERROR]: $error")
        }
    }
}