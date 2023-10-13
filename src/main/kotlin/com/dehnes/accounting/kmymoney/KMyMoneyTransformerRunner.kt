package com.dehnes.accounting.kmymoney

import com.dehnes.accounting.kmymoney.utils.XmlUtils


object KMyMoneyTransformerRunner {

    fun runTransformers(
        original: XmlUtils.XmlElement,
        transformers: List<KMyMoneyTransformer>
    ): XmlUtils.XmlElement {
        var current = original

        transformers.forEach { t ->
            while (!t.done()) {

                val r = current.children.map { c ->
                    if (c.name == t.targetRootElement()) {

                        val newChilds = mutableListOf<XmlUtils.XmlElement>()
                        c.children.forEach { subChild ->
                            val result = t.transformElement(subChild)
                            result?.let {
                                newChilds.addAll(it)
                            }
                        }

                        c.copy(children = newChilds)
                    } else {
                        c
                    }
                }

                current = current.copy(children = r)
            }
        }

        return current
    }

}