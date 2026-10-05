package com.innovii.starhash.core

import kotlinx.coroutines.delay

/**
 * A pretend Telesom network with the real *122# and *400# menus, a balance and an SMS inbox. It lets people try
 * StarHash without a SIM or a charge, and the tests use it to check the runner end to end.
 */
class DemoNetwork(
    var balance: Double = 1.00,
    private val now: () -> Long = System::currentTimeMillis,
    /** How long the pretend network takes to answer, like a real one (1–2 s). */
    private val answerDelayMs: LongRange = 1_000L..2_200L,
    /** When false, subscribing sends the welcome SMS but takes no money (to see a revenue-leak FAIL). */
    var charges: Boolean = true,
) : UssdDriver, SmsSource {
    override val name = "Demo network"
    override val supportsMenus = true

    val inbox = mutableListOf<Sms>()
    val subscriptions = mutableSetOf<String>()

    private var node: Node? = null

    private class Node(val text: String, val next: (String) -> Node? = { null }) {
        val canReply get() = UssdText.options(text).isNotEmpty()
    }

    override suspend fun dial(code: String, timeoutMs: Long): UssdReply {
        delay(answerDelayMs.random())
        val c = UssdText.normalizeCode(code)
        node = when {
            c == "*122#" -> balanceMenu()
            c == "*400#" -> vasMenu()
            c.startsWith("*122*") || c.startsWith("*400*") -> chained(c)
            else -> null
        }
        return show() ?: UssdReply.Failed("Connection problem or invalid MMI code.")
    }

    override suspend fun reply(text: String, timeoutMs: Long): UssdReply {
        delay(answerDelayMs.random())
        val n = node ?: return UssdReply.Failed("No USSD session is open")
        node = n.next(text.trim()) ?: Node("Invalid choice. Please try again.")
        return show()!!
    }

    override suspend fun close() {
        node = null
    }

    override fun since(at: Long): List<Sms> = inbox.filter { it.at >= at }

    private fun show(): UssdReply? = node?.let { UssdReply.Screen(it.text, it.canReply) }

    /** *400*4*1*1*1# style codes, as the one-shot engine sends them. */
    private fun chained(code: String): Node? {
        val parts = code.removePrefix("*").removeSuffix("#").split("*")
        var n: Node? = if (parts[0] == "122") balanceMenu() else vasMenu()
        for (p in parts.drop(1)) n = n?.next?.invoke(p)
        return n
    }

    private fun sms(from: String, body: String) {
        inbox += Sms(from, body, now())
    }

    private fun money(v: Double) = UssdText.money(v)

    // --- *122# ---

    private fun balanceMenu(): Node = Node(
        "***Telesom Company***\n1.Prepaid balance\n2.Internet balance\n3.Kaafayia balance\n" +
            "4.Promotion balance\n5.Unlimited Balance\n6.Language settings",
    ) { choice ->
        when (choice) {
            "1" -> {
                sms("Telesom", "Dear Customer,\nBalance: ${money(balance)}USD\nThank you for using Telesom")
                Node(
                    "Balance: ${money(balance)}USD\nWelcome to the Mama-Khadija service, where you can follow daily " +
                        "lessons on cooking, beauty care, and child care. Please press 1\n1.Subscribe",
                ) { if (it == "1") subscribe("Mama Khadija", 0.10, "Ku soo dhawaw Maama Khadiija.") else null }
            }
            "2" -> Node("Internet balance: 0 MB")
            "3" -> Node("Kaafayia balance: 0.00USD")
            "4" -> Node("Promotion balance: 0.00USD")
            "5" -> Node("Dear Customer, Your Unlimited Package expires on: 06-10-2026 10:07")
            "6" -> Node("Language\n1.Somali\n2.English") { Node("Language saved") }
            else -> null
        }
    }

    // --- *400# ---

    private val services = listOf(
        "MyStatus (ila dareen)", "Ila maqal", "Mobile Education", "Mobile Market", "Iga-Qabo",
        "SMS Groupy", "Antitheft Service", "Jobs", "Maama Khadiija",
    )

    private fun vasMenu(): Node = Node(
        "Main Menu\n" + services.mapIndexed { i, s -> "${i + 1}.$s" }.joinToString("\n") + "\n00.next",
    ) { choice ->
        when (choice) {
            "4" -> mobileMarket()
            "00" -> Node("Main Menu\n10.Quran\n11.Football news\n0.Back") { null }
            else -> choice.toIntOrNull()?.takeIf { it in 1..services.size }?.let { genericService(services[it - 1]) }
        }
    }

    private val cities = listOf("Hargeysa", "Burco", "Berbera", "Boorama", "Laascaanood", "Wajaale", "Ceerigaabo", "Gabilay")

    private fun mobileMarket(): Node = Node(
        "Mobile Market\n1.Furo Mobile Market\n2.Faahfaahinta adeegga\n0.Dib u noqo / Back",
    ) { choice ->
        when (choice) {
            "1" -> Node(
                "fadlan dooro magalaada\n" + cities.mapIndexed { i, c -> "${i + 1}.$c" }.joinToString("\n") +
                    "\n0.Dib u noqo / Back",
            ) { city ->
                if (city.toIntOrNull()?.let { it in 1..cities.size } == true) {
                    Node(
                        "Adeeggan Mobile Market qiimihiisu waa 0.5$ Waxaanu kuu Shaqaynayaa muddo bil ah, marka uu " +
                            "kaa dhamaado waqtiguna waa lagu cusboonaysiinayaa bil walba.\n1.Furo Mobile Market\n0.Dib u no",
                    ) {
                        if (it == "1") {
                            subscribe(
                                "Mobile Market", 0.5,
                                "ku soo dhawaw adeega Mobile market: waa adeeg casri ah oo kuu sahlaaya in aad ka heli " +
                                    "karto alaabooyin la iibinaayo ama la kireynaayo.",
                            )
                        } else {
                            null
                        }
                    }
                } else {
                    null
                }
            }
            "2" -> Node("Mobile Market: iibso oo iibi alaabta adiga oo jooga meel kasta. Qiimaha: 0.5$ bishii.")
            else -> null
        }
    }

    private fun genericService(name: String): Node = Node("$name\n1.Furo $name\n2.Faahfaahinta adeegga\n0.Dib u noqo / Back") { choice ->
        when (choice) {
            "1" -> Node("$name qiimihiisu waa 0.2$ bishii.\n1.Haa\n0.Maya") {
                if (it == "1") subscribe(name, 0.2, "Ku soo dhawaw $name.") else null
            }
            "2" -> Node("$name: adeeg INNOVII ah. Qiimaha: 0.2$ bishii.")
            else -> null
        }
    }

    private fun subscribe(service: String, price: Double, welcome: String): Node {
        if (service in subscriptions) return Node("Horey ayaad ugu diiwaan gashan tahay $service.")
        if (balance + 1e-9 < price) {
            return Node("Hadhaagaagu kuma filna. Balance: ${money(balance)}USD. Fadlan ku shubo lacag.")
        }
        if (charges) balance = UssdText.round2(balance - price)
        subscriptions += service
        sms("400", welcome)
        sms("Telesom", "Hadhagaagu hadda waa: ${money(balance)}, Macmiil,waad ku mahadsantahay isticmaalka Telesom")
        return Node("Mahadsanid! Waxaad ku guuleysatay is-diiwaangelinta $service.")
    }
}
