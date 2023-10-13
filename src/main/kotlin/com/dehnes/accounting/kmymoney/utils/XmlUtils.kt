package com.dehnes.accounting.kmymoney.utils

import org.w3c.dom.Attr
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.StringReader
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult


object XmlUtils {

    fun parseXMl(xmlStr: String): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }
        val builder = factory.newDocumentBuilder()
        val source = InputSource(StringReader(xmlStr))
        return builder.parse(source)
    }

    fun children(node: Node): List<Node> {
        val l = mutableListOf<Node>()
        val childNodes = node.childNodes
        if (childNodes.length > 0) {
            (0..childNodes.length).forEach {
                l.add(childNodes.item(it) ?: return@forEach)
            }
        }
        return l.filter { it.nodeType == Node.ELEMENT_NODE }
    }

    fun parseStructure(node: Node): XmlElement {
        val c = children(node).map { parseStructure(it) }

        val attributes = node.attributes
        val attrs = if (attributes.length > 0) {
            (0..attributes.length).mapNotNull {
                val item = attributes.item(it) as Attr? ?: return@mapNotNull null
                item.name to item.value
            }.toMap()
        } else {
            emptyMap()
        }

        return XmlElement(
            node.localName,
            attrs,
            node.textContent.trim().ifBlank { null },
            c
        )
    }

    fun toDocument(rootElement: XmlElement): Document {
        val newInstance = DocumentBuilderFactory.newInstance()
        val newDocumentBuilder = newInstance.newDocumentBuilder()
        val newDocument = newDocumentBuilder.newDocument()


        val rootEl = newDocument.createElement(rootElement.name)
        newDocument.appendChild(rootEl)

        fun addChildren(parent: Element, xmlElement: XmlElement) {
            val element = newDocument.createElement(xmlElement.name)

            xmlElement.attributes.forEach { (key, value) ->
                element.setAttribute(key, value)
            }

            parent.appendChild(element)

            xmlElement.children.forEach {
                addChildren(element, it)
            }
        }

        rootElement.children.forEach { c ->
            addChildren(rootEl, c)
        }

        return newDocument
    }

    fun parseKMyMoneyFile(xmlString: String): XmlElement {
        val document = parseXMl(xmlString)
        return XmlElement(
            document.documentElement.nodeName,
            emptyMap(),
            null,
            children(document.documentElement).map { parseStructure(it) }
        )
    }

    fun marshallToXml(doc: Document): ByteArray {
        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "1")


        val byteArrayOutputStream = ByteArrayOutputStream()
        transformer.transform(DOMSource(doc), StreamResult(byteArrayOutputStream))

        return byteArrayOutputStream.toByteArray()
    }

    fun toKMyMoneyFile(rawXml: ByteArray): ByteArray {
        val header = """
            <?xml version="1.0" encoding="utf-8"?>
            <!DOCTYPE KMYMONEY-FILE>
            
        """.trimIndent().toByteArray(StandardCharsets.UTF_8)
        return header.plus(rawXml)
    }

    data class XmlElement(
        val name: String,
        val attributes: Map<String, String>,
        val text: String?,
        val children: List<XmlElement>
    )

}