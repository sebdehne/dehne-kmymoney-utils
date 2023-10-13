package com.dehnes.accounting.kmymoney

import com.dehnes.accounting.kmymoney.utils.XmlUtils
import java.time.LocalDate

object KMyMoneyUtils {

    fun XmlUtils.XmlElement.parseAllBanks() = children.first { it.name == "INSTITUTIONS" }.children
        .map { institution ->

            val allAccountIds =
                institution.children.firstOrNull { it.name == "ACCOUNTIDS" }?.children?.map { it.attributes["id"]!! }
                    ?: emptyList()

            KBank(
                institution.attributes["id"]!!, institution.attributes["name"]!!, allAccountIds
            )
        }.filter { it.accountIds.isNotEmpty() }

    fun XmlUtils.XmlElement.parseTransaction(): KTransaction {
        val splits = children.first { it.name == "SPLITS" }.children.map {
            check(it.attributes["action"] == "")
            check(it.attributes["price"] == "1/1")
            check(it.attributes["shares"] == it.attributes["value"])
            KTransactionSplit(
                it.attributes["id"]!!,
                it.attributes["number"]?.ifBlank { null }?.toInt(),
                it.attributes["payee"]?.ifBlank { null },
                it.attributes["account"]!!,
                it.attributes["memo"]?.ifBlank { null },
                valueToAmountInCents(it.attributes["value"]!!)
            )
        }

        check(attributes["memo"] == "")

        return KTransaction(
            attributes["id"]!!,
            attributes["postdate"]!!.let { LocalDate.parse(it) },
            attributes["entrydate"]!!.let { LocalDate.parse(it) },
            splits
        )
    }

    fun XmlUtils.XmlElement.parseTransactions(): List<KTransaction> {
        val transactions = children.first { it.name == "TRANSACTIONS" }.children.map {
            it.parseTransaction()
        }
        return transactions
    }

    fun XmlUtils.XmlElement.getAllPayees(): List<Payee> =
        children.first { it.name == "PAYEES" }.children.map {
            Payee(it.attributes["id"]!!, it.attributes["name"]!!)
        }

    fun XmlUtils.XmlElement.getAllAccounts(): List<Account> =
        children.first { it.name == "ACCOUNTS" }.children.map { account ->
            val closed =
                account.children.firstOrNull { it.name == "KEYVALUEPAIRS" }?.children?.firstOrNull { it.attributes["key"] == "mm-closed" && it.attributes["value"] == "yes" } != null
            Account(account.attributes["id"]!!,
                account.attributes["parentaccount"]?.ifBlank { null },
                account.attributes["type"]!!,
                account.attributes["opened"]?.ifBlank { null }?.let { LocalDate.parse(it) },
                account.attributes["name"]!!,
                account.attributes["number"]?.ifBlank { null },
                account.attributes["description"]?.ifBlank { null },
                account.children.firstOrNull { it.name == "SUBACCOUNTS" }?.children?.map { it.attributes["id"]!! }
                    ?: emptyList(),
                closed)
        }

    private fun valueToAmountInCents(value: String): Long {
        val (numerator, denominator) = value.split("/").map { it.trim().toLong() }
        val multiplyer = 100 / denominator
        return numerator * multiplyer
    }

    data class Account(
        val id: String,
        val parentId: String?,
        val type: String,
        val opened: LocalDate?,
        val name: String,
        val number: String?,
        val description: String?,
        val subAccounts: List<String>,
        val closed: Boolean,
    )

    data class Payee(
        val id: String,
        val name: String
    )

    data class KTransaction(
        val id: String,
        val postDate: LocalDate,
        val entryDate: LocalDate,
        val splits: List<KTransactionSplit>,
    )

    data class KTransactionSplit(
        val id: String,
        val number: Int?,
        val payeeId: String?,
        val accountId: String,
        val memo: String?,
        val amountInCents: Long,
    )

    data class KBank(
        val id: String,
        val name: String,
        val accountIds: List<String>,
    )

}