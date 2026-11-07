package com.pdfman.app.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.withScale
import com.pdfman.app.pdf.GridMath
import com.pdfman.app.pdf.PageDrawer
import com.pdfman.app.pdf.ScaleMode

/**
 * Shows one page large. Tap an image to give it its own fit or crop; drag a cropped image to choose which
 * part of it shows inside its cell.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PageScreen(vm: MainViewModel, state: UiState) {
  BackHandler(onBack = vm::closePage)
  val page = state.viewerPage
  val pageImages = state.imagesOnPage(page)
  val selected = pageImages.firstOrNull { it.id == state.selectedId }
  val selectedIndex = pageImages.indexOf(selected)

  Scaffold(
    topBar = {
      TopAppBar(
        navigationIcon = {
          IconButton(onClick = vm::closePage) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        },
        title = { Text("Page ${page + 1} of ${state.pageCount}") },
        actions = {
          IconButton(onClick = { vm.showPage(page - 1) }, enabled = page > 0) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous page")
          }
          IconButton(onClick = { vm.showPage(page + 1) }, enabled = page < state.pageCount - 1) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next page")
          }
        },
      )
    },
    bottomBar = {
      Surface(color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 6.dp) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
          if (selected == null) {
            Text(
              "Tap an image to set its fit or crop. Drag a cropped image to move it within its cell.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            ImageControls(state, selected, page * state.settings.perPage + selectedIndex + 1, vm)
          }
        }
      }
    },
  ) { padding ->
    Box(
      Modifier.padding(padding).fillMaxSize().padding(16.dp),
      contentAlignment = Alignment.Center,
    ) {
      PageCanvas(state, pageImages, vm)
    }
  }
}

@Composable
private fun ImageControls(state: UiState, image: PickedImage, number: Int, vm: MainViewModel) {
  val layoutMode = state.settings.scaleMode
  val mode = image.adjust.modeOr(layoutMode)
  Text("Image $number", style = MaterialTheme.typography.titleSmall)
  Spacer(Modifier.height(8.dp))
  val options = listOf<ScaleMode?>(null, ScaleMode.FIT, ScaleMode.CROP)
  SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
    options.forEachIndexed { i, opt ->
      SegmentedButton(
        selected = image.adjust.scaleMode == opt,
        onClick = { vm.setImageMode(image.id, opt) },
        shape = SegmentedButtonDefaults.itemShape(i, options.size),
        label = {
          Text(
            when (opt) {
              null -> "Layout"
              ScaleMode.FIT -> "Fit"
              ScaleMode.CROP -> "Crop"
            },
            maxLines = 1,
          )
        },
      )
    }
  }
  Spacer(Modifier.height(8.dp))
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
      when {
        mode == ScaleMode.FIT -> "The whole image is shown. Pick Crop to move it."
        image.adjust.scaleMode == null -> "Follows the layout setting. Drag the image to move it."
        else -> "Drag the image to choose what shows."
      },
      Modifier.weight(1f),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    OutlinedButton(
      onClick = { vm.resetImagePan(image.id) },
      enabled = mode == ScaleMode.CROP && !image.adjust.isCentered,
      contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
      Icon(Icons.Outlined.CenterFocusStrong, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
      Spacer(Modifier.size(ButtonDefaults.IconSpacing))
      Text("Recenter")
    }
  }
}

@Composable
private fun PageCanvas(state: UiState, pageImages: List<PickedImage>, vm: MainViewModel) {
  val settings = state.settings
  val cells = remember(settings) { settings.cells() }
  val pageRatio = settings.pageWidth / settings.pageHeight
  // Gesture callbacks outlive recompositions, so read the latest state through these.
  val latestImages by rememberUpdatedState(pageImages)
  val latestSettings by rememberUpdatedState(settings)
  val selectionColor = MaterialTheme.colorScheme.primary
  val strokePx = with(LocalDensity.current) { 3.dp.toPx() }

  BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    val fitsWidth = maxWidth / maxHeight < pageRatio
    val w = if (fitsWidth) maxWidth else maxHeight * pageRatio
    val h = if (fitsWidth) maxWidth / pageRatio else maxHeight
    Canvas(
      Modifier
        .size(w, h)
        .shadow(4.dp, RoundedCornerShape(2.dp))
        .semantics { contentDescription = "Page preview. Tap an image to select it, drag to move a cropped image." }
        .pointerInput(cells) {
          detectTapGestures { pos ->
            val scale = size.width / latestSettings.pageWidth
            val index = GridMath.cellAt(cells, pos.x / scale, pos.y / scale)
            vm.setSelected(latestImages.getOrNull(index)?.id)
          }
        }
        .pointerInput(cells) {
          var dragIndex = -1
          detectDragGestures(
            onDragStart = { pos ->
              val scale = size.width / latestSettings.pageWidth
              dragIndex = GridMath.cellAt(cells, pos.x / scale, pos.y / scale)
              latestImages.getOrNull(dragIndex)?.let { vm.setSelected(it.id) }
            },
            onDrag = { change, amount ->
              val img = latestImages.getOrNull(dragIndex) ?: return@detectDragGestures
              val thumb = img.thumb ?: return@detectDragGestures
              val s = latestSettings
              if (img.adjust.modeOr(s.scaleMode) != ScaleMode.CROP) return@detectDragGestures
              change.consume()
              val scale = size.width / s.pageWidth
              val cell = cells[dragIndex]
              val (px, py) = GridMath.panAfterDrag(
                thumb.width.toFloat(), thumb.height.toFloat(), cell, PageDrawer.isRotated(thumb, cell, s),
                img.adjust.panX, img.adjust.panY, amount.x / scale, amount.y / scale,
              )
              vm.setImagePan(img.id, px, py)
            },
          )
        },
    ) {
      val scale = size.width / settings.pageWidth
      drawIntoCanvas { canvas ->
        canvas.nativeCanvas.withScale(scale, scale) {
          PageDrawer.draw(this, settings, cells, pageImages.map { it.thumb }, pageImages.map { it.adjust }, preview = true)
        }
      }
      val selectedIndex = pageImages.indexOfFirst { it.id == state.selectedId }
      cells.getOrNull(selectedIndex)?.let { cell ->
        drawRect(
          color = selectionColor,
          topLeft = Offset(cell.left * scale, cell.top * scale),
          size = Size(cell.width * scale, cell.height * scale),
          style = Stroke(strokePx),
        )
      }
    }
  }
}
