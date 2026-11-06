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
}
