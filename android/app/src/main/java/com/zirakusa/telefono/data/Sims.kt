package com.zirakusa.telefono.data

/** One SIM that can call: its slot as the user sees it (1, 2), the phone account's id, the subscription and the carrier. */
data class Sim(val slot: Int, val id: String, val subId: Int = -1, val carrier: String = "")

/** A phone account that can call, as Android's TelecomManager lists it ([subId] when Android can tell). */
data class Handle(val id: String, val subId: Int? = null)

/** An active SIM as SubscriptionManager lists it ([slot] from 0). */
data class SubInfo(val slot: Int, val subId: Int, val iccId: String = "", val carrier: String = "")

/** «SIM por defecto»: ask every time, or always SIM 1 / SIM 2. */
enum class SimChoice { ASK, SIM1, SIM2 }

/** Pure rules of the two SIMs: which account is which slot, which SIM a call used and which one to call with. */
object Sims {
    private fun same(id: String, sub: SubInfo) = id.isNotBlank() &&
        (id == sub.subId.toString() || (sub.iccId.isNotBlank() && id.equals(sub.iccId, ignoreCase = true)))

    /** The SIMs that can call, by slot. A handle with no matching SIM (like a SIP account) is left out. */
    fun match(handles: List<Handle>, subs: List<SubInfo>): List<Sim> = handles.mapNotNull { h ->
        val sub = subs.firstOrNull { (h.subId != null && h.subId == it.subId) || same(h.id.trim(), it) } ?: return@mapNotNull null
        Sim(sub.slot + 1, h.id, sub.subId, sub.carrier)
    }.distinctBy { it.slot }.sortedBy { it.slot }

    /** The slot (1, 2) of the SIM a call used, from the call log's PHONE_ACCOUNT_ID; null when unknown. */
    fun slotOf(account: String, sims: List<Sim>): Int? {
        val a = account.trim()
        if (a.isEmpty()) return null
        return sims.firstOrNull { it.id == a || (it.subId >= 0 && a == it.subId.toString()) }?.slot
    }

    /** The SIM to call with: the only one, or the default one; null = ask (two SIMs and no default) or no SIM known. */
    fun pick(choice: SimChoice, sims: List<Sim>): Sim? = when {
        sims.size == 1 -> sims.first()
        choice == SimChoice.SIM1 -> sims.firstOrNull { it.slot == 1 }
        choice == SimChoice.SIM2 -> sims.firstOrNull { it.slot == 2 }
        else -> null
    }

    /** Two SIMs and no default: Teléfono asks which one on each call. */
    fun mustAsk(choice: SimChoice, sims: List<Sim>) = sims.size >= 2 && pick(choice, sims) == null
}
