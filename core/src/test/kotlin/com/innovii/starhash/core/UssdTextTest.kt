package com.innovii.starhash.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UssdTextTest {
    private val menu122 = "***Telesom Company***\n1.Prepaid balance\n2.Internet balance\n3.Kaafayia balance\n" +
        "4.Promotion balance\n5.Unlimited Balance\n6.Language settings"

    @Test
    fun `reads the balance from the menu and the SMS`() {
        assertEquals(0.02, UssdText.balance("Balance: 0.02USD\nWelcome to the Mama-Khadija service ... Please press 1\n1.Subscribe"))
        assertEquals(0.02, UssdText.balance("Hadhagaagu hadda waa: 0.02,\nMacmiil,waad ku mahadsantahay isticmaalka Telesom"))
        assertEquals(12.5, UssdText.balance("Dear Customer,\nBalance: 12.50USD\nThank you for using Telesom"))
        assertEquals(3.0, UssdText.balance("Your balance is \$3"))
    }

    @Test
    fun `menu option names are not a balance`() {
        assertNull(UssdText.balance(menu122))
        assertNull(UssdText.balance("1.Prepaid balance 2.Internet balance"))
    }

    @Test
    fun `custom balance pattern wins`() {
        assertEquals(7.25, UssdText.balance("Credit left 7.25 dollars", """credit left ([0-9.]+)"""))
        assertEquals(0.02, UssdText.balance("Balance: 0.02USD", "((("))
    }

    @Test
    fun `reads the price and ignores the balance`() {
        assertEquals(0.5, UssdText.price("Adeeggan Mobile Market qiimihiisu waa 0.5\$ Waxaanu kuu Shaqaynayaa muddo bil ah"))
        assertEquals(0.1, UssdText.price("Subscribe for USD 0.10 per week"))
        assertNull(UssdText.price("Balance: 0.02USD\nWelcome to the Mama-Khadija service"))
        assertNull(UssdText.price(menu122))
    }

    @Test
    fun `menu options`() {
        val o = UssdText.options("Main Menu\n1.MyStatus (ila dareen)\n4.Mobile Market\n00.next")
        assertEquals(listOf("1", "4", "00"), o.map { it.key })
        assertEquals("Mobile Market", o[1].label)
        assertTrue(UssdText.isBackOption(MenuOption("0", "Dib u noqo / Back")))
        assertFalse(UssdText.isBackOption(MenuOption("4", "Mobile Market")))
    }

    @Test
    fun `errors and progress`() {
        assertNotNull(UssdText.errorIn("Connection problem or invalid MMI code."))
        assertNull(UssdText.errorIn(menu122))
        assertTrue(UssdText.isProgress("USSD code running…"))
        assertFalse(UssdText.isProgress(menu122))
    }

    @Test
    fun `subscription answers`() {
        assertEquals(Outcome.SUCCESS, UssdText.outcome("Mahadsanid! Waxaad ku guuleysatay is-diiwaangelinta Mobile Market."))
        assertEquals(Outcome.INSUFFICIENT, UssdText.outcome("Hadhaagaagu kuma filna. Fadlan ku shubo lacag."))
        assertEquals(Outcome.INSUFFICIENT, UssdText.outcome("Sorry, insufficient balance"))
        assertEquals(Outcome.ALREADY, UssdText.outcome("You are already subscribed"))
        assertEquals(Outcome.UNKNOWN, UssdText.outcome("Mobile Market"))
    }

    @Test
    fun `codes and paths`() {
        assertEquals("*400*4*1*1*1#", UssdText.chain("*400#", listOf("4", "1", "1", "1")))
        assertEquals("*122#", UssdText.chain("*122", emptyList()))
        assertEquals(listOf("4", "1", "1", "1"), UssdText.parsePath("4 1, 1 → 1"))
        assertEquals(listOf("4", "00"), UssdText.parsePath("4*00"))
    }
}
