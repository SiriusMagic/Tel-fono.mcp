package com.zirakusa.contactos.data

/** What an account is, for its badge and for «Guardar en». MESSENGER (WhatsApp, Telegram…) is read only. */
enum class AccountKind { PHONE, SIM, GOOGLE, MESSENGER, OTHER }

/** Where part of a contact is saved: an account of the phone (Google, SIM…) or the phone itself ([type] null). */
data class Account(val type: String? = null, val name: String? = null) {
    val kind get() = Accounts.kind(type)
    /** The SIM's slot (1, 2) when its name says it («SIM1», «SIM 2»). */
    val slot get() = if (kind == AccountKind.SIM) name?.lastOrNull { it.isDigit() }?.digitToInt()?.takeIf { it in 1..4 } else null
    /** What the settings keep: «type|name», and «» for the phone without account. */
    val key get() = if (type.isNullOrEmpty()) "" else "$type|${name.orEmpty()}"

    companion object {
        val PHONE = Account()
        fun of(key: String?): Account? = when {
            key == null -> null
            key.isEmpty() -> PHONE
            else -> Account(key.substringBefore('|'), key.substringAfter('|', "").ifEmpty { null })
        }
    }
}

/** One raw contact: the part of a person saved in one account (Android joins several into one contact). */
data class Raw(val id: Long, val account: Account = Account.PHONE)

/** The small mark on a picture: a SIM (with its slot) or Google. */
data class Badge(val kind: AccountKind, val slot: Int? = null)

/** Pure rules about accounts: what each one is, which can be written, the badge, and the «Guardar en» list. */
object Accounts {
    private val messengers = listOf("whatsapp", "telegram", "signal", "thoughtcrime", "viber", "tachyon", "skype", "facebook", "messenger", "line.android")

    fun kind(type: String?): AccountKind {
        val t = type?.lowercase().orEmpty()
        return when {
            t.isEmpty() -> AccountKind.PHONE
            messengers.any { it in t } -> AccountKind.MESSENGER
            "sim" in t || "icc" in t -> AccountKind.SIM
            t == "com.google" -> AccountKind.GOOGLE
            "phone" in t || "local" in t -> AccountKind.PHONE
            else -> AccountKind.OTHER
        }
    }

    /** A SIM before Google; nothing for the phone (most contacts), nor for messengers. */
    fun badge(raws: List<Raw>): Badge? {
        val accounts = raws.map { it.account }
        accounts.firstOrNull { it.kind == AccountKind.SIM }?.let { return Badge(AccountKind.SIM, it.slot) }
        return if (accounts.any { it.kind == AccountKind.GOOGLE }) Badge(AccountKind.GOOGLE) else null
    }

    /** «Guardar en»: each account once, without messengers; the phone first, then SIMs by slot, Google, the rest. */
    fun choices(found: List<Account>): List<Account> {
        val usable = found.map { if (it.type.isNullOrEmpty()) Account.PHONE else it }.distinct().filter { it.kind != AccountKind.MESSENGER }
        // A phone with its own «local phone» account doesn't need the account-less one too.
        val all = if (usable.any { it.kind == AccountKind.PHONE }) usable else listOf(Account.PHONE) + usable
        val list = if (all.count { it.kind == AccountKind.PHONE } > 1) all.filter { it != Account.PHONE } else all
        return list.sortedWith(compareBy<Account>({ it.kind.ordinal }, { it.slot ?: 0 }, { it.name.orEmpty() }))
    }

    /** Contacts of only one account («Contactos para mostrar»); everyone when [only] is null. */
    fun filter(items: List<Contact>, only: Account?): List<Contact> =
        if (only == null) items else items.filter { c -> c.raws.isEmpty() && only == Account.PHONE || c.raws.any { it.account == only } }
}

/** Joining and separating raw contacts with Android's aggregation (nothing is deleted). */
object Links {
    /** To join several contacts: the first part of the first with every part of the others. */
    fun together(raws: List<List<Long>>): List<Pair<Long, Long>> {
        val first = raws.firstOrNull()?.firstOrNull() ?: return emptyList()
        return raws.flatten().filter { it != first }.distinct().map { first to it }
    }

    /** To separate a contact: every pair of its parts. */
    fun apart(raws: List<Long>): List<Pair<Long, Long>> = raws.distinct().let { r -> r.indices.flatMap { i -> (i + 1 until r.size).map { r[i] to r[it] } } }
}
