package com.pdfman.app.ui.main

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfman.app.data.Creation
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel, snackbar: SnackbarHostState, onNew: () -> Unit) {
  val creations by vm.creations.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val listState = rememberLazyListState()
  val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
  var pendingRemoval by remember { mutableStateOf<Creation?>(null) }

  pendingRemoval?.let { creation ->
    AlertDialog(
      onDismissRequest = { pendingRemoval = null },
      icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
      title = { Text("Remove from recents?") },
      text = { Text("“${creation.name.removeSuffix(".pdf")}” will no longer be editable in pdfman. The PDF file itself is kept.") },
      confirmButton = {
        TextButton(onClick = { vm.removeCreation(creation); pendingRemoval = null }) { Text("Remove") }
      },
      dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text("Cancel") } },
    )
  }

  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      LargeTopAppBar(
        title = { Text("pdfman", fontWeight = FontWeight.SemiBold) },
        actions = {
          IconButton(onClick = vm::openAbout) { Icon(Icons.Outlined.Info, contentDescription = "About pdfman") }
        },
        scrollBehavior = scrollBehavior,
      )
    },
    snackbarHost = { SnackbarHost(snackbar) },
    floatingActionButton = {
      if (creations.isNotEmpty()) {
        ExtendedFloatingActionButton(
          onClick = onNew,
          expanded = fabExpanded,
          icon = { Icon(Icons.Filled.Add, contentDescription = null) },
          text = { Text("New PDF") },
        )
      }
    },
  ) { padding ->
    if (creations.isEmpty()) {
      EmptyState(
        Modifier.padding(padding),
        title = "Images to PDF in seconds",
        body = "Pick some photos, choose a grid, and save. Saved PDFs appear here so you can edit them later.",
        actionLabel = "Add images",
        onAction = onNew,
      )
    } else {
      LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
          start = 16.dp,
          end = 16.dp,
          top = padding.calculateTopPadding(),
          bottom = padding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        item { SectionHeader("Recent creations", Modifier.padding(start = 0.dp)) }
        items(creations, key = { it.creation.id }) { item ->
          CreationCard(
            item = item,
            onEdit = { vm.edit(item.creation) },
            onOpen = {
              if (!openPdf(context, item.creation.pdfUri)) {
                scope.launch { snackbar.showSnackbar("Couldn't open the PDF. It may have been moved or deleted.") }
              }
            },
            onShare = { runCatching { sharePdf(context, item.creation.pdfUri) } },
            onRemove = { pendingRemoval = item.creation },
            modifier = Modifier.animateItem(),
          )
        }
      }
    }
  }
}

@Composable
private fun CreationCard(
  item: CreationItem,
  onEdit: () -> Unit,
  onOpen: () -> Unit,
  onShare: () -> Unit,
  onRemove: () -> Unit,
  modifier: Modifier,
) {
  val c = item.creation
  val s = c.settings
  val title = c.name.removeSuffix(".pdf")
  var menu by remember { mutableStateOf(false) }
  Card(
    onClick = onEdit,
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    ListItem(
      colors = ListItemDefaults.colors(containerColor = Color.Transparent),
      leadingContent = {
        Box(
          Modifier.size(width = 54.dp, height = 72.dp).clip(RoundedCornerShape(4.dp)).background(Color.White),
          contentAlignment = Alignment.Center,
        ) {
          item.thumb?.let { BitmapImage(it, Modifier.size(width = 54.dp, height = 72.dp), ContentScale.Fit) }
        }
      },
      headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
      supportingContent = {
        val pages = "${c.pageCount} page${if (c.pageCount == 1) "" else "s"}"
        Text("${s.cols} × ${s.rows} · ${s.pageSize.label} · $pages\n${timeAgo(c.updatedAt)}", maxLines = 2)
      },
      trailingContent = {
        Box {
          IconButton(onClick = { menu = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More options for $title")
          }
          DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
              text = { Text("Edit") },
              leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
              onClick = { menu = false; onEdit() },
            )
            DropdownMenuItem(
              text = { Text("Open") },
              leadingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null) },
              onClick = { menu = false; onOpen() },
            )
            DropdownMenuItem(
              text = { Text("Share") },
              leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
              onClick = { menu = false; onShare() },
            )
            DropdownMenuItem(
              text = { Text("Remove from recents") },
              leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
              onClick = { menu = false; onRemove() },
            )
          }
        }
      },
    )
  }
}

private fun timeAgo(time: Long): String {
  val now = System.currentTimeMillis()
  return if (now - time < DateUtils.MINUTE_IN_MILLIS) "Just now"
  else DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS).toString()
}
