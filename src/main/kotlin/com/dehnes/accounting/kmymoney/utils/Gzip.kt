package com.dehnes.accounting.kmymoney.utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object Gzip {

    fun compress(byteArray: ByteArray): ByteArray {
        val result = ByteArrayOutputStream()
        val out = GZIPOutputStream(result)
        val inStr = ByteArrayInputStream(byteArray)
        inStr.copyTo(out)
        out.close()
        return result.toByteArray()
    }

    fun uncompress(byteArray: ByteArray): ByteArray {
        val result = ByteArrayOutputStream()
        val inStr = GZIPInputStream(ByteArrayInputStream(byteArray))
        inStr.copyTo(result)
        return result.toByteArray()
    }

}