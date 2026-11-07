package com.pdfman.app.pdf

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min
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

  /**
   * Draws one page in PDF point coordinates. [images] are row-major and may be shorter than the cell count;
   * [adjusts] holds each image's own fit/crop and crop position (missing entries use the defaults).
   */
  fun draw(
    canvas: Canvas,
    settings: PdfSettings,
    cells: List<Box>,
    images: List<Bitmap?>,
    adjusts: List<ImageAdjust>,
    preview: Boolean,
  ) {
    canvas.drawColor(Color.WHITE)
    cells.forEachIndexed { i, cell ->
      val bmp = images.getOrNull(i)
      if (bmp != null) {
        drawInCell(canvas, bmp, cell, settings, adjusts.getOrNull(i) ?: ImageAdjust.DEFAULT)
      } else if (preview && i < images.size) {
        canvas.drawRect(cell.toRectF(), placeholderPaint) // still loading
      }
      if (settings.borders && i < images.size) canvas.drawRect(cell.toRectF(), borderPaint)
    }
  }

  /** True when auto-rotate turns [bmp] 90 degrees to fit [cell]. */
  fun isRotated(bmp: Bitmap, cell: Box, s: PdfSettings): Boolean =
    s.autoRotate && GridMath.shouldRotate(bmp.width.toFloat(), bmp.height.toFloat(), cell.width, cell.height)

  fun drawInCell(canvas: Canvas, bmp: Bitmap, cell: Box, s: PdfSettings, adjust: ImageAdjust) {
    val bw = bmp.width.toFloat()
    val bh = bmp.height.toFloat()
    val rotate = isRotated(bmp, cell, s)
    // Work in a local box centred on the cell; if rotated, its sides are swapped.
    val lw = if (rotate) cell.height else cell.width
    val lh = if (rotate) cell.width else cell.height
    val local = Box(-lw / 2f, -lh / 2f, lw, lh)

    canvas.withClip(cell.toRectF()) {
      translate(cell.centerX, cell.centerY)
      if (rotate) rotate(90f)
      when (adjust.modeOr(s.scaleMode)) {
        ScaleMode.FIT -> drawBitmap(bmp, null, GridMath.fit(bw, bh, local).toRectF(), bitmapPaint)
        ScaleMode.CROP -> {
          val src = GridMath.cropSource(bw, bh, lw, lh, adjust.panX, adjust.panY)
          drawBitmap(bmp, src.toRect(), local.toRectF(), bitmapPaint)
        }
      }
    }
  }

  /** Renders a small bitmap preview of a page using already-decoded thumbnails. */
  fun renderPreview(settings: PdfSettings, images: List<Bitmap?>, adjusts: List<ImageAdjust>, widthPx: Int): Bitmap {
    val scale = widthPx / settings.pageWidth
    val bmp = createBitmap(widthPx, (settings.pageHeight * scale).toInt())
    val canvas = Canvas(bmp)
    canvas.scale(scale, scale)
    draw(canvas, settings, settings.cells(), images, adjusts, preview = true)
    return bmp
  }

  private fun Box.toRectF() = RectF(left, top, right, bottom)
  private fun Box.toRect() = Rect(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())
}

object PdfComposer {
  private const val BORDER_GRAY = 160f / 255f
  private const val BORDER_WIDTH = 0.75f

  /**
   * Writes the PDF, decoding one page worth of images at a time to bound memory use. Each image is rendered
   * at the cell's size and the quality's dpi (never above the image's own resolution) and stored as JPEG.
   */
  fun write(
    resolver: ContentResolver,
    uris: List<Uri>,
    adjusts: List<ImageAdjust>,
    settings: PdfSettings,
    out: OutputStream,
    onProgress: (page: Int, total: Int) -> Unit = { _, _ -> },
  ) {
    val cells = settings.cells()
    val maxDim = GridMath.decodeSize(cells, settings.quality.dpi)
    val pages = uris.chunked(settings.perPage)
    val pageAdjusts = adjusts.chunked(settings.perPage)
    val pdf = PdfWriter(out)
    val jpeg = ByteArrayOutputStream()
    pages.forEachIndexed { index, chunk ->
      onProgress(index + 1, pages.size)
      pdf.beginPage(settings.pageWidth, settings.pageHeight)
      val adjustsHere = pageAdjusts.getOrElse(index) { emptyList() }
      chunk.forEachIndexed { i, uri ->
        val cell = cells[i]
        val src = ImageLoader.load(resolver, uri, maxDim)
        if (src != null) {
          val rendered = renderCell(src, cell, settings, adjustsHere.getOrNull(i) ?: ImageAdjust.DEFAULT)
          src.recycle()
          jpeg.reset()
          rendered.compress(Bitmap.CompressFormat.JPEG, settings.quality.jpegQuality, jpeg)
          pdf.drawJpeg(jpeg.toByteArray(), rendered.width, rendered.height, cell)
          rendered.recycle()
        }
        if (settings.borders) pdf.strokeRect(cell, BORDER_GRAY, BORDER_WIDTH)
      }
      pdf.endPage()
    }
    pdf.finish()
  }

  /** Draws [src] as it appears in [cell] into a white bitmap covering the cell. */
  private fun renderCell(src: Bitmap, cell: Box, settings: PdfSettings, adjust: ImageAdjust): Bitmap {
    val rotated = PageDrawer.isRotated(src, cell, settings)
    val lw = if (rotated) cell.height else cell.width
    val lh = if (rotated) cell.width else cell.height
    val crop = adjust.modeOr(settings.scaleMode) == ScaleMode.CROP
    val density = GridMath.sourceDensity(src.width.toFloat(), src.height.toFloat(), lw, lh, crop)
    val pxPerPt = min(settings.quality.dpi / 72f, density)
    val w = (cell.width * pxPerPt).roundToInt().coerceAtLeast(1)
    val h = (cell.height * pxPerPt).roundToInt().coerceAtLeast(1)
    val bmp = createBitmap(w, h)
    Canvas(bmp).apply {
      drawColor(Color.WHITE)
      scale(w / cell.width, h / cell.height)
      translate(-cell.left, -cell.top)
      PageDrawer.drawInCell(this, src, cell, settings, adjust)
    }
    return bmp
  }
}
