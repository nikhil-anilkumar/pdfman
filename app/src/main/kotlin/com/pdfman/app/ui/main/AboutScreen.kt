package com.pdfman.app.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pdfman.app.BuildConfig
import com.pdfman.app.R
import kotlinx.coroutines.launch

private const val DEVELOPER = "Nikhil Anilkumar"
private const val APACHE_2 = "Apache License 2.0"
private const val APACHE_2_URL = "https://www.apache.org/licenses/LICENSE-2.0"
private const val GOOGLE = "Google LLC"
private const val JETBRAINS = "JetBrains s.r.o."

/** An open-source component bundled in the app, with its copyright owner and license. */
private data class Library(
  val name: String,
  val owner: String,
  val version: String,
  val url: String,
  val license: String = APACHE_2,
)

private val LIBRARIES = listOf(
  Library("Jetpack Compose", GOOGLE, "BOM ${BuildConfig.COMPOSE_BOM_VERSION}", "https://developer.android.com/compose"),
  Library("Compose Material 3", GOOGLE, "BOM ${BuildConfig.COMPOSE_BOM_VERSION}", "https://developer.android.com/jetpack/androidx/releases/compose-material3"),
  Library("Compose Material Icons", GOOGLE, "BOM ${BuildConfig.COMPOSE_BOM_VERSION}", "https://developer.android.com/jetpack/androidx/releases/compose-material"),
  Library("AndroidX Activity", GOOGLE, BuildConfig.ACTIVITY_VERSION, "https://developer.android.com/jetpack/androidx/releases/activity"),
  Library("AndroidX Core", GOOGLE, BuildConfig.CORE_VERSION, "https://developer.android.com/jetpack/androidx/releases/core"),
  Library("AndroidX Lifecycle", GOOGLE, BuildConfig.LIFECYCLE_VERSION, "https://developer.android.com/jetpack/androidx/releases/lifecycle"),
  Library("Kotlin Standard Library", JETBRAINS, BuildConfig.KOTLIN_VERSION, "https://kotlinlang.org"),
  Library("kotlinx.coroutines", JETBRAINS, BuildConfig.COROUTINES_VERSION, "https://github.com/Kotlin/kotlinx.coroutines"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutScreen(onBack: () -> Unit) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current
  val snackbar = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val open: (String) -> Unit = { url ->
    if (!openUrl(context, url)) scope.launch { snackbar.showSnackbar("No browser available to open the link") }
  }
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      TopAppBar(
        title = { Text("About") },
        navigationIcon = {
          IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        },
        scrollBehavior = scrollBehavior,
      )
    },
    snackbarHost = { SnackbarHost(snackbar) },
  ) { padding ->
    LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp)) {
      item { AppHeader() }

      item { SectionHeader("Developer") }
      item {
        ListItem(
          leadingContent = { Icon(Icons.Outlined.Person, contentDescription = null) },
          headlineContent = { Text(DEVELOPER) },
          supportingContent = { Text("Design and development") },
        )
      }

      item { SectionHeader("Privacy") }
      item {
        ListItem(
          leadingContent = { Icon(Icons.Outlined.Lock, contentDescription = null) },
          headlineContent = { Text("Works completely offline") },
          supportingContent = {
            Text("pdfman has no internet access. It only reads the images you choose, and your PDFs stay on your device.")
          },
        )
      }

      item { HorizontalDivider(Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.outlineVariant) }
      item { SectionHeader("Open-source libraries") }
      item {
        Text(
          "pdfman is built with these open-source libraries. Tap one to visit its project page.",
          Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      items(LIBRARIES, key = { it.name }) { lib ->
        ListItem(
          modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Open project page") { open(lib.url) },
          leadingContent = { Icon(Icons.Outlined.Code, contentDescription = null) },
          headlineContent = { Text(lib.name) },
          supportingContent = { Text("${lib.owner}\n${lib.version} · ${lib.license}") },
          trailingContent = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null) },
        )
      }
      item {
        ListItem(
          modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Read the license") { open(APACHE_2_URL) },
          leadingContent = { Icon(Icons.Outlined.Gavel, contentDescription = null) },
          headlineContent = { Text(APACHE_2) },
          supportingContent = { Text("All libraries above are distributed under this license. Tap to read it.") },
          trailingContent = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null) },
        )
      }
      item {
        Text(
          "© 2026 $DEVELOPER",
          Modifier.fillMaxWidth().padding(top = 24.dp),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

@Composable
private fun AppHeader() {
  Column(
    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Image(
      painterResource(R.drawable.ic_launcher_foreground),
      contentDescription = null,
      modifier = Modifier
        .size(96.dp)
        .clip(MaterialTheme.shapes.extraLarge)
        .background(colorResource(R.color.ic_launcher_background)),
    )
    Spacer(Modifier.height(16.dp))
    Text("pdfman", style = MaterialTheme.typography.headlineMedium)
    Text(
      "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))
    Text(
      "Turn your photos into PDFs with simple grid layouts. Fit or crop each image, pick any paper size, and edit your saved PDFs any time.",
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center,
    )
  }
}
