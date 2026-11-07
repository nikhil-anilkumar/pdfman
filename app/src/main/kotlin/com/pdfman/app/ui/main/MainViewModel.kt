package com.pdfman.app.ui.main

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pdfman.app.data.Creation
import com.pdfman.app.data.CreationStore
import com.pdfman.app.pdf.ImageAdjust
import com.pdfman.app.pdf.ImageLoader
import com.pdfman.app.pdf.PageDrawer
import com.pdfman.app.pdf.PdfComposer
import com.pdfman.app.pdf.PdfSettings
import com.pdfman.app.pdf.ScaleMode
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PickedImage(
  val id: Long,
  val uri: Uri,
  val thumb: Bitmap? = null,
  val failed: Boolean = false,
  /** This image's own fit/crop and crop position. */
  val adjust: ImageAdjust = ImageAdjust.DEFAULT,
)

enum class Screen { HOME, EDITOR, PAGE, ABOUT }

data class UiState(
  val screen: Screen = Screen.HOME,
  val images: List<PickedImage> = emptyList(),
  val settings: PdfSettings = PdfSettings(),
  val selectedId: Long? = null,
  val busyMessage: String? = null,
  /** 0..1 while a PDF is being written. */
  val busyProgress: Float = 0f,
  /** The saved creation being edited, or null for a new, never-saved PDF. */
  val editing: Creation? = null,
  /** True when the editor has changes that are not in a saved PDF yet. */
  val dirty: Boolean = false,
  /** Page shown in the page viewer (0-based). */
  val viewerPage: Int = 0,
) {
  val pageCount get() = settings.pageCount(images.size)

  /** Images on page [page] (0-based), in cell order. */
  fun imagesOnPage(page: Int): List<PickedImage> =
    images.drop(page * settings.perPage).take(settings.perPage)
}

/** A saved creation plus its decoded page-1 thumbnail for the home list. */
data class CreationItem(val creation: Creation, val thumb: Bitmap?)

sealed interface UiEvent {
  data class Saved(val uri: Uri, val updated: Boolean) : UiEvent
  data class Share(val uri: Uri) : UiEvent
  data class Error(val message: String) : UiEvent
  /** The original file of an edited creation can't be written any more; ask for a new location. */
  data class PickSaveLocation(val suggestedName: String) : UiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
  private val resolver = app.contentResolver
  private val store = CreationStore(app)
  private val thumbDispatcher = Dispatchers.IO.limitedParallelism(4)
  private var nextId = 0L

  private val _state = MutableStateFlow(UiState())
  val state = _state.asStateFlow()

  private val _previews = MutableStateFlow<List<Bitmap>>(emptyList())
  val previews = _previews.asStateFlow()

  private val _creations = MutableStateFlow<List<CreationItem>>(emptyList())
  val creations = _creations.asStateFlow()

  private val _events = Channel<UiEvent>(Channel.BUFFERED)
  val events = _events.receiveAsFlow()

  init {
    viewModelScope.launch {
      _state.map { it.images to it.settings }.distinctUntilChanged().collectLatest { (images, settings) ->
        delay(80) // coalesce rapid changes such as slider drags
        _previews.value = withContext(Dispatchers.Default) {
          images.chunked(settings.perPage).take(MAX_PREVIEW_PAGES).map { chunk ->
            PageDrawer.renderPreview(settings, chunk.map { it.thumb }, chunk.map { it.adjust }, PREVIEW_WIDTH_PX)
          }
        }
      }
    }
    refreshCreations()
  }

  // ---- Navigation -------------------------------------------------------------------------------

  /** Starts a brand-new PDF from freshly picked or shared images. */
  fun startNew(uris: List<Uri>) {
    if (uris.isEmpty()) return
    _state.value = UiState(screen = Screen.EDITOR)
    addImages(uris)
  }

  /** Re-opens a saved creation in the editor with its images and settings. */
  fun edit(creation: Creation) {
    _state.value = UiState(screen = Screen.EDITOR, settings = creation.settings, editing = creation)
    addImages(creation.imageFiles.map(Uri::fromFile), markDirty = false, adjusts = creation.imageAdjusts)
  }

  /** Opens the page viewer on [page] (0-based), where images can be moved within their cells. */
  fun openPage(page: Int) = _state.update { st ->
    st.copy(screen = Screen.PAGE, viewerPage = page.coerceIn(0, (st.pageCount - 1).coerceAtLeast(0)), selectedId = null)
  }

  fun showPage(page: Int) = _state.update { st ->
    st.copy(viewerPage = page.coerceIn(0, (st.pageCount - 1).coerceAtLeast(0)), selectedId = null)
  }

  fun closePage() = _state.update { it.copy(screen = Screen.EDITOR, selectedId = null) }

  fun openAbout() = _state.update { it.copy(screen = Screen.ABOUT) }

  fun goHome() {
    _state.value = UiState(screen = Screen.HOME)
    refreshCreations()
  }

  // ---- Editing ----------------------------------------------------------------------------------

  fun addImages(uris: List<Uri>, markDirty: Boolean = true, adjusts: List<ImageAdjust> = emptyList()) {
    if (uris.isEmpty()) return
    if (_state.value.screen == Screen.HOME) return startNew(uris)
    val added = uris.mapIndexed { i, uri -> PickedImage(nextId++, uri, adjust = adjusts.getOrNull(i) ?: ImageAdjust.DEFAULT) }
    _state.update { it.copy(images = it.images + added, dirty = it.dirty || markDirty) }
    added.forEach { img ->
      viewModelScope.launch(thumbDispatcher) {
        val thumb = ImageLoader.load(resolver, img.uri, THUMB_SIZE_PX)
        _state.update { st ->
          st.copy(images = st.images.map { if (it.id == img.id) it.copy(thumb = thumb, failed = thumb == null) else it })
        }
      }
    }
  }

  fun select(id: Long) = _state.update { it.copy(selectedId = if (it.selectedId == id) null else id) }

  /** Selects [id] without toggling (null clears the selection). */
  fun setSelected(id: Long?) = _state.update { it.copy(selectedId = id) }

  /** Sets one image's fit/crop; null goes back to the layout setting. */
  fun setImageMode(id: Long, mode: ScaleMode?) = updateAdjust(id) { it.copy(scaleMode = mode) }

  /** Moves which part of a cropped image shows; see [ImageAdjust]. */
  fun setImagePan(id: Long, panX: Float, panY: Float) = updateAdjust(id) { it.copy(panX = panX, panY = panY) }

  fun resetImagePan(id: Long) = updateAdjust(id) { it.copy(panX = ImageAdjust.CENTER, panY = ImageAdjust.CENTER) }

  private fun updateAdjust(id: Long, transform: (ImageAdjust) -> ImageAdjust) = _state.update { st ->
    var changed = false
    val images = st.images.map { img ->
      if (img.id != id) img else transform(img.adjust).let { new -> changed = new != img.adjust; img.copy(adjust = new) }
    }
    if (changed) st.copy(images = images, dirty = true) else st
  }

  fun moveSelected(delta: Int) = _state.update { st ->
    val from = st.images.indexOfFirst { it.id == st.selectedId }
    val to = from + delta
    if (from < 0 || to !in st.images.indices) return@update st
    val list = st.images.toMutableList()
    list.add(to, list.removeAt(from))
    st.copy(images = list, dirty = true)
  }

  fun removeSelected() = _state.update { st ->
    st.copy(images = st.images.filterNot { it.id == st.selectedId }, selectedId = null, dirty = true)
  }

  fun updateSettings(transform: (PdfSettings) -> PdfSettings) =
    _state.update { st -> transform(st.settings).let { new -> if (new == st.settings) st else st.copy(settings = new, dirty = true) } }

  // ---- Saving -----------------------------------------------------------------------------------

  fun suggestedFileName(): String =
    "pdfman-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".pdf"

  /** Save button: overwrite the original file when editing, otherwise the UI asks for a location. */
  fun saveExisting(): Boolean {
    val editing = _state.value.editing ?: return false
    runExport { uris, adjusts, settings ->
      val ok = runCatching {
        resolver.openOutputStream(editing.pdfUri, "wt")!!.use { out ->
          PdfComposer.write(resolver, uris, adjusts, settings, out, ::reportProgress)
        }
      }.isSuccess
      if (!ok) return@runExport UiEvent.PickSaveLocation(suggestedFileName())
      record(editing.pdfUri, uris, adjusts, settings)
      UiEvent.Saved(editing.pdfUri, updated = true)
    }
    return true
  }

  /** Saves to a location chosen with the system "Save as" dialog and records it as a creation. */
  fun saveTo(target: Uri) = runExport { uris, adjusts, settings ->
    resolver.openOutputStream(target, "wt")?.use { out ->
      PdfComposer.write(resolver, uris, adjusts, settings, out, ::reportProgress)
    } ?: error("Could not open the destination file")
    runCatching {
      resolver.takePersistableUriPermission(
        target, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
      )
    }
    record(target, uris, adjusts, settings)
    UiEvent.Saved(target, updated = false)
  }

  fun share() = runExport { uris, adjusts, settings ->
    val dir = File(getApplication<Application>().cacheDir, "shared").apply { mkdirs() }
    dir.listFiles()?.forEach { it.delete() }
    val file = File(dir, suggestedFileName())
    file.outputStream().use { PdfComposer.write(resolver, uris, adjusts, settings, it, ::reportProgress) }
    UiEvent.Share(shareUriFor(file))
  }

  /** Stores the creation (images + settings + thumbnail) so it shows on the home screen and can be edited. */
  private fun record(pdfUri: Uri, uris: List<Uri>, adjusts: List<ImageAdjust>, settings: PdfSettings) {
    val st = _state.value
    val id = st.editing?.id ?: store.newId()
    val firstPage = st.images.take(settings.perPage)
    val thumb = PageDrawer.renderPreview(settings, firstPage.map { it.thumb }, adjusts.take(settings.perPage), CREATION_THUMB_PX)
    val creation = store.save(
      id = id,
      name = displayName(pdfUri) ?: st.editing?.name ?: suggestedFileName(),
      pdfUri = pdfUri,
      createdAt = st.editing?.createdAt ?: System.currentTimeMillis(),
      pageCount = settings.pageCount(uris.size),
      settings = settings,
      images = uris,
      adjusts = adjusts,
      thumbnail = thumb,
    )
    // Point the editor at the stored copies so later saves don't copy the images again.
    _state.update { s ->
      val images = if (s.images.size == creation.imageFiles.size) {
        s.images.mapIndexed { i, img -> img.copy(uri = Uri.fromFile(creation.imageFiles[i])) }
      } else s.images
      s.copy(editing = creation, images = images, dirty = false)
    }
  }

  // ---- Home screen ------------------------------------------------------------------------------

  fun refreshCreations() {
    viewModelScope.launch(Dispatchers.IO) {
      _creations.value = store.loadAll().map { c ->
        CreationItem(c, runCatching { BitmapFactory.decodeFile(c.thumbFile.path) }.getOrNull())
      }
    }
  }

  fun removeCreation(creation: Creation) {
    viewModelScope.launch(Dispatchers.IO) {
      store.delete(creation.id)
      runCatching {
        resolver.releasePersistableUriPermission(
          creation.pdfUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
      }
      refreshCreations()
    }
  }

  // ---- Helpers ----------------------------------------------------------------------------------

  private fun displayName(uri: Uri): String? = runCatching {
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
      if (c.moveToFirst()) c.getString(0) else null
    }
  }.getOrNull()

  private fun shareUriFor(file: File): Uri {
    val app = getApplication<Application>()
    return FileProvider.getUriForFile(app, "${app.packageName}.files", file)
  }

  private fun reportProgress(page: Int, total: Int) =
    _state.update { it.copy(busyMessage = "Rendering page $page of $total…", busyProgress = (page - 1f) / total) }

  private fun runExport(block: suspend (List<Uri>, List<ImageAdjust>, PdfSettings) -> UiEvent) {
    val st = _state.value
    if (st.images.isEmpty() || st.busyMessage != null) return
    _state.update { it.copy(busyMessage = "Preparing…", busyProgress = 0f) }
    viewModelScope.launch {
      val event = try {
        withContext(Dispatchers.IO) { block(st.images.map { it.uri }, st.images.map { it.adjust }, st.settings) }
      } catch (e: Throwable) {
        UiEvent.Error(e.message ?: "Failed to create PDF")
      }
      _state.update { it.copy(busyMessage = null) }
      _events.send(event)
    }
  }

  companion object {
    const val THUMB_SIZE_PX = 480
    const val PREVIEW_WIDTH_PX = 560
    const val MAX_PREVIEW_PAGES = 12
    const val CREATION_THUMB_PX = 240
  }
}
