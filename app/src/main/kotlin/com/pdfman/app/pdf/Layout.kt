package com.pdfman.app.pdf

import kotlin.math.max
import kotlin.math.min

/** A simple axis-aligned rectangle in PDF points (or pixels), independent of android.graphics. */
data class Box(val left: Float, val top: Float, val width: Float, val height: Float) {
  val right get() = left + width
  val bottom get() = top + height
  val centerX get() = left + width / 2f
  val centerY get() = top + height / 2f
}

enum class ScaleMode(val label: String) {
  /** Whole image visible, letterboxed inside its cell. */
  FIT("Fit"),
  /** Image fills the cell, overflow is cropped at the cell border. */
  CROP("Crop to fill"),
}

enum class PaperGroup(val label: String) {
  ISO_A("ISO A"),
  ISO_B("ISO B"),
  NORTH_AMERICA("North America"),
  JIS("Japan (JIS)"),
  PHOTO("Photo"),
}

private const val MM = 72f / 25.4f
private const val IN = 72f

/** Paper sizes in PDF points (1/72 inch), stored as (short side, long side). */
enum class PageSize(val label: String, val group: PaperGroup, val shortSide: Float, val longSide: Float, val dims: String) {
  A0("A0", PaperGroup.ISO_A, 841 * MM, 1189 * MM, "841 × 1189 mm"),
  A1("A1", PaperGroup.ISO_A, 594 * MM, 841 * MM, "594 × 841 mm"),
  A2("A2", PaperGroup.ISO_A, 420 * MM, 594 * MM, "420 × 594 mm"),
  A3("A3", PaperGroup.ISO_A, 297 * MM, 420 * MM, "297 × 420 mm"),
  A4("A4", PaperGroup.ISO_A, 210 * MM, 297 * MM, "210 × 297 mm"),
  A5("A5", PaperGroup.ISO_A, 148 * MM, 210 * MM, "148 × 210 mm"),
  A6("A6", PaperGroup.ISO_A, 105 * MM, 148 * MM, "105 × 148 mm"),
  A7("A7", PaperGroup.ISO_A, 74 * MM, 105 * MM, "74 × 105 mm"),
  B0("B0", PaperGroup.ISO_B, 1000 * MM, 1414 * MM, "1000 × 1414 mm"),
  B1("B1", PaperGroup.ISO_B, 707 * MM, 1000 * MM, "707 × 1000 mm"),
  B2("B2", PaperGroup.ISO_B, 500 * MM, 707 * MM, "500 × 707 mm"),
  B3("B3", PaperGroup.ISO_B, 353 * MM, 500 * MM, "353 × 500 mm"),
  B4("B4", PaperGroup.ISO_B, 250 * MM, 353 * MM, "250 × 353 mm"),
  B5("B5", PaperGroup.ISO_B, 176 * MM, 250 * MM, "176 × 250 mm"),
  B6("B6", PaperGroup.ISO_B, 125 * MM, 176 * MM, "125 × 176 mm"),
  LETTER("Letter", PaperGroup.NORTH_AMERICA, 8.5f * IN, 11f * IN, "8.5 × 11 in"),
  LEGAL("Legal", PaperGroup.NORTH_AMERICA, 8.5f * IN, 14f * IN, "8.5 × 14 in"),
  TABLOID("Tabloid / Ledger", PaperGroup.NORTH_AMERICA, 11f * IN, 17f * IN, "11 × 17 in"),
  EXECUTIVE("Executive", PaperGroup.NORTH_AMERICA, 7.25f * IN, 10.5f * IN, "7.25 × 10.5 in"),
  STATEMENT("Statement / Half Letter", PaperGroup.NORTH_AMERICA, 5.5f * IN, 8.5f * IN, "5.5 × 8.5 in"),
  FOLIO("Folio", PaperGroup.NORTH_AMERICA, 8.5f * IN, 13f * IN, "8.5 × 13 in"),
  GOV_LETTER("Government Letter", PaperGroup.NORTH_AMERICA, 8f * IN, 10.5f * IN, "8 × 10.5 in"),
  JUNIOR_LEGAL("Junior Legal", PaperGroup.NORTH_AMERICA, 5f * IN, 8f * IN, "5 × 8 in"),
  ARCH_A("ARCH A", PaperGroup.NORTH_AMERICA, 9f * IN, 12f * IN, "9 × 12 in"),
  ARCH_B("ARCH B", PaperGroup.NORTH_AMERICA, 12f * IN, 18f * IN, "12 × 18 in"),
  JIS_B4("JIS B4", PaperGroup.JIS, 257 * MM, 364 * MM, "257 × 364 mm"),
  JIS_B5("JIS B5", PaperGroup.JIS, 182 * MM, 257 * MM, "182 × 257 mm"),
  JIS_B6("JIS B6", PaperGroup.JIS, 128 * MM, 182 * MM, "128 × 182 mm"),
  PHOTO_4X6("4 × 6 in", PaperGroup.PHOTO, 4f * IN, 6f * IN, "10 × 15 cm"),
  PHOTO_5X7("5 × 7 in", PaperGroup.PHOTO, 5f * IN, 7f * IN, "13 × 18 cm"),
  PHOTO_8X10("8 × 10 in", PaperGroup.PHOTO, 8f * IN, 10f * IN, "20 × 25 cm"),
  SQUARE_8("Square 8 in", PaperGroup.PHOTO, 8f * IN, 8f * IN, "8 × 8 in"),
}

/** Converts PDF points to millimetres for display. */
fun Float.ptToMm(): Float = this / MM

enum class Quality(val label: String, val dpi: Int, val jpegQuality: Int) {
  SMALL("Small file", 100, 70),
  BALANCED("Balanced", 150, 80),
  HIGH("High", 300, 90),
}

/**
 * Settings for a single image. [scaleMode] null means "use the layout's setting". [panX] and [panY] pick
 * which part of a cropped image shows: 0 is the left/top edge, 1 the right/bottom edge, 0.5 the center.
 */
data class ImageAdjust(
  val scaleMode: ScaleMode? = null,
  val panX: Float = CENTER,
  val panY: Float = CENTER,
) {
  fun modeOr(default: ScaleMode): ScaleMode = scaleMode ?: default
  val isCentered: Boolean get() = panX == CENTER && panY == CENTER

  companion object {
    const val CENTER = 0.5f
    val DEFAULT = ImageAdjust()
  }
}

data class PdfSettings(
  val rows: Int = 2,
  val cols: Int = 2,
  val scaleMode: ScaleMode = ScaleMode.CROP,
  val pageSize: PageSize = PageSize.A4,
  val landscape: Boolean = false,
  /** Page margin in points. */
  val margin: Float = 18f,
  /** Space between cells in points. */
  val gap: Float = 6f,
  val autoRotate: Boolean = false,
  val borders: Boolean = false,
  val quality: Quality = Quality.BALANCED,
) {
  val pageWidth: Float get() = if (landscape) pageSize.longSide else pageSize.shortSide
  val pageHeight: Float get() = if (landscape) pageSize.shortSide else pageSize.longSide
  val perPage: Int get() = rows * cols

  fun cells(): List<Box> = GridMath.cells(pageWidth, pageHeight, rows, cols, margin, gap)

  fun pageCount(imageCount: Int): Int = GridMath.pageCount(imageCount, perPage)

  companion object {
    const val MAX_GRID = 6
    const val MAX_MARGIN = 72f // 1 inch
    const val MAX_GAP = 36f // 0.5 inch
  }
}

object GridMath {
  /** Cells in row-major order (left→right, top→bottom). */
  fun cells(pageW: Float, pageH: Float, rows: Int, cols: Int, margin: Float, gap: Float): List<Box> {
    require(rows > 0 && cols > 0)
    // On small pages large margins/gaps could leave no room; keep every cell at least 1pt.
    val cellW = ((pageW - 2 * margin - gap * (cols - 1)) / cols).coerceAtLeast(1f)
    val cellH = ((pageH - 2 * margin - gap * (rows - 1)) / rows).coerceAtLeast(1f)
    return buildList {
      for (r in 0 until rows) for (c in 0 until cols) {
        add(Box(margin + c * (cellW + gap), margin + r * (cellH + gap), cellW, cellH))
      }
    }
  }

  fun pageCount(imageCount: Int, perPage: Int): Int =
    if (imageCount <= 0) 0 else (imageCount + perPage - 1) / perPage

  /** Destination rect for an image of [srcW]x[srcH] scaled to fit entirely inside [cell], centered. */
  fun fit(srcW: Float, srcH: Float, cell: Box): Box {
    val scale = min(cell.width / srcW, cell.height / srcH)
    val w = srcW * scale
    val h = srcH * scale
    return Box(cell.centerX - w / 2f, cell.centerY - h / 2f, w, h)
  }

  /**
   * Source rect (in image coordinates) for a crop of an image of [srcW]x[srcH] that exactly covers a cell
   * of [cellW]x[cellH] with no letterboxing. [panX]/[panY] (0..1) choose where the crop sits; 0.5 centers it.
   */
  fun cropSource(
    srcW: Float,
    srcH: Float,
    cellW: Float,
    cellH: Float,
    panX: Float = ImageAdjust.CENTER,
    panY: Float = ImageAdjust.CENTER,
  ): Box {
    val cellRatio = cellW / cellH
    val srcRatio = srcW / srcH
    return if (srcRatio > cellRatio) {
      val w = srcH * cellRatio
      Box((srcW - w) * panX.coerceIn(0f, 1f), 0f, w, srcH)
    } else {
      val h = srcW / cellRatio
      Box(0f, (srcH - h) * panY.coerceIn(0f, 1f), srcW, h)
    }
  }

  /**
   * New (panX, panY) after the user drags a cropped image by ([dx], [dy]) page points, so the picture
   * follows the finger. [rotated] is true when auto-rotate turned the image 90 degrees clockwise in its cell.
   * Pan along an axis with nothing cropped away is left unchanged.
   */
  fun panAfterDrag(
    srcW: Float,
    srcH: Float,
    cell: Box,
    rotated: Boolean,
    panX: Float,
    panY: Float,
    dx: Float,
    dy: Float,
  ): Pair<Float, Float> {
    // The image is drawn in a frame rotated 90 degrees clockwise, so page (dx, dy) is local (dy, -dx).
    val lx = if (rotated) dy else dx
    val ly = if (rotated) -dx else dy
    val lw = if (rotated) cell.height else cell.width
    val lh = if (rotated) cell.width else cell.height
    val crop = cropSource(srcW, srcH, lw, lh)
    val pxPerPt = crop.width / lw
    val slackX = srcW - crop.width
    val slackY = srcH - crop.height
    val nx = if (slackX > 0.5f) (panX - lx * pxPerPt / slackX).coerceIn(0f, 1f) else panX
    val ny = if (slackY > 0.5f) (panY - ly * pxPerPt / slackY).coerceIn(0f, 1f) else panY
    return nx to ny
  }

  /** Index of the cell containing page point ([x], [y]), or -1. */
  fun cellAt(cells: List<Box>, x: Float, y: Float): Int =
    cells.indexOfFirst { x >= it.left && x <= it.right && y >= it.top && y <= it.bottom }

  /** True if rotating the image 90° makes its orientation match the cell's. Square shapes never rotate. */
  fun shouldRotate(srcW: Float, srcH: Float, cellW: Float, cellH: Float): Boolean {
    val srcLandscape = srcW > srcH * 1.05f
    val srcPortrait = srcH > srcW * 1.05f
    val cellLandscape = cellW > cellH * 1.05f
    val cellPortrait = cellH > cellW * 1.05f
    return (srcLandscape && cellPortrait) || (srcPortrait && cellLandscape)
  }

  /**
   * Source pixels per point when an image of [srcW]x[srcH] is drawn into a [cellW]x[cellH] box,
   * whole ([crop] false) or filling it ([crop] true).
   */
  fun sourceDensity(srcW: Float, srcH: Float, cellW: Float, cellH: Float, crop: Boolean): Float =
    if (crop) min(srcW / cellW, srcH / cellH) else max(srcW / cellW, srcH / cellH)

  /** Pixel size to decode images at so they look sharp at [dpi] inside the given cells. */
  fun decodeSize(cells: List<Box>, dpi: Int): Int {
    val cell = cells.firstOrNull() ?: return 1024
    return (max(cell.width, cell.height) * dpi / 72f).toInt().coerceIn(256, 4000)
  }
}
