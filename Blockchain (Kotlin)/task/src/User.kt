package blockchain

import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature

data class User(val name: String) {
    override fun toString(): String = name

    @Volatile
    private var _virtualCoins = 100u
    val virtualCoins: UInt
        @Synchronized
        get() = _virtualCoins

    private var _publicKey: PublicKey
    private var _privateKey: PrivateKey

    init {
        val keypair = KeyPairGenerator.getInstance("RSA").genKeyPair()
        _publicKey = keypair.public
        _privateKey = keypair.private
    }

    data class Transaction(val from: User, val to: User, val amount: UInt) {
        override fun toString(): String = "$from sent $amount VC to $to ($timestamp)"
        fun toByteArray(): ByteArray = toString().toByteArray()
        val timestamp = System.currentTimeMillis()
        private var _signature: ByteArray = from.sign(toString())

        val signature: ByteArray
            get() = _signature
    }

    private fun sign(data: String): ByteArray {
        val rsa = Signature.getInstance("SHA1withRSA")
        rsa.initSign(_privateKey)
        rsa.update(data.toByteArray())
        return rsa.sign()
    }

    fun verify(transaction: Transaction): Boolean {
        val sig = Signature.getInstance("SHA1withRSA")
        sig.initVerify(_publicKey)
        sig.update(transaction.toByteArray())

        return sig.verify(transaction.signature)
    }

    @Synchronized
    fun sendVirtualCoins(user: User, amount: UInt): Transaction {
        assert(_virtualCoins >= amount) {"Invalid transaction = $amount > $_virtualCoins"}
        _virtualCoins -= amount
        return Transaction(from = this, to = user, amount = amount)
    }

    @Synchronized
    fun receiveVirtualCoins(transaction: Transaction) {
        assert(transaction.to.name == this.name) {"Invalid transaction - ${transaction.to.name} != $name"}
        assert(USERS[transaction.from.name]?.verify(transaction) ?: false) {"Invalid transaction - failed to verify"}
        _virtualCoins += transaction.amount
    }

    @Synchronized
    fun receiveMiningReward(block: Block, minerReward: UInt) {
        assert(block.miner == name) { "Invalid miner name - ${block.miner} != $name" }
        _virtualCoins += minerReward
    }
}