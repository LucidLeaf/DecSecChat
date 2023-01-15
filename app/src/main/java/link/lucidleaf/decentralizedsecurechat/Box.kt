package link.lucidleaf.decentralizedsecurechat

import android.content.Intent
import java.util.*

//https://stackoverflow.com/a/62406575
object Box {
    private var Number = 1
    private fun nextNumber(): Int {
        return Number++
    }
    private const val _Intent_Identifier = "_Intent_Identifier"
    private val DeleteList = HashMap<Int, Vector<Int>>()
    private val ObjectList = HashMap<Int, HashMap<String, Any>>()
    private fun getIntentIdentifier(I: Intent): Int {
        var Intent_Identifier = I.getIntExtra(_Intent_Identifier, 0)
        if (Intent_Identifier == 0) I.putExtra(_Intent_Identifier, nextNumber().also {
            Intent_Identifier = it
        })
        return Intent_Identifier
    }

    fun add(I: Intent, Name: String, O: Any) {
        val Intent_Identifier = getIntentIdentifier(I)
        synchronized(ObjectList) {
            if (!ObjectList.containsKey(Intent_Identifier)) ObjectList[Intent_Identifier] =
                HashMap()
            ObjectList[Intent_Identifier]!!.put(Name, O)
        }
    }

    fun <T> get(I: Intent, Name: String): T? {
        val Intent_Identifier = getIntentIdentifier(I)
        synchronized(DeleteList) { DeleteList.remove(Intent_Identifier) }
        return ObjectList[Intent_Identifier]!![Name] as T?
    }

    fun remove(I: Intent) {
        val Intent_Identifier = getIntentIdentifier(I)
        val ThreadID = nextNumber()
        synchronized(DeleteList) {
            if (!DeleteList.containsKey(Intent_Identifier)) DeleteList[Intent_Identifier] =
                Vector()
            DeleteList[Intent_Identifier]!!.add(ThreadID)
        }
        Thread {
            try {
                Thread.sleep((60 * 1000).toLong())
            } catch (_: InterruptedException) {
            }
            synchronized(DeleteList) {
                if (DeleteList.containsKey(Intent_Identifier)) if (DeleteList[Intent_Identifier]!!
                        .contains(ThreadID)
                ) synchronized(ObjectList) {
                    ObjectList.remove(
                        Intent_Identifier
                    )
                }
            }
        }.start()
    }
}