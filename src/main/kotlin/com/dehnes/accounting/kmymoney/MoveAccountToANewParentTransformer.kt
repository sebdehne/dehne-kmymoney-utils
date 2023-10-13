package com.dehnes.accounting.kmymoney

import com.dehnes.accounting.kmymoney.utils.XmlUtils


class MoveAccountToANewParentTransformer(
    accountMap: AccountIdMapping,
    targetPath: String,
    newParentPath: String
) : KMyMoneyTransformer {

    private val targetAccount = accountMap.pathToAccount[targetPath]!!
    private val newParentAccount = accountMap.pathToAccount[newParentPath]!!
    private var typeOfSubaccountForNewParent: String? = null
    private var type: String? = null
    private var oldParentUpdated = false
    private var newParentUpdated = false
    private var targetUpdated = false

    override fun targetRootElement() = "ACCOUNTS"

    override fun transformElement(element: XmlUtils.XmlElement): List<XmlUtils.XmlElement> {

        if (typeOfSubaccountForNewParent == null) {
            if (type == null && element.attributes["id"] in newParentAccount.subAccounts) {
                type = element.attributes["type"]
            }
            return listOf(element)
        }

        if (!targetUpdated && element.attributes["id"] == targetAccount.id) {
            targetUpdated = true
            return listOf(
                element.copy(
                    attributes = element.attributes
                            + ("parentaccount" to newParentAccount.id)
                            + ("type" to typeOfSubaccountForNewParent!!)
                )
            )
        }

        if (!newParentUpdated && element.attributes["id"] == newParentAccount.id) {
            newParentUpdated = true
            return listOf(
                element.copy(
                    children = element.children.map {
                        if (it.name == "SUBACCOUNTS")
                            it.copy(
                                children = it.children + XmlUtils.XmlElement(
                                    "SUBACCOUNT",
                                    mapOf("id" to targetAccount.id),
                                    null,
                                    emptyList()
                                )
                            )
                        else it
                    }
                )
            )
        }

        if (!oldParentUpdated && element.attributes["id"] == targetAccount.parentId) {
            oldParentUpdated = true
            return listOf(
                element.copy(
                    children = element.children.map { subAccountsEl ->
                        if (subAccountsEl.name == "SUBACCOUNTS")
                            subAccountsEl.copy(
                                children = subAccountsEl.children.filterNot {
                                    it.attributes["id"] == targetAccount.id
                                }
                            )
                        else subAccountsEl
                    }
                )
            )
        }

        return listOf(element)
    }

    override fun done(): Boolean {
        if (type == null) return false

        if (typeOfSubaccountForNewParent == null) {
            typeOfSubaccountForNewParent = type!!
            return false
        }

        return targetUpdated && oldParentUpdated && newParentUpdated
    }
}