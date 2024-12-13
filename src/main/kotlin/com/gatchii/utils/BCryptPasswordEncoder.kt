package com.gatchii.utils

import org.mindrot.jbcrypt.BCrypt

class BCryptPasswordEncoder {

    fun encode(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    fun matches(password: String, hashed: String): Boolean {
        return BCrypt.checkpw(password, hashed)
    }

}
