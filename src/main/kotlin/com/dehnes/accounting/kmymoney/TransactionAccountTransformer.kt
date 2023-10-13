package com.dehnes.accounting.kmymoney

import com.dehnes.accounting.kmymoney.utils.XmlUtils

data class TransactionAccountTransformerRule(
    val targetTxId: String,
    val oldAccountId: String,
    val newAccountId: String,
)

class TransactionAccountTransformer(
    private val rules: List<TransactionAccountTransformerRule>
) : KMyMoneyTransformer {

    var done = false

    override fun targetRootElement() = "TRANSACTIONS"

    override fun transformElement(element: XmlUtils.XmlElement): List<XmlUtils.XmlElement>? {

        val txId = element.attributes["id"]
        if (txId in rules.map { it.targetTxId }) {
            val rule = rules.first { it.targetTxId == txId }
            val split = element.children[0].children.first { it.attributes["account"] == rule.oldAccountId }
            val otherSplits = element.children[0].children.filter { it.attributes["account"] != rule.oldAccountId }

            return listOf(
                element.copy(
                    children = listOf(
                        element.children[0].copy(
                            children = otherSplits + split.copy(
                                attributes = split.attributes + ("account" to rule.newAccountId)
                            )
                        )
                    )
                )
            )
        }

        return listOf(element)
    }

    override fun done(): Boolean {
        val p = done
        done = true
        return p
    }

}