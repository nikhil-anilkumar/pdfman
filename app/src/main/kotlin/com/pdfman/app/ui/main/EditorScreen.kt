package com.pdfman.app.ui.main

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfman.app.pdf.PageSize
import com.pdfman.app.pdf.PaperGroup
import com.pdfman.app.pdf.PdfSettings
import com.pdfman.app.pdf.Quality
import com.pdfman.app.pdf.ScaleMode
import com.pdfman.app.pdf.ptToMm
import java.util.Locale
import kotlin.math.roundToInt

private val PREVIEW_HEIGHT = 190.dp
private val THUMB_SIZE = 80.dp
private val GRID_PRESETS = listOf(1 to 1, 1 to 2, 2 to 1, 2 to 2, 3 to 2, 2 to 3, 3 to 3, 4 to 3, 4 to 4)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorScreen(
  vm: MainViewModel,
  state: UiState,
  snackbar: SnackbarHostState,
  onAddImages: () -> Unit,
  onSave: () -> Unit,
) {
  val previews by vm.previews.collectAsStateWithLifecycle()
  var confirmLeave by remember { mutableStateOf(false) }
  val leave = { if (state.dirty && state.images.isNotEmpty()) confirmLeave = true else vm.goHome() }
  BackHandler(onBack = leave)
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

  if (confirmLeave) {
    val isNew = state.editing == null
    AlertDialog(
      onDismissRequest = { confirmLeave = false },
      title = { Text(if (isNew) "Discard this PDF?" else "Discard changes?") },
      text = {
        Text(if (isNew) "It hasn't been saved yet." else "Edits since the last save will be lost. The saved PDF stays as it was.")
      },
      confirmButton = { TextButton(onClick = { confirmLeave = false; vm.goHome() }) { Text("Discard") } },
      dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Keep editing") } },
    )
  }

  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      TopAppBar(
        navigationIcon = {
          IconButton(onClick = leave) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        },
        title = {
          Text(state.editing?.name?.removeSuffix(".pdf") ?: "New PDF", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        actions = {
          IconButton(onClick = onAddImages) { Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = "Add images") }
        },
        scrollBehavior = scrollBehavior,
      )
    },
    snackbarHost = { SnackbarHost(snackbar) },
    bottomBar = {
      if (state.images.isNotEmpty()) BottomActions(state = state, onSave = onSave, onShare = vm::share)
    },
  ) { padding ->
    if (state.images.isEmpty()) {
      EmptyState(
        Modifier.padding(padding),
        title = "No images yet",
        body = "Add the photos you want on your pages.",
        actionLabel = "Add images",
        onAction = onAddImages,
      )
    } else {
      Column(Modifier.padding(padding).fillMaxSize()) {
        // The preview stays pinned; settings scroll underneath it.
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 2.dp) {
          Column { PreviewStrip(previews, state.pageCount, state.settings) }
        }
        Column(
          Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        ) {
          ImagesStrip(state, onAdd = onAddImages, onSelect = vm::select)
          if (state.selectedId != null) {
            SelectionActions(
              canMoveLeft = state.images.firstOrNull()?.id != state.selectedId,
              canMoveRight = state.images.lastOrNull()?.id != state.selectedId,
              onLeft = { vm.moveSelected(-1) },
              onRight = { vm.moveSelected(1) },
              onRemove = vm::removeSelected,
            )
          }
          Settings(state.settings, vm::updateSettings)
        }
      }
    }
  }
}

@Composable
private fun ImagesStrip(state: UiState, onAdd: () -> Unit, onSelect: (Long) -> Unit) {
  SectionHeader("Images · ${state.images.size}")
  Text(
    "Tap an image to move or remove it.",
    Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
  val shape = MaterialTheme.shapes.medium
  LazyRow(
    contentPadding = PaddingValues(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    itemsIndexed(state.images, key = { _, img -> img.id }) { index, img ->
      val selected = img.id == state.selectedId
      Box(
        Modifier
          .size(THUMB_SIZE)
          .clip(shape)
          .background(MaterialTheme.colorScheme.surfaceVariant)
          .border(if (selected) 3.dp else 0.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, shape)
          .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onSelect(img.id) })
          .semantics { contentDescription = "Image ${index + 1}" },
      ) {
        img.thumb?.let { BitmapImage(it, Modifier.fillMaxSize(), ContentScale.Crop) }
        if (img.failed) {
          Icon(
            Icons.Outlined.BrokenImage, contentDescription = "Couldn't load image",
            Modifier.align(Alignment.Center), tint = MaterialTheme.colorScheme.error,
          )
        }
        Surface(
          Modifier.align(Alignment.TopStart).padding(4.dp),
          shape = CircleShape,
          color = if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.55f),
          contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White,
        ) {
          if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, Modifier.padding(2.dp).size(16.dp))
          } else {
            Text("${index + 1}", Modifier.padding(horizontal = 7.dp, vertical = 1.dp), style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }
    item {
      OutlinedCard(onClick = onAdd, modifier = Modifier.size(THUMB_SIZE), shape = shape) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = "Add more images", tint = MaterialTheme.colorScheme.primary)
        }
      }
    }
  }
}

@Composable
private fun SelectionActions(
  canMoveLeft: Boolean,
  canMoveRight: Boolean,
  onLeft: () -> Unit,
  onRight: () -> Unit,
  onRemove: () -> Unit,
) {
  Row(
    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    val pad = PaddingValues(horizontal = 12.dp)
    FilledTonalButton(onClick = onLeft, enabled = canMoveLeft, modifier = Modifier.weight(1f), contentPadding = pad) {
      Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
      Spacer(Modifier.size(ButtonDefaults.IconSpacing))
      Text("Earlier", maxLines = 1)
    }
    FilledTonalButton(onClick = onRight, enabled = canMoveRight, modifier = Modifier.weight(1f), contentPadding = pad) {
      Text("Later", maxLines = 1)
      Spacer(Modifier.size(ButtonDefaults.IconSpacing))
      Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
    }
    OutlinedButton(
      onClick = onRemove,
      modifier = Modifier.weight(1f),
      contentPadding = pad,
      colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
    ) {
      Icon(Icons.Outlined.Delete, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
      Spacer(Modifier.size(ButtonDefaults.IconSpacing))
      Text("Remove", maxLines = 1)
    }
  }
}

@Composable
private fun PreviewStrip(previews: List<Bitmap>, pageCount: Int, s: PdfSettings) {
  val orientation = if (s.landscape) "landscape" else "portrait"
  Text(
    "Preview · $pageCount page${if (pageCount == 1) "" else "s"} · ${s.pageSize.label} $orientation",
    Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
  )
  LazyRow(
    Modifier.fillMaxWidth().height(PREVIEW_HEIGHT + 30.dp),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    itemsIndexed(previews) { index, bmp ->
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BitmapImage(
          bmp,
          Modifier
            .height(PREVIEW_HEIGHT)
            .aspectRatio(bmp.width.toFloat() / bmp.height)
            .shadow(3.dp, RoundedCornerShape(2.dp)),
          ContentScale.Fit,
          contentDescription = "Page ${index + 1} preview",
        )
        Spacer(Modifier.height(4.dp))
        Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    if (pageCount > previews.size && previews.isNotEmpty()) {
      item {
        Box(Modifier.height(PREVIEW_HEIGHT), contentAlignment = Alignment.Center) {
          Text("+${pageCount - previews.size} more", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Settings(s: PdfSettings, update: ((PdfSettings) -> PdfSettings) -> Unit) {
  SectionHeader("Grid layout")
  FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    GRID_PRESETS.forEach { (rows, cols) ->
      val selected = s.rows == rows && s.cols == cols
      FilterChip(
        selected = selected,
        onClick = { update { it.copy(rows = rows, cols = cols) } },
        label = { Text("$cols × $rows") },
        leadingIcon = {
          GridGlyph(
            rows, cols,
            if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary,
            Modifier.size(width = FilterChipDefaults.IconSize * 0.8f, height = FilterChipDefaults.IconSize),
          )
        },
        modifier = Modifier.semantics { contentDescription = "$cols columns by $rows rows" },
      )
    }
  }
  Row(
    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Stepper("Columns", s.cols, Modifier.weight(1f)) { v -> update { it.copy(cols = v) } }
    Stepper("Rows", s.rows, Modifier.weight(1f)) { v -> update { it.copy(rows = v) } }
  }

  SectionDivider()
  SectionHeader("Image placement")
  Segmented(ScaleMode.entries, s.scaleMode, { it.label }) { m -> update { it.copy(scaleMode = m) } }
  Text(
    if (s.scaleMode == ScaleMode.FIT) "The whole image is shown, which may leave white space."
    else "Images fill their cells; edges are cropped at the grid border.",
    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
  SwitchItem("Auto-rotate", "Turn images to match the cell's orientation", s.autoRotate) { v ->
    update { it.copy(autoRotate = v) }
  }
  SwitchItem("Cell borders", "Draw a thin outline around each cell", s.borders) { v -> update { it.copy(borders = v) } }

  SectionDivider()
  SectionHeader("Page")
  PaperPicker(s.pageSize) { p -> update { it.copy(pageSize = p) } }
  Spacer(Modifier.height(12.dp))
  Segmented(listOf(false, true), s.landscape, { if (it) "Landscape" else "Portrait" }) { l -> update { it.copy(landscape = l) } }

  SectionDivider()
  SectionHeader("Margins & spacing")
  PointSlider("Page margin", s.margin, PdfSettings.MAX_MARGIN) { v -> update { it.copy(margin = v) } }
  PointSlider("Space between images", s.gap, PdfSettings.MAX_GAP) { v -> update { it.copy(gap = v) } }

  SectionDivider()
  SectionHeader("Image quality")
  Segmented(Quality.entries, s.quality, { it.label }) { q -> update { it.copy(quality = q) } }
  Text(
    "${s.quality.dpi} dpi. Higher quality makes larger files.",
    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
private fun SectionDivider() = HorizontalDivider(Modifier.padding(top = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaperPicker(selected: PageSize, onSelect: (PageSize) -> Unit) {
  var expanded by remember { mutableStateOf(false) }
  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
  ) {
    OutlinedTextField(
      value = selected.label,
      onValueChange = {},
      readOnly = true,
      singleLine = true,
      label = { Text("Paper size") },
      supportingText = { Text(selected.dims) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      PaperGroup.entries.forEach { group ->
        Text(
          group.label,
          Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        PageSize.entries.filter { it.group == group }.forEach { size ->
          val isSelected = size == selected
          DropdownMenuItem(
            text = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(size.label, Modifier.weight(1f), fontWeight = if (isSelected) FontWeight.SemiBold else null)
                Spacer(Modifier.width(16.dp))
                Text(size.dims, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            },
            leadingIcon = {
              if (isSelected) Icon(Icons.Filled.Check, contentDescription = "Selected") else Spacer(Modifier.size(24.dp))
            },
            onClick = { onSelect(size); expanded = false },
            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
          )
        }
      }
    }
  }
}

@Composable
private fun PointSlider(label: String, valuePt: Float, maxPt: Float, onChange: (Float) -> Unit) {
  val valueText = if (valuePt == 0f) "None" else String.format(Locale.US, "%.1f mm", valuePt.ptToMm())
  Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
      Text(valueText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
    Slider(
      value = valuePt,
      onValueChange = { onChange(it.roundToInt().toFloat()) },
      valueRange = 0f..maxPt,
      modifier = Modifier.semantics {
        contentDescription = label
        stateDescription = valueText
      },
    )
  }
}

@Composable
private fun BottomActions(state: UiState, onSave: () -> Unit, onShare: () -> Unit) {
  Surface(color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 6.dp) {
    Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
      val busy = state.busyMessage
      if (busy != null) {
        Text(busy, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(progress = { state.busyProgress }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
      }
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
          onClick = onShare,
          enabled = busy == null,
          modifier = Modifier.height(56.dp),
          contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
          Icon(Icons.Outlined.Share, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
          Spacer(Modifier.size(ButtonDefaults.IconSpacing))
          Text("Share")
        }
        Button(
          onClick = onSave,
          enabled = busy == null,
          modifier = Modifier.weight(1f).height(56.dp),
          contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
          val n = state.pageCount
          val verb = if (state.editing != null) "Update PDF" else "Save PDF"
          Icon(Icons.Outlined.Save, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
          Spacer(Modifier.size(ButtonDefaults.IconSpacing))
          Text("$verb · $n page${if (n == 1) "" else "s"}", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }
    }
  }
}

@Composable
private fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
  SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
    options.forEachIndexed { i, opt ->
      SegmentedButton(
        selected = opt == selected,
        onClick = { onSelect(opt) },
        shape = SegmentedButtonDefaults.itemShape(i, options.size),
        label = { Text(label(opt), maxLines = 1, overflow = TextOverflow.Ellipsis) },
      )
    }
  }
}

@Composable
private fun Stepper(label: String, value: Int, modifier: Modifier, onChange: (Int) -> Unit) {
  OutlinedCard(modifier) {
    Text(
      label,
      Modifier.padding(start = 16.dp, top = 12.dp),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
    )
    Row(
      Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      FilledTonalIconButton(onClick = { onChange(value - 1) }, enabled = value > 1) {
        Icon(Icons.Filled.Remove, contentDescription = "Fewer ${label.lowercase()}")
      }
      Text("$value", textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
      FilledTonalIconButton(onClick = { onChange(value + 1) }, enabled = value < PdfSettings.MAX_GRID) {
        Icon(Icons.Filled.Add, contentDescription = "More ${label.lowercase()}")
      }
    }
  }
}

@Composable
private fun SwitchItem(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
  ListItem(
    headlineContent = { Text(title) },
    supportingContent = { Text(subtitle) },
    trailingContent = { Switch(checked = checked, onCheckedChange = null) },
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
  )
}
