package ru.skillbranch.kotlinexample

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom


@SuppressLint("DefaultLocale")
class User private constructor(
    private val firstName: String,
    private val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    var meta: Map<String, Any>? = null
) {

    val userInfo: String

    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .capitalize()

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().toUpperCase() }
            .joinToString(" ")

    private var phone: String? = null
        set(value) {
            field = value?.replace("[^+\\d]".toRegex(), "")
        }

    private var _login: String? = null

    var login: String
        set(value) {
            _login = value.toLowerCase()
        }
        get() = _login!!

    private var salt: String = ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    // for email
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        password: String,
        metat: Map<String, Any>? = null,
        tatat: String? = null
    ): this(firstName, lastName, email,meta = metat ?: mapOf("auth" to "password")) {
        println("Secondary mail constructor")
        passwordHash = encrypt(password)
    }

    // for phone
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String?,
        metat: Map<String, Any>? = null
    ): this(firstName, lastName, rawPhone = rawPhone, meta = metat ?: mapOf("auth" to "sms")) {
        println("Secondary phone constructor")
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        sendAccessCodeToUser(phone!!, code)
    }

    // for csv
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        salt: String,
        passwordHash: String
    ): this(firstName, lastName, email = email, meta = mapOf("src" to "csv")) {
        println("Secondary csv constructor")
        this.salt = salt
        this.passwordHash = passwordHash
        val code = generateAccessCode()
        this.accessCode = code
    }

    init {
        println("First init block, primary constructor was called")

        check(!firstName.isBlank()) { "FirstName must be not blank" }
        check(email.isNullOrBlank() || rawPhone.isNullOrBlank()) { "Email or phone must be not blank" }

        phone = rawPhone
        login = email ?: phone!!

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
        """.trimIndent()
    }

    fun checkPassword(pass: String) = encrypt(pass) == passwordHash

    fun changeAccessCode() {
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        sendAccessCodeToUser(phone!!, code)
    }

    fun changePassword(oldPass: String, newPass: String) {
        if(checkPassword(oldPass)) passwordHash = encrypt(newPass)
        else throw IllegalArgumentException("The entered password does not match the current password")
    }

    private fun encrypt(password: String) = salt.plus(password).md5()

    private fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    private fun sendAccessCodeToUser(phone: String, code: String) {
        println("..... sending access code: $code on $phone")
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray())
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    companion object Factory{
        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null,
            salt: String? = null,
            passwordHash: String? = null,
            meta: Map<String, Any>? = null
        ): User{
            val (firstName, lastName) = fullName.fullNameToPair()

            return when{
                !phone.isNullOrBlank() -> User(firstName, lastName, rawPhone = phone, metat = meta)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(firstName, lastName, email, password, metat = meta)
                !email.isNullOrBlank() &&
                        !salt.isNullOrBlank() &&
                        !passwordHash.isNullOrBlank() -> User(firstName, lastName, email, salt, passwordHash)
                else -> throw java.lang.IllegalArgumentException("Email or phone must be not null or blank")
            }
        }

        /**
         * Parse User from csv string.
         *
         * String structure: full name; email; salt:hash; phone
         */
        fun parseFromCsv(data: String): User {
            data.split(";").also {
                val fullName = it[0].trim()
                val email = it[1].trim()
                val (salt, hash) = it[2].trim().split(":", limit = 2)
                val phone = it[3]

                return makeUser(
                    fullName,
                    email,
                    phone = phone,
                    salt = salt,
                    passwordHash = hash
                ).apply {
                    meta = mapOf("src" to "csv")
                    this.salt = salt
                    passwordHash = hash
                    println(userInfo)
                }
            }

        }

        private fun String.fullNameToPair(): Pair<String, String?> {
            return this.split(" ")
                .filter { it.isNotBlank() }
                .run {
                    when(size) {
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw java.lang.IllegalArgumentException(
                            "Fullname must contain only first name and last name, " +
                                    "current split result ${this@fullNameToPair}"
                        )
                    }
                }
        }
    }
}
