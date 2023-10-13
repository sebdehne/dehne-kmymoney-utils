package com.dehnes.accounting.kmymoney

import com.dehnes.accounting.kmymoney.utils.XmlUtils


interface KMyMoneyTransformer {
    fun targetRootElement(): String

    fun transformElement(element: XmlUtils.XmlElement): List<XmlUtils.XmlElement>?

    fun done(): Boolean
}

