package com.dehnes.accounting.kmymoney

class AccountIdMapping private constructor(
    val idToPath: Map<String, String>,
    val pathToAccount: Map<String, KMyMoneyUtils.Account>,
) {

    fun getById(id: String) = pathToAccount[idToPath[id]]
    fun getByPath(path: String) = pathToAccount[path]

    fun idToPath(id: String) = idToPath[id]

    companion object {
        fun create(accounts: List<KMyMoneyUtils.Account>): AccountIdMapping {
            fun getPath(a: KMyMoneyUtils.Account): String {
                val parentPath = a.parentId?.let { pId ->
                    getPath(
                        accounts.first { it.id == pId }
                    )
                } ?: ""

                return parentPath + "/" + a.name
            }

            return AccountIdMapping(
                accounts.associate { it.id to getPath(it) },
                accounts.associateBy { getPath(it) }
            )
        }
    }


}