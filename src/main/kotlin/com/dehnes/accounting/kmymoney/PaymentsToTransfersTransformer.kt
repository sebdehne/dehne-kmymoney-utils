package com.dehnes.accounting.kmymoney

import com.dehnes.accounting.kmymoney.KMyMoneyUtils.parseTransaction
import com.dehnes.accounting.kmymoney.utils.XmlUtils
import java.time.LocalDate

typealias PaymentsToTransfersTransformerFilter = (
    date: LocalDate,
    amountInCents: Long,
    payee: KMyMoneyUtils.Payee?,
    mainAccountPath: String,
    mainAccountId: String,
    contraAccountPath: String,
    contraAccountId: String,
) -> Boolean

class PaymentsToTransfersTransformerFilterWithSide(
    val leftSide: Boolean,
    val filter: PaymentsToTransfersTransformerFilter,
    val contraSideDaysDelta: Long = 5
)

class PaymentsToTransfersTransformer(
    private val payees: List<KMyMoneyUtils.Payee>,
    private val accountIdMapping: AccountIdMapping,
    leftAccountPath: String,
    rightAccountPath: String,
    private val filter: PaymentsToTransfersTransformerFilterWithSide,
) : KMyMoneyTransformer {

    data class MainTransactionRecord(
        val tx: KMyMoneyUtils.KTransaction,
        val mainSplit: KMyMoneyUtils.KTransactionSplit,
        val contraSplit: KMyMoneyUtils.KTransactionSplit,
        var contraTransaction: KMyMoneyUtils.KTransaction? = null,
        var updated: Boolean = false,
        var allRemoved: Boolean = false
    )

    private val leftAccount = accountIdMapping.pathToAccount[leftAccountPath]!!
    private val rightAccount = accountIdMapping.pathToAccount[rightAccountPath]!!
    private val mainAccount = if (filter.leftSide) leftAccount else rightAccount
    private val contraAccount = if (!filter.leftSide) leftAccount else rightAccount

    var state = 0
    // 1 = building main transactions list
    // 2 = finding matching contra transactions
    // 3 = updating the main transactions
    // 4 = deleting the contra transactions

    val mainSideTransactions = mutableListOf<MainTransactionRecord>()

    override fun targetRootElement(): String = "TRANSACTIONS"

    override fun transformElement(element: XmlUtils.XmlElement): List<XmlUtils.XmlElement>? {

        when (state) {
            1 -> {
                val tx = element.parseTransaction()

                if (tx.splits.any { it.accountId == mainAccount.id }) {
                    val mainSplit = tx.splits.single { it.accountId == mainAccount.id }
                    val otherSplits = tx.splits.filter { it.accountId != mainAccount.id }

                    if (otherSplits.size == 1) {
                        val otherSplit = otherSplits.single()

                        if (filter.filter(
                                tx.postDate,
                                mainSplit.amountInCents,
                                mainSplit.payeeId?.let { payees.first { it.id == mainSplit.payeeId } },
                                accountIdMapping.idToPath[mainAccount.id]!!,
                                mainAccount.id,
                                accountIdMapping.idToPath[otherSplit.accountId]!!,
                                otherSplit.accountId
                            )
                        ) {
                            mainSideTransactions.add(
                                MainTransactionRecord(
                                    tx,
                                    mainSplit,
                                    otherSplit
                                )
                            )
                        }
                    }
                }
            }

            2 -> {
                val tx = element.parseTransaction()

                if (tx.splits.any { it.accountId == contraAccount.id }) {
                    val mainSplit = tx.splits.single { it.accountId == contraAccount.id }
                    val otherSplits = tx.splits.filter { it.accountId != contraAccount.id }

                    if (otherSplits.size == 1) {
                        val dateRange =
                            (tx.postDate.minusDays(filter.contraSideDaysDelta))..(tx.postDate.plusDays(filter.contraSideDaysDelta))

                        val candiates = mainSideTransactions.filter { cTx ->
                            cTx.contraSplit.amountInCents == mainSplit.amountInCents && cTx.tx.postDate in dateRange
                        }

                        if (candiates.size != 0) {
                            check(candiates.size == 1)
                            candiates.single().contraTransaction = tx
                        }
                    }
                }
            }

            3 -> {
                val match = mainSideTransactions.firstOrNull {
                    it.tx.id == element.attributes["id"]
                }
                if (match != null) {
                    val mainSplit = element.children[0].children.first {
                        it.attributes["id"] == match.mainSplit.id
                    }
                    val contraSplit = element.children[0].children.first {
                        it.attributes["id"] == match.contraSplit.id
                    }

                    match.updated = true
                    return listOf(
                        element.copy(
                            children = listOf(
                                element.children[0].copy(
                                    children = listOf(
                                        mainSplit,
                                        contraSplit.copy(
                                            attributes = contraSplit.attributes + ("account" to contraAccount.id)
                                        )
                                    )
                                )
                            )
                        )
                    )
                }
            }

            4 -> {
                val match = mainSideTransactions.firstOrNull {
                    it.contraTransaction!!.id == element.attributes["id"]
                }

                if (match != null) {
                    match.allRemoved = true
                    return emptyList()
                }
            }
        }

        return listOf(element)
    }

    override fun done(): Boolean {
        state = when (state) {
            0 -> 1
            1 -> 2
            2 -> {
                check(mainSideTransactions.all { it.contraTransaction != null })
                3
            }

            3 -> {
                check(mainSideTransactions.all { it.updated })
                4
            }

            4 -> {
                check(mainSideTransactions.all { it.allRemoved })
                return true
            }

            else -> error("")
        }
        return false
    }

}

