package com.pdfman.app.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Root composable: owns the system launchers and switches between screens. */
@Composable
fun MainScreen(vm: MainViewModel) {
  val state by vm.state.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbar = remember { SnackbarHostState() }

  val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { vm.addImages(it) }
  val pickImages = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
  val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
    if (uri != null) vm.saveTo(uri)
  }

  LaunchedEffect(Unit) {
    vm.events.collect { event ->
      when (event) {
        is UiEvent.Saved -> {
          val msg = if (event.updated) "PDF updated" else "PDF saved"
          val res = snackbar.showSnackbar(msg, actionLabel = "Open", duration = SnackbarDuration.Long)
          if (res == SnackbarResult.ActionPerformed && !openPdf(context, event.uri)) {
            snackbar.showSnackbar("Couldn't open the PDF")
          }
        }
        is UiEvent.Share -> sharePdf(context, event.uri)
        is UiEvent.Error -> snackbar.showSnackbar(event.message)
        is UiEvent.PickSaveLocation -> {
          snackbar.showSnackbar("The original file is gone. Choose where to save it.")
          saver.launch(event.suggestedName)
        }
      }
    }
  }

  AnimatedContent(
    targetState = state.screen,
    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
    label = "screen",
  ) { screen ->
    when (screen) {
      Screen.HOME -> HomeScreen(vm, snackbar, onNew = pickImages)
      Screen.EDITOR -> EditorScreen(
        vm = vm,
        state = state,
        snackbar = snackbar,
        onAddImages = pickImages,
        onSave = { if (!vm.saveExisting()) saver.launch(vm.suggestedFileName()) },
      )
      Screen.ABOUT -> AboutScreen(onBack = vm::goHome)
    }
  }
}

internal fun openPdf(context: Context, uri: Uri): Boolean = try {
  context.startActivity(
    Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
  )
  true
} catch (_: Exception) {
  false
}

internal fun sharePdf(context: Context, uri: Uri) {
  val send = Intent(Intent.ACTION_SEND).setType("application/pdf")
    .putExtra(Intent.EXTRA_STREAM, uri)
    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  context.startActivity(Intent.createChooser(send, "Share PDF"))
}

internal fun openUrl(context: Context, url: String): Boolean = try {
  context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
  true
} catch (_: Exception) {
  false
}
