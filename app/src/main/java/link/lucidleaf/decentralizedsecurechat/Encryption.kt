package link.lucidleaf.decentralizedsecurechat

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import androidx.security.crypto.MasterKeys
import java.io.File
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.SecretKey

object Encryption {
    private var loaded = false
    private var publicKey: PublicKey? = null
    private var privateKey: PrivateKey? = null
    private var keyGen: KeyPairGenerator? = null

    init {
        val kG = KeyPairGenerator.getInstance("RSA")
        kG.initialize(1024)
        keyGen = kG
    }


    fun loadKeys(context: Context) {
        val publicKeyFile = File(context.filesDir, FILE_PUBLIC_KEY)
        val privateKeyFile = File(context.filesDir, FILE_PRIVATE_KEY)
        if (publicKeyFile.exists() && privateKeyFile.exists()) {
            val publicKeyContents = publicKeyFile.readText()
            val privateKeyContents = privateKeyFile.readText()
        } else {
            val pair = keyGen?.generateKeyPair()
            if (pair != null) {
                privateKey = pair.private
                publicKey = pair.public
            }
            print("PRIVATE KEY $privateKey--------------------------------------------")
            print(publicKey)
        }
        loaded = true
    }

    fun encryptMessage(body: String): String {
        if (!loaded) {
            Log.e(TAG, "keys are not loaded in yet")
        }
        return ""
    }

    fun decryptMessage(body: String, publicKey: String): String {
        return body
    }

    fun getPublicKey(): String {
        return publicKey.toString()
    }


}