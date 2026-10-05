package com.innovii.starhash.core

/** One answer from the network. */
sealed interface UssdReply {
    /** A pop-up with text; [canReply] when it has a box to type the next answer. */
    data class Screen(val text: String, val canReply: Boolean) : UssdReply

    /** No pop-up, a phone error, or a timeout. */
    data class Failed(val reason: String) : UssdReply
}

/** Something that can dial a USSD code and answer its menus: the real phone, or [DemoNetwork]. */
interface UssdDriver {
    val name: String

    /** False for one-shot drivers: the runner then sends the whole menu path in one chained code. */
    val supportsMenus: Boolean

    suspend fun dial(code: String, timeoutMs: Long): UssdReply

    suspend fun reply(text: String, timeoutMs: Long): UssdReply

    /** Close the session (press Cancel/OK on the pop-up). Safe to call when nothing is open. */
    suspend fun close()
}

/** The SMS the phone received; the runner asks for the ones that arrived after a moment. */
fun interface SmsSource {
    fun since(at: Long): List<Sms>
}
