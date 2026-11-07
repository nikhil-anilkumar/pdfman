package com.pdfman.app.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class PdfWriterTest {
  private fun writeSample(): String {
    val buf = ByteArrayOutputStream()
    PdfWriter(buf).apply {
      repeat(2) {
        beginPage(200f, 100f)
        drawJpeg(byteArrayOf(1, 2, 3), 4, 2, Box(10f, 20f, 40f, 30f))
        strokeRect(Box(10f, 20f, 40f, 30f), 0.5f, 0.75f)
        endPage()
      }
      finish()
    }
    return buf.toByteArray().toString(Charsets.ISO_8859_1)
  }

  @Test fun xrefOffsetsPointAtTheirObjects() {
    val pdf = writeSample()
    val startXref = Regex("startxref\n(\\d+)\n%%EOF\n$").find(pdf)!!.groupValues[1].toInt()
    assertTrue(pdf.startsWith("xref\n", startXref))
    val lines = pdf.substring(startXref).lines()
    val count = lines[1].split(" ")[1].toInt()
    for (id in 1 until count) {
      val offset = lines[2 + id].substring(0, 10).toInt()
      assertTrue("object $id", pdf.startsWith("$id 0 obj\n", offset))
    }
  }

  @Test fun pagesAndImagesAreReferenced() {
    val pdf = writeSample()
    assertTrue(pdf.contains("/Count 2"))
    assertEquals(2, Regex("/Filter /DCTDecode /Length 3 >>").findAll(pdf).count())
    // Box top 20 on a 100pt page with height 30 sits at y = 100 - 50 = 50 in PDF space.
    assertTrue(pdf.contains("q 40 0 0 30 10 50 cm /Im0 Do Q"))
    assertTrue(pdf.contains("10 50 40 30 re S"))
  }
}
