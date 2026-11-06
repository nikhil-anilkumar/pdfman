package com.pdfman.app.pdf

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object ImageLoader {
  /** Decodes [uri] (EXIF orientation applied) so its longest side is roughly <= 2x [maxDim]. */
  fun load(resolver: ContentResolver, uri: Uri, maxDim: Int): Bitmap? = try {
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
      decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
      val largest = max(info.size.width, info.size.height)
      if (largest > maxDim) decoder.setTargetSampleSize(largest / maxDim)
    }
  } catch (e: Exception) {
    null
  }
}

object PageDrawer {
  private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
  private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    color = Color.rgb(160, 160, 160)
    strokeWidth = 0.75f
  }
  private val placeholderPaint = Paint().apply { color = Color.rgb(238, 238, 238) }

  /** Draws one page in PDF point coordinates. [images] are row-major and may be shorter than the cell count. */
  fun draw(canvas: Canvas, settings: PdfSettings, cells: List<Box>, images: List<Bitmap?>, preview: Boolean) {
    canvas.drawColor(Color.WHITE)
    cells.forEachIndexed { i, cell ->
      val bmp = images.getOrNull(i)
      if (bmp != null) {
        drawInCell(canvas, bmp, cell, settings)
      } else if (preview && i < images.size) {
        canvas.drawRect(cell.toRectF(), placeholderPaint) // still loading
      }
      if (settings.borders && i < images.size) canvas.drawRect(cell.toRectF(), borderPaint)
    }
  }

  private fun drawInCell(canvas: Canvas, bmp: Bitmap, cell: Box, s: PdfSettings) {
    val bw = bmp.width.toFloat()
    val bh = bmp.height.toFloat()
    val rotate = s.autoRotate && GridMath.shouldRotate(bw, bh, cell.width, cell.height)
    // Work in a local box centred on the cell; if rotated, its sides are swapped.
    val lw = if (rotate) cell.height else cell.width
    val lh = if (rotate) cell.width else cell.height
    val local = Box(-lw / 2f, -lh / 2f, lw, lh)

    canvas.withClip(cell.toRectF()) {
      translate(cell.centerX, cell.centerY)
      if (rotate) rotate(90f)
      when (s.scaleMode) {
        ScaleMode.FIT -> drawBitmap(bmp, null, GridMath.fit(bw, bh, local).toRectF(), bitmapPaint)
        ScaleMode.CROP -> {
          val src = GridMath.cropSource(bw, bh, lw, lh)
          drawBitmap(bmp, src.toRect(), local.toRectF(), bitmapPaint)
        }
      }
    }
  }

  /** Renders a small bitmap preview of a page using already-decoded thumbnails. */
  fun renderPreview(settings: PdfSettings, images: List<Bitmap?>, widthPx: Int): Bitmap {
    val scale = widthPx / settings.pageWidth
    val bmp = createBitmap(widthPx, (settings.pageHeight * scale).toInt())
    val canvas = Canvas(bmp)
    canvas.scale(scale, scale)
    draw(canvas, settings, settings.cells(), images, preview = true)
    return bmp
  }

  private fun Box.toRectF() = RectF(left, top, right, bottom)
  private fun Box.toRect() = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
}

object PdfComposer {
  /** Writes the PDF, decoding one page worth of full-quality images at a time to bound memory use. */
  fun write(
    resolver: ContentResolver,
    uris: List<Uri>,
    settings: PdfSettings,
    out: OutputStream,
    onProgress: (page: Int, total: Int) -> Unit = { _, _ -> },
  ) {
    val cells = settings.cells()
    val maxDim = GridMath.decodeSize(cells, settings.quality.dpi)
    val pages = uris.chunked(settings.perPage)
    val doc = PdfDocument()
    try {
      pages.forEachIndexed { index, chunk ->
        onProgress(index + 1, pages.size)
        val info = PdfDocument.PageInfo.Builder(
          settings.pageWidth.roundToInt(), settings.pageHeight.roundToInt(), index + 1,
        ).create()
        val page = doc.startPage(info)
        val bitmaps = chunk.map { ImageLoader.load(resolver, it, maxDim) }
        PageDrawer.draw(page.canvas, settings, cells, bitmaps, preview = false)
        doc.finishPage(page)
        bitmaps.forEach { it?.recycle() }
      }
      doc.writeTo(out)
    } finally {
      doc.close()
    }
  }
}
