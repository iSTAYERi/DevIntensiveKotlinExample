package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User {
        return User.makeUser(fullName, email, password)
            .also {
                if (!map.containsKey(it.login)) {
                    map[it.login] = it
                } else {
                    throw IllegalArgumentException("A user with this email already exists")
                }
            }
    }

    fun registerUserByPhone(
        fullName: String,
        rawPhone: String
    ): User {
        return User.makeUser(fullName, phone = rawPhone)
            .also {
                if (!map.containsKey(rawPhone)) {
                    map[rawPhone] = it
                } else {
                    throw IllegalArgumentException("A user with this phone already exists")
                }
                if (!it.login.matches("[+][\\d]{11}".toRegex())) {
                    throw IllegalArgumentException("Enter a valid phone number starting " +
                            "with a + and containing 11 digits")
                }
            }
    }

    fun loginUser(login: String, password: String): String?{
        return map[login.trim()]?.run {
            if (checkPassword(password)) this.userInfo
            else null
        }
    }

    fun requestAccessCode(login: String) {
        //TODO implement function
        map[login].also {

        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder(){
        map.clear()
    }
}