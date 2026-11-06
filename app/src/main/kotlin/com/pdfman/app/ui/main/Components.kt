package com.pdfman.app.ui.main

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Empty state shown before any images or creations exist. */
@Composable
internal fun EmptyState(modifier: Modifier, title: String, body: String, actionLabel: String, onAction: () -> Unit) {
  Column(
    modifier.fillMaxSize().padding(horizontal = 32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(120.dp)) {
      Box(contentAlignment = Alignment.Center) {
        GridGlyph(2, 2, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.size(width = 48.dp, height = 62.dp))
      }
    }
    Spacer(Modifier.height(24.dp))
    Text(
      title,
      Modifier.semantics { heading() },
      style = MaterialTheme.typography.headlineSmall,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
      body,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(32.dp))
    Button(onClick = onAction, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
      Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
      Spacer(Modifier.size(ButtonDefaults.IconSpacing))
      Text(actionLabel)
    }
  }
}

/** Section header in the Material 3 list-subheader style. */
@Composable
internal fun SectionHeader(text: String, modifier: Modifier = Modifier) {
  Text(
    text,
    modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp).semantics { heading() },
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.primary,
  )
}

@Composable
internal fun BitmapImage(bitmap: Bitmap, modifier: Modifier, scale: ContentScale, contentDescription: String? = null) {
  val image = remember(bitmap) { bitmap.asImageBitmap() }
  Image(image, contentDescription = contentDescription, modifier = modifier, contentScale = scale)
}

/** Small drawing of a page split into a rows × cols grid, used for layout chips and the empty state. */
@Composable
internal fun GridGlyph(rows: Int, cols: Int, color: Color, modifier: Modifier) {
  Canvas(modifier) {
    val gap = size.minDimension * 0.1f
    val cw = (size.width - gap * (cols - 1)) / cols
    val ch = (size.height - gap * (rows - 1)) / rows
    val r = CornerRadius(size.minDimension * 0.06f)
    for (row in 0 until rows) for (col in 0 until cols) {
      drawRoundRect(color, Offset(col * (cw + gap), row * (ch + gap)), Size(cw, ch), r)
    }
  }
}
