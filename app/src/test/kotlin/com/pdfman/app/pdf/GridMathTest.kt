package com.pdfman.app.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GridMathTest {
  private val eps = 0.01f

  @Test fun cellsTileThePageInRowMajorOrder() {
    val cells = GridMath.cells(600f, 800f, rows = 2, cols = 3, margin = 10f, gap = 5f)
    assertEquals(6, cells.size)
    val first = cells.first()
    assertEquals(10f, first.left, eps)
    assertEquals(10f, first.top, eps)
    assertEquals((600f - 20f - 10f) / 3f, first.width, eps)
    assertEquals((800f - 20f - 5f) / 2f, first.height, eps)
    // second cell is to the right, fourth cell starts the next row
    assertEquals(first.right + 5f, cells[1].left, eps)
    assertEquals(first.bottom + 5f, cells[3].top, eps)
    assertEquals(590f, cells.last().right, eps)
    assertEquals(790f, cells.last().bottom, eps)
  }

  @Test fun pageCountRoundsUp() {
    assertEquals(0, GridMath.pageCount(0, 4))
    assertEquals(1, GridMath.pageCount(4, 4))
    assertEquals(2, GridMath.pageCount(5, 4))
    assertEquals(7, PdfSettings(rows = 1, cols = 1).pageCount(7))
  }

  @Test fun fitKeepsAspectAndCentres() {
    val cell = Box(0f, 0f, 100f, 100f)
    val dst = GridMath.fit(400f, 200f, cell) // wide image
    assertEquals(100f, dst.width, eps)
    assertEquals(50f, dst.height, eps)
    assertEquals(25f, dst.top, eps)
    assertEquals(50f, dst.centerX, eps)
  }

  @Test fun cropCoversCellWithCentredSource() {
    // wide image into square cell: crop the sides
    val src = GridMath.cropSource(400f, 200f, 100f, 100f)
    assertEquals(200f, src.width, eps)
    assertEquals(200f, src.height, eps)
    assertEquals(100f, src.left, eps)
    // tall image into wide cell: crop top/bottom
    val tall = GridMath.cropSource(100f, 400f, 200f, 100f)
    assertEquals(100f, tall.width, eps)
    assertEquals(50f, tall.height, eps)
    assertEquals(175f, tall.top, eps)
  }

  @Test fun rotateOnlyWhenOrientationsDisagree() {
    assertTrue(GridMath.shouldRotate(400f, 300f, 100f, 200f))
    assertTrue(GridMath.shouldRotate(300f, 400f, 200f, 100f))
    assertFalse(GridMath.shouldRotate(400f, 300f, 200f, 100f))
    assertFalse(GridMath.shouldRotate(100f, 100f, 100f, 200f))
  }

  @Test fun paperSizesAreSaneAndKnownSizesMatch() {
    PageSize.entries.forEach { assertTrue(it.name, it.shortSide > 0 && it.shortSide <= it.longSide) }
    assertEquals(595f, PageSize.A4.shortSide, 0.5f)
    assertEquals(842f, PageSize.A4.longSide, 0.5f)
    assertEquals(612f, PageSize.LETTER.shortSide, eps)
    assertEquals(1224f, PageSize.TABLOID.longSide, eps)
    PaperGroup.entries.forEach { g -> assertTrue(g.name, PageSize.entries.any { it.group == g }) }
  }

  @Test fun hugeMarginsOnTinyPagesStillGivePositiveCells() {
    val s = PdfSettings(pageSize = PageSize.A7, rows = 6, cols = 6, margin = PdfSettings.MAX_MARGIN, gap = PdfSettings.MAX_GAP)
    s.cells().forEach { assertTrue(it.width >= 1f && it.height >= 1f) }
  }

  @Test fun landscapeSwapsPageDimensions() {
    val s = PdfSettings(pageSize = PageSize.A4, landscape = true)
    assertEquals(842f, s.pageWidth, 0.5f)
    assertEquals(595f, s.pageHeight, 0.5f)
  }

  @Test fun cropPanMovesTheWindowAcrossTheSlack() {
    // 400x200 image in a square cell: a 200x200 window that can slide 200px sideways.
    assertEquals(0f, GridMath.cropSource(400f, 200f, 100f, 100f, panX = 0f).left, eps)
    assertEquals(200f, GridMath.cropSource(400f, 200f, 100f, 100f, panX = 1f).left, eps)
    assertEquals(50f, GridMath.cropSource(400f, 200f, 100f, 100f, panX = 0.25f).left, eps)
    // Out-of-range pans are clamped, and the axis with no slack ignores pan.
    assertEquals(200f, GridMath.cropSource(400f, 200f, 100f, 100f, panX = 3f).left, eps)
    assertEquals(0f, GridMath.cropSource(400f, 200f, 100f, 100f, panY = 1f).top, eps)
  }

  @Test fun draggingRightRevealsTheLeftOfTheImage() {
    val cell = Box(0f, 0f, 100f, 100f)
    // 2 image px per point and 200px of slack: dragging 25pt right moves the window 50px left.
    val (x, y) = GridMath.panAfterDrag(400f, 200f, cell, rotated = false, panX = 0.5f, panY = 0.5f, dx = 25f, dy = 40f)
    assertEquals(0.25f, x, eps)
    assertEquals(0.5f, y, eps) // nothing cropped vertically
    val (clamped, _) = GridMath.panAfterDrag(400f, 200f, cell, rotated = false, panX = 0.1f, panY = 0.5f, dx = 500f, dy = 0f)
    assertEquals(0f, clamped, eps)
  }

  @Test fun dragOnARotatedImageFollowsTheFinger() {
    // Turned 90 degrees clockwise, the image's x axis points down the page.
    val cell = Box(0f, 0f, 100f, 100f)
    val (down, _) = GridMath.panAfterDrag(400f, 200f, cell, rotated = true, panX = 0.5f, panY = 0.5f, dx = 0f, dy = 25f)
    assertEquals(0.25f, down, eps)
    val (sideways, _) = GridMath.panAfterDrag(400f, 200f, cell, rotated = true, panX = 0.5f, panY = 0.5f, dx = 25f, dy = 0f)
    assertEquals(0.5f, sideways, eps)
  }

  @Test fun cellAtFindsTheTappedCell() {
    val cells = GridMath.cells(200f, 200f, rows = 2, cols = 2, margin = 0f, gap = 0f)
    assertEquals(0, GridMath.cellAt(cells, 10f, 10f))
    assertEquals(3, GridMath.cellAt(cells, 150f, 150f))
    assertEquals(-1, GridMath.cellAt(cells, 250f, 10f))
  }

  @Test fun imageAdjustFallsBackToTheLayoutMode() {
    assertEquals(ScaleMode.CROP, ImageAdjust().modeOr(ScaleMode.CROP))
    assertEquals(ScaleMode.FIT, ImageAdjust(scaleMode = ScaleMode.FIT).modeOr(ScaleMode.CROP))
    assertTrue(ImageAdjust().isCentered)
    assertFalse(ImageAdjust(panX = 0f).isCentered)
  }
}
