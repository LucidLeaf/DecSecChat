package link.lucidleaf.decentralizedsecurechat

import android.content.Context
import android.util.Base64
import android.widget.Toast
import java.io.File
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher


object Encryption {
    private var loaded = false
    private var publicKey: PublicKey? = null
    private var privateKey: PrivateKey? = null
    private var keyGen: KeyPairGenerator? = null
    private val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)

    init {
        val kG = KeyPairGenerator.getInstance(ENCRYPTION_ALGORITHM)
        kG.initialize(1024)
        keyGen = kG
    }

    fun loadKeys(context: Context) {
        val publicKeyFile = File(context.filesDir, FILE_PUBLIC_KEY)
        val privateKeyFile = File(context.filesDir, FILE_PRIVATE_KEY)
        if (publicKeyFile.exists() && privateKeyFile.exists()) {
            val factory: KeyFactory = KeyFactory.getInstance(ENCRYPTION_ALGORITHM)
            try {
                val publicEncodedKey = readKey(publicKeyFile)
                val privateEncodedKey = readKey(privateKeyFile)
                publicKey = factory.generatePublic(X509EncodedKeySpec(publicEncodedKey))
                privateKey = factory.generatePrivate(PKCS8EncodedKeySpec(privateEncodedKey))
            } catch (e: java.lang.IllegalArgumentException) {
                Toast.makeText(
                    context,
                    "Error reading key files, generating new pair",
                    Toast.LENGTH_SHORT
                ).show()
                generateNewKeys(publicKeyFile, privateKeyFile)
            }
        } else {
            generateNewKeys(publicKeyFile, privateKeyFile)
        }
        loaded = true
    }

    private fun generateNewKeys(publicKeyFile: File, privateKeyFile: File) {
        val pair = keyGen?.generateKeyPair()
        if (pair != null) {
            privateKey = pair.private
            publicKey = pair.public
            writeKey(publicKeyFile, publicKey!!)
            writeKey(privateKeyFile, privateKey!!)
        }
    }

    private fun writeKey(keyFile: File, key: Key) {
        val keyEncoded = key.encoded
        val keyString = Base64.encodeToString(keyEncoded, Base64.DEFAULT)
        keyFile.writeText(keyString)
    }

    private fun readKey(keyFile: File): ByteArray {
        val keyFileLines = keyFile.readText()
        return Base64.decode(keyFileLines, Base64.DEFAULT)
    }

    fun encryptMessage(body: String, publicKey: String): String {
        //create key from string
        val factory: KeyFactory = KeyFactory.getInstance(ENCRYPTION_ALGORITHM)
        val decodedKey = Base64.decode(publicKey, Base64.DEFAULT)
        val key = factory.generatePublic(X509EncodedKeySpec(decodedKey))
        //encrypt text
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encryptedText = cipher.doFinal(body.toByteArray())
        val encodedText =  Base64.encodeToString(encryptedText, Base64.DEFAULT)
        return encodedText
    }

    fun decryptMessage(body: String): String {
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        val encryptedText = Base64.decode(body, Base64.DEFAULT)
        val decryptedText = cipher.doFinal(encryptedText)
        return String(decryptedText)
    }

    fun getPublicKey(): String? {
        return publicKey?.encoded?.let { Base64.encodeToString(it, Base64.DEFAULT) }
    }

}