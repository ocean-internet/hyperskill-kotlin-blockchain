package blockchain

import java.security.MessageDigest

class Block(
    val miner: String,
    val id: UInt,
    val magicNumber: UInt,
    val transactions: List<User.Transaction>,
    val previousHash: String,
    val previousTimestamp: Long
) {
    companion object {
        const val INITIAL_HASH = 0x0.toString()
    }

    val timeStamp = System.currentTimeMillis()

    val hash: String = MessageDigest.getInstance("SHA-256")
        .digest(
            listOf(
                id,
                timeStamp,
                previousHash,
                magicNumber,
                transactions
            )
                .joinToString()
                .toByteArray()
        )
        .joinToString("") { "%02x".format(it) }
}