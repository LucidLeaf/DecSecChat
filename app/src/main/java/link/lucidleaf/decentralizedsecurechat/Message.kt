package link.lucidleaf.decentralizedsecurechat

import java.util.Date

class Message(val user: User, val body: String, val createdAt: Date) {
    //todo implement message class

    fun thisUser(): Boolean {
        return false
    }

}

class User(val name:String, val id: Int, val nickName: String) {

}
