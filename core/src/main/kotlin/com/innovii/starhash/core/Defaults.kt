package com.innovii.starhash.core

/** The tests a new install starts with: the Telesom codes INNOVII runs on. */
object Defaults {
    const val BALANCE_CODE = "*122#"
    const val VAS_CODE = "*400#"

    fun tests(): List<TestCase> = listOf(
        TestCase(
            id = "balance", name = "Balance (*122# → 1)", kind = Kind.BALANCE,
            note = "Reads the prepaid balance; the code and menu answers are in Settings.",
        ),
        TestCase(
            id = "122-menu", name = "*122# menu answers", kind = Kind.MENU,
            code = BALANCE_CODE, expectText = "Prepaid balance, balance",
        ),
        TestCase(
            id = "400-menu", name = "*400# main menu", kind = Kind.MENU,
            code = VAS_CODE, expectText = "Main Menu, MyStatus, Mobile Market",
        ),
        TestCase(
            id = "400-explore", name = "*400# every service opens", kind = Kind.EXPLORE,
            code = VAS_CODE,
            note = "Opens each item of the main menu once and checks the network answers. Never subscribes.",
        ),
        TestCase(
            id = "400-market", name = "Mobile Market menu", kind = Kind.MENU,
            code = VAS_CODE, path = listOf("4"), expectText = "Mobile Market",
        ),
        TestCase(
            id = "400-market-sub", name = "Subscribe: Mobile Market (Hargeysa)", kind = Kind.SUBSCRIBE,
            code = VAS_CODE, path = listOf("4", "1", "1", "1"), price = 0.5,
            smsFrom = "400", smsText = "Mobile market",
            enabled = false,
            note = "Main Menu 4 → Furo Mobile Market 1 → Hargeysa 1 → confirm 1. Costs 0.5 USD a month.",
        ),
    )
}
