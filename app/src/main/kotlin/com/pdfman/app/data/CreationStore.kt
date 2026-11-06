package com.pdfman.app.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import com.pdfman.app.pdf.PageSize
import com.pdfman.app.pdf.PdfSettings
import com.pdfman.app.pdf.Quality
import com.pdfman.app.pdf.ScaleMode
import java.io.File
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** A PDF the user saved. Its source images are copied into app storage so it can be edited later. */
data class Creation(
  val id: String,
  val name: String,
  val pdfUri: Uri,
  val createdAt: Long,
  val updatedAt: Long,
  val pageCount: Int,
  val settings: PdfSettings,
  val imageFiles: List<File>,
  val thumbFile: File,
)

/**
 * Stores each creation in `files/creations/<id>/` as `creation.json`, a `thumb.png` of page 1, and
 * copies of the source images (so edits survive the original photos being moved or permissions expiring).
 */
class CreationStore(context: Context) {
  private val root = File(context.filesDir, "creations").apply { mkdirs() }
  private val resolver: ContentResolver = context.contentResolver

  fun newId(): String = UUID.randomUUID().toString()

  fun dirFor(id: String) = File(root, id)

  fun loadAll(): List<Creation> =
    root.listFiles().orEmpty().mapNotNull { dir -> runCatching { read(dir) }.getOrNull() }
      .sortedByDescending { it.updatedAt }

  /**
   * Saves (or updates) a creation. Images not already inside this creation's folder are copied in;
   * files no longer used are deleted.
   */
  fun save(
    id: String,
    name: String,
    pdfUri: Uri,
    createdAt: Long,
    pageCount: Int,
    settings: PdfSettings,
    images: List<Uri>,
    thumbnail: Bitmap?,
  ): Creation {
    val dir = dirFor(id).apply { mkdirs() }
    val files = images.map { uri -> ownedFile(dir, uri) ?: copyIn(dir, uri) }
    val keep = files.map { it.name }.toSet() + setOf(JSON, THUMB)
    dir.listFiles().orEmpty().filter { it.name !in keep }.forEach { it.delete() }

    val thumbFile = File(dir, THUMB)
    thumbnail?.let { bmp -> thumbFile.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }

    val now = System.currentTimeMillis()
    val json = JSONObject()
      .put("name", name)
      .put("pdfUri", pdfUri.toString())
      .put("createdAt", createdAt)
      .put("updatedAt", now)
      .put("pageCount", pageCount)
      .put("settings", settings.toJson())
      .put("images", JSONArray(files.map { it.name }))
    File(dir, JSON).writeText(json.toString())
    return Creation(id, name, pdfUri, createdAt, now, pageCount, settings, files, thumbFile)
  }

  fun delete(id: String) {
    dirFor(id).deleteRecursively()
  }

  private fun read(dir: File): Creation {
    val json = JSONObject(File(dir, JSON).readText())
    val images = json.getJSONArray("images").let { arr -> (0 until arr.length()).map { File(dir, arr.getString(it)) } }
    return Creation(
      id = dir.name,
      name = json.getString("name"),
      pdfUri = json.getString("pdfUri").toUri(),
      createdAt = json.getLong("createdAt"),
      updatedAt = json.getLong("updatedAt"),
      pageCount = json.getInt("pageCount"),
      settings = settingsFromJson(json.getJSONObject("settings")),
      imageFiles = images,
      thumbFile = File(dir, THUMB),
    )
  }

  /** Returns the file if [uri] already points into [dir]. */
  private fun ownedFile(dir: File, uri: Uri): File? {
    if (uri.scheme != ContentResolver.SCHEME_FILE) return null
    val file = File(uri.path ?: return null)
    return file.takeIf { it.parentFile?.canonicalPath == dir.canonicalPath && it.exists() }
  }

  private fun copyIn(dir: File, uri: Uri): File {
    val file = File(dir, "img_${UUID.randomUUID()}")
    resolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
      ?: error("Could not read an image")
    return file
  }

  companion object {
    private const val JSON = "creation.json"
    private const val THUMB = "thumb.png"

    fun PdfSettings.toJson(): JSONObject = JSONObject()
      .put("rows", rows)
      .put("cols", cols)
      .put("scaleMode", scaleMode.name)
      .put("pageSize", pageSize.name)
      .put("landscape", landscape)
      .put("margin", margin.toDouble())
      .put("gap", gap.toDouble())
      .put("autoRotate", autoRotate)
      .put("borders", borders)
      .put("quality", quality.name)

    fun settingsFromJson(j: JSONObject): PdfSettings {
      val d = PdfSettings()
      return PdfSettings(
        rows = j.optInt("rows", d.rows),
        cols = j.optInt("cols", d.cols),
        scaleMode = runCatching { ScaleMode.valueOf(j.getString("scaleMode")) }.getOrDefault(d.scaleMode),
        pageSize = runCatching { PageSize.valueOf(j.getString("pageSize")) }.getOrDefault(d.pageSize),
        landscape = j.optBoolean("landscape", d.landscape),
        margin = j.optDouble("margin", d.margin.toDouble()).toFloat(),
        gap = j.optDouble("gap", d.gap.toDouble()).toFloat(),
        autoRotate = j.optBoolean("autoRotate", d.autoRotate),
        borders = j.optBoolean("borders", d.borders),
        quality = runCatching { Quality.valueOf(j.getString("quality")) }.getOrDefault(d.quality),
      )
    }
  }
}
