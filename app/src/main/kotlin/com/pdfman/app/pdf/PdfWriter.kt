package com.pdfman.app.pdf

import java.io.OutputStream
import java.util.Locale

/**
 * Minimal streaming PDF 1.4 writer for pages of JPEG images and stroked rectangles. Unlike
 * android.graphics.pdf.PdfDocument, which stores bitmaps losslessly, images are embedded as the JPEG bytes
 * given (DCTDecode), and each object is written as soon as it is complete, so memory use stays small.
 *
 * Coordinates are PDF points with the origin at the page's top-left, like [Box].
 */
class PdfWriter(output: OutputStream) {
  private val out = CountingStream(output)
  private val offsets = mutableMapOf<Int, Long>()
  private val pageIds = mutableListOf<Int>()
  private var nextId = FIRST_FREE_ID

  private var pageHeight = 0f
  private var pageWidth = 0f
  private var content: StringBuilder? = null
  private val pageImages = mutableListOf<Int>()

  init {
    out.write("%PDF-1.4\n%âãÏÓ\n".toByteArray(Charsets.ISO_8859_1))
  }

  fun beginPage(width: Float, height: Float) {
    check(content == null) { "Previous page not finished" }
    pageWidth = width
    pageHeight = height
    content = StringBuilder()
    pageImages.clear()
  }

  /** Draws a baseline JPEG of [pixelWidth]x[pixelHeight] stretched over [box]. */
  fun drawJpeg(jpeg: ByteArray, pixelWidth: Int, pixelHeight: Int, box: Box) {
    val c = checkNotNull(content) { "No page started" }
    val id = beginObject()
    ascii(
      "<< /Type /XObject /Subtype /Image /Width $pixelWidth /Height $pixelHeight " +
        "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpeg.size} >>\nstream\n",
    )
    out.write(jpeg)
    ascii("\nendstream\nendobj\n")
    val name = "Im${pageImages.size}"
    pageImages += id
    c.append("q ${n(box.width)} 0 0 ${n(box.height)} ${n(box.left)} ${n(pageHeight - box.bottom)} cm /$name Do Q\n")
  }

  /** Strokes the outline of [box] in a gray level (0 black .. 1 white). */
  fun strokeRect(box: Box, gray: Float, lineWidth: Float) {
    val c = checkNotNull(content) { "No page started" }
    c.append("q ${n(gray)} G ${n(lineWidth)} w ")
    c.append("${n(box.left)} ${n(pageHeight - box.bottom)} ${n(box.width)} ${n(box.height)} re S Q\n")
  }

  fun endPage() {
    val c = checkNotNull(content) { "No page started" }
    val bytes = c.toString().toByteArray(Charsets.US_ASCII)
    val contentId = beginObject()
    ascii("<< /Length ${bytes.size} >>\nstream\n")
    out.write(bytes)
    ascii("\nendstream\nendobj\n")

    val xobjects = pageImages.withIndex().joinToString(" ") { (i, id) -> "/Im$i $id 0 R" }
    val pageId = beginObject()
    ascii(
      "<< /Type /Page /Parent $PAGES_ID 0 R /MediaBox [0 0 ${n(pageWidth)} ${n(pageHeight)}] " +
        "/Resources << /XObject << $xobjects >> >> /Contents $contentId 0 R >>\nendobj\n",
    )
    pageIds += pageId
    content = null
  }

  /** Writes the page tree, catalog and cross-reference table. Does not close the underlying stream. */
  fun finish() {
    check(content == null) { "Page not finished" }
    beginObject(PAGES_ID)
    ascii("<< /Type /Pages /Kids [${pageIds.joinToString(" ") { "$it 0 R" }}] /Count ${pageIds.size} >>\nendobj\n")
    beginObject(CATALOG_ID)
    ascii("<< /Type /Catalog /Pages $PAGES_ID 0 R >>\nendobj\n")

    val xref = out.count
    val size = nextId
    val sb = StringBuilder("xref\n0 $size\n0000000000 65535 f \n")
    for (id in 1 until size) {
      val offset = offsets[id]
      if (offset != null) sb.append(String.format(Locale.US, "%010d 00000 n \n", offset))
      else sb.append("0000000000 65535 f \n")
    }
    sb.append("trailer\n<< /Size $size /Root $CATALOG_ID 0 R >>\nstartxref\n$xref\n%%EOF\n")
    ascii(sb.toString())
    out.flush()
  }

  private fun beginObject(id: Int = nextId++): Int {
    offsets[id] = out.count
    ascii("$id 0 obj\n")
    return id
  }

  private fun ascii(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))

  /** Formats a number compactly without exponent notation. */
  private fun n(v: Float): String {
    val s = String.format(Locale.US, "%.3f", v).trimEnd('0').trimEnd('.')
    return if (s == "-0") "0" else s
  }

  private class CountingStream(private val target: OutputStream) : OutputStream() {
    var count = 0L
      private set

    override fun write(b: Int) {
      target.write(b)
      count++
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
      target.write(b, off, len)
      count += len
    }

    override fun flush() = target.flush()
  }

  private companion object {
    const val CATALOG_ID = 1
    const val PAGES_ID = 2
    const val FIRST_FREE_ID = 3
  }
}
