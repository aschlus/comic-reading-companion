package com.aschlus.comicreadingcompanion

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aschlus.comicreadingcompanion.data.exporter.ComicAppBackupDto
import com.aschlus.comicreadingcompanion.data.exporter.readingListExportFileName
import com.aschlus.comicreadingcompanion.ui.component.ComicBottomNavigation
import com.aschlus.comicreadingcompanion.ui.screen.BrowseScreen
import com.aschlus.comicreadingcompanion.ui.screen.CollectionScreen
import com.aschlus.comicreadingcompanion.ui.screen.ComicVineVolumeReviewScreen
import com.aschlus.comicreadingcompanion.ui.screen.ComicVineVolumeSearchScreen
import com.aschlus.comicreadingcompanion.ui.screen.CreateReadingListScreen
import com.aschlus.comicreadingcompanion.ui.screen.EditSeriesScreen
import com.aschlus.comicreadingcompanion.ui.screen.HomeScreen
import com.aschlus.comicreadingcompanion.ui.screen.IssueDetailScreen
import com.aschlus.comicreadingcompanion.ui.screen.LibraryScreen
import com.aschlus.comicreadingcompanion.ui.screen.PublisherDetailScreen
import com.aschlus.comicreadingcompanion.ui.screen.ReadingHistoryScreen
import com.aschlus.comicreadingcompanion.ui.screen.ReadingListDetailScreen
import com.aschlus.comicreadingcompanion.ui.screen.SeriesDetailScreen
import com.aschlus.comicreadingcompanion.ui.screen.SettingsScreen
import com.aschlus.comicreadingcompanion.ui.theme.ComicReadingCompanionTheme
import com.aschlus.comicreadingcompanion.ui.viewmodel.AddIssueToReadingListViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.AddIssueToReadingListViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.BrowseViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.BrowseViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.CollectionViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.CollectionViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.ComicVineSeriesRefreshViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.ComicVineSeriesRefreshViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.ComicVineSettingsViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.ComicVineSettingsViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.ComicVineVolumeReviewViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.ComicVineVolumeReviewViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.ComicVineVolumeSearchViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.ComicVineVolumeSearchViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.CreateReadingListViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.CreateReadingListViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.EditSeriesViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.EditSeriesViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.HomeViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.HomeViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.ImportReadingListViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.ImportReadingListViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.IssueDetailViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.IssueDetailViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.MetronSettingsViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.MetronSettingsViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.PublisherDetailViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.PublisherDetailViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.ReadingListDetailViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.ReadingListDetailViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.SeriesDetailViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.SeriesDetailViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var backupValidationError by mutableStateOf<String?>(null)

    private var pendingRestoreJson by mutableStateOf<String?>(null)

    private var pendingRestoreFileName by mutableStateOf<String?>(null)

    private var pendingRestoreBackup by mutableStateOf<ComicAppBackupDto?>(null)

    private var restoreErrorMessage by mutableStateOf<String?>(null)

    private val homeViewModel: HomeViewModel by viewModels {

        val container =
            (application as ComicReadingCompanionApplication)
                .container

        HomeViewModelFactory(
            repository = container.comicRepository,
            homeUiPreferences = container.homeUiPreferences
        )
    }

    private val importReadingListViewModel:
            ImportReadingListViewModel by viewModels {
                val container =
                    (application as ComicReadingCompanionApplication)
                        .container

        ImportReadingListViewModelFactory(
            parser = container.readingListAssetParser,
            importer = container.readingListImporter
        )
    }

    private val importReadingListLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri == null) {
                return@registerForActivityResult
            }

            lifecycleScope.launch {
                try {
                    val fileName =
                        getDisplayName(uri)
                            ?: "selected file"

                    val jsonText =
                        withContext(
                            Dispatchers.IO
                        ) {
                            contentResolver
                                .openInputStream(uri)
                                ?.bufferedReader()
                                ?.use { reader ->
                                    reader.readText()
                                }
                                ?: throw IllegalArgumentException(
                                    "Could not read selected file '$fileName"
                                )
                        }

                    importReadingListViewModel
                        .importReadingList(
                            jsonText = jsonText,
                            sourceDescription = "reading-list file '$fileName'"
                        )
                } catch (
                    exception: Exception
                ) {
                    importReadingListViewModel
                        .reportError(
                            exception.message
                                ?: "Could not read selected reading-list file"
                        )
                }
            }
        }

    private var pendingExportReadingListId:
            Long? = null

    private val exportReadingListLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument(
                "application/json"
            )
        ) { uri ->
            val readingListId =
                pendingExportReadingListId

            pendingExportReadingListId = null

            if (uri == null || readingListId == null) {
                return@registerForActivityResult
            }

            lifecycleScope.launch {
                try {
                    val exporter =
                        (application as ComicReadingCompanionApplication)
                            .container
                            .readingListExporter

                    val jsonText =
                        withContext(
                            Dispatchers.IO
                        ) {
                            exporter.exportJson(
                                readingListId = readingListId
                            )
                        }

                    withContext(
                        Dispatchers.IO
                    ) {
                        contentResolver
                            .openOutputStream(uri, "wt")
                            ?.bufferedWriter()
                            ?.use { writer ->
                                writer.write(jsonText)
                            }
                            ?: throw IllegalArgumentException(
                                "Could not write exported file"
                            )
                    }

                    Toast.makeText(
                        this@MainActivity,
                        "Reading list exported",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (
                    exception : Exception
                ) {
                    Toast.makeText(
                        this@MainActivity,
                        exception.message
                            ?: "Could not export reading list",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    private val exportBackupLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument(
                "application/json"
            )
        ) { uri ->
            if (uri == null) {
                return@registerForActivityResult
            }

            lifecycleScope.launch {
                try {
                    val exporter =
                        (application as ComicReadingCompanionApplication)
                            .container
                            .comicAppBackupExporter

                    val jsonText =
                        withContext(Dispatchers.IO) {
                            exporter.exportJson()
                        }

                    withContext(Dispatchers.IO) {
                        contentResolver
                            .openOutputStream(uri, "wt")
                            ?.bufferedWriter()
                            ?.use { writer ->
                                writer.write(jsonText)
                            }
                            ?: throw IllegalArgumentException(
                                "Could not write backup file."
                            )
                    }

                    Toast.makeText(
                        this@MainActivity,
                        "Full backup exported",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (exception: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        exception.message
                            ?: "Could not export full backup.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    private val validateBackupLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri == null) {
                return@registerForActivityResult
            }

            lifecycleScope.launch {
                try {
                    val fileName =
                        getDisplayName(uri) ?: "selected file"

                    val parser =
                        (application as ComicReadingCompanionApplication)
                            .container
                            .comicAppBackupParser

                    val backup = withContext(Dispatchers.IO) {
                        val jsonText =
                            contentResolver
                                .openInputStream(uri)
                                ?.bufferedReader()
                                ?.use { reader ->
                                    reader.readText()
                                }
                                ?: throw IllegalArgumentException(
                                    "Could not read backup file '$fileName'."
                                )

                        parser.parseJson(
                            jsonText = jsonText,
                            sourceDescription = "backup file '$fileName'"
                        )
                    }

                    Toast.makeText(
                        this@MainActivity,
                        "Backup validated: ${backup.series.size} series, " +
                        "${backup.issues.size} issues, " +
                        "${backup.readingLists.size} lists. " +
                        "No data changed.",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (exception: Exception) {
                    backupValidationError =
                        exception.message
                            ?: "Could not validate backup file."
                }
            }
        }

    private val restoreBackupLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri == null) {
                return@registerForActivityResult
            }

            lifecycleScope.launch {
                try {
                    val fileName =
                        getDisplayName(uri) ?: "selected file"

                    val container =
                        (application as ComicReadingCompanionApplication)
                            .container

                    val parsed = withContext(Dispatchers.IO) {
                        val jsonText =
                            contentResolver
                                .openInputStream(uri)
                                ?.bufferedReader()
                                ?.use { reader ->
                                    reader.readText()
                                }
                                ?: throw IllegalArgumentException(
                                    "Could not read backup file '$fileName'"
                                )

                        val backup =
                            container.comicAppBackupParser.parseJson(
                                jsonText = jsonText,
                                sourceDescription = "backup file '$fileName'"
                            )

                        jsonText to backup
                    }

                    pendingRestoreJson = parsed.first
                    pendingRestoreFileName = fileName
                    pendingRestoreBackup = parsed.second
                } catch (exception: Exception) {
                    backupValidationError =
                        exception.message
                            ?: "Could not validate backup file"
                }
            }
        }

    private fun getDisplayName(
        uri: Uri
    ): String? {
        contentResolver
            .query(
                uri,
                arrayOf(
                    OpenableColumns.DISPLAY_NAME
                ),
                null,
                null,
                null
            )
            ?.use { cursor ->
                val columnIndex =
                    cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                if (columnIndex >= 0 && cursor.moveToFirst()) {
                    return cursor.getString(columnIndex)
                }
            }

        return uri.lastPathSegment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            ComicReadingCompanionTheme {

                val navController = rememberNavController()

                val safeNavigateBack: () -> Unit = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }

                val navBackStackEntry by navController
                    .currentBackStackEntryAsState()

                val currentRoute =
                    navBackStackEntry
                        ?.destination
                        ?.route

                val topLevelRoutes =
                    setOf(
                        "home",
                        "browse",
                        "library",
                        "collection"
                    )

                val navigateTopLevel:
                        (String) -> Unit = { route ->

                            navController.navigate(route) {
                                popUpTo("home") {
                                    saveState = true
                                }

                                launchSingleTop = true
                                restoreState = true
                            }
                }

                Scaffold(
                    contentWindowInsets =
                        WindowInsets(
                            left = 0,
                            top = 0,
                            right = 0,
                            bottom = 0
                        ),
                    bottomBar = {
                        if (currentRoute in topLevelRoutes) {
                            ComicBottomNavigation(
                                currentRoute = currentRoute,
                                onHomeClick = {
                                    navigateTopLevel("home")
                                },
                                onBrowseClick = {
                                    navigateTopLevel("browse")
                                },
                                onLibraryClick = {
                                    navigateTopLevel("library")
                                },
                                onCollectionClick = {
                                    navigateTopLevel("collection")
                                }
                            )
                        }
                    }
                ) { appPadding ->

                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(appPadding),
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None },
                        predictivePopEnterTransition = { EnterTransition.None },
                        predictivePopExitTransition = { ExitTransition.None },
                        sizeTransform = { null }
                    ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onBrowseClick = {
                                    navigateTopLevel("browse")
                                },
                                onLibraryClick = {
                                    navigateTopLevel("library")
                                },
                                onCreateReadingListClick = {
                                    navController.navigate(
                                        "createReadingList"
                                    )
                                },
                                onReadingListClick = { readingListId, startPosition ->
                                    navController.navigate(
                                        "readingList/$readingListId?startPosition=$startPosition"
                                    )
                                },
                                onIssueClick = { issueId ->
                                    navController.navigate(
                                        "issue/$issueId"
                                    )
                                },
                                onReadingHistoryClick = {
                                    navController.navigate(
                                        "readingHistory"
                                    )
                                },
                                onSettingsClick = {
                                    navController.navigate("settings")
                                }
                            )
                        }

                        composable("settings") {
                            val container =
                                (application as ComicReadingCompanionApplication).container

                            val settingsViewModel: ComicVineSettingsViewModel =
                                viewModel(
                                    factory = ComicVineSettingsViewModelFactory(
                                        apiKeyStore = container.comicVineApiKeyStore,
                                        apiClient = container.comicVineApiClient
                                    )
                                )

                            val metronSettingsViewModel: MetronSettingsViewModel =
                                viewModel(
                                    factory = MetronSettingsViewModelFactory(
                                        tokenStore = container.metronApiTokenStore,
                                        apiClient = container.metronApiClient
                                    )
                                )

                            SettingsScreen(
                                viewModel = settingsViewModel,
                                metronViewModel = metronSettingsViewModel,
                                onBackClick = safeNavigateBack,
                                onSearchSeriesClick = {
                                    navController.navigate("comicVineVolumeSearch")
                                },
                                onExportBackupClick = {
                                    exportBackupLauncher.launch(
                                        "comic-reading-companion-backup.json"
                                    )
                                },
                                onValidateBackupClick = {
                                    validateBackupLauncher.launch(
                                        arrayOf("application/json", "text/plain")
                                    )
                                },
                                onRestoreBackupClick = {
                                    restoreBackupLauncher.launch(
                                        arrayOf("application/json", "text/plain")
                                    )
                                }
                            )
                        }

                        composable("comicVineVolumeSearch") {
                            val container =
                                (application as ComicReadingCompanionApplication).container

                            val searchViewModel: ComicVineVolumeSearchViewModel =
                                viewModel(
                                    factory = ComicVineVolumeSearchViewModelFactory(
                                        apiKeyStore = container.comicVineApiKeyStore,
                                        apiClient = container.comicVineApiClient
                                    )
                                )

                            ComicVineVolumeSearchScreen(
                                viewModel = searchViewModel,
                                onBackClick = safeNavigateBack,
                                onVolumeClick = { volumeId ->
                                    navController.navigate(
                                        "comicVineVolumeReview/$volumeId"
                                    )
                                }
                            )
                        }

                        composable(
                            route = "comicVineVolumeReview/{volumeId}",
                            arguments = listOf(
                                navArgument("volumeId") {
                                    type = NavType.LongType
                                }
                            )
                        ) { backStackEntry ->
                            val volumeId =
                                backStackEntry.arguments?.getLong("volumeId") ?: 0L

                            val container =
                                (application as ComicReadingCompanionApplication)
                                    .container

                            val searchBackStackEntry = remember(backStackEntry) {
                                navController.getBackStackEntry("comicVineVolumeSearch")
                            }

                            val searchViewModel: ComicVineVolumeSearchViewModel =
                                viewModel(
                                    viewModelStoreOwner = searchBackStackEntry,
                                    factory = ComicVineVolumeSearchViewModelFactory(
                                        apiKeyStore = container.comicVineApiKeyStore,
                                        apiClient = container.comicVineApiClient
                                    )
                                )

                            val searchUiState by searchViewModel.uiState.collectAsState()

                            val selectedVolume = searchUiState.results
                                .firstOrNull { it.id == volumeId }

                            val reviewViewModel: ComicVineVolumeReviewViewModel =
                                viewModel(
                                    factory = ComicVineVolumeReviewViewModelFactory(
                                        apiKeyStore = container.comicVineApiKeyStore,
                                        apiClient = container.comicVineApiClient,
                                        comicRepository = container.comicRepository,
                                        universeFlow = container.comicRepository.getAllUniverses(),
                                        comicCatalogImporter = container.comicCatalogImporter
                                    )
                                )

                            LaunchedEffect(volumeId, selectedVolume) {
                                reviewViewModel.load(
                                    volumeId = volumeId,
                                    selectedVolume = selectedVolume
                                )
                            }

                            ComicVineVolumeReviewScreen(
                                viewModel = reviewViewModel,
                                onBackClick = safeNavigateBack
                            )
                        }

                        composable("createReadingList") {
                            val createReadingListViewModel: CreateReadingListViewModel =
                                viewModel(
                                    factory =
                                        CreateReadingListViewModelFactory(
                                            (application as ComicReadingCompanionApplication)
                                                .container
                                                .comicRepository
                                        )
                                )
                            CreateReadingListScreen(
                                viewModel = createReadingListViewModel,
                                onBackClick = safeNavigateBack,
                                onReadingListCreated = { readingListId ->
                                    navController.navigate(
                                        "readingList/$readingListId?startPosition=-1"
                                    ) {
                                        popUpTo("createReadingList") {
                                            inclusive = true
                                        }
                                    }
                                }
                            )
                        }

                        composable("browse") {
                            val browseViewModel:
                                    BrowseViewModel = viewModel(
                                factory =
                                    BrowseViewModelFactory(
                                        (application as ComicReadingCompanionApplication)
                                            .container
                                            .comicRepository
                                    )
                            )

                            BrowseScreen(
                                viewModel = browseViewModel,
                                onPublisherClick = { publisherId ->
                                    navController.navigate(
                                        "publisher/$publisherId"
                                    )
                                },
                                onSeriesClick = { seriesId ->
                                    navController.navigate(
                                        "series/$seriesId"
                                    )
                                },
                                onIssueClick = { issueId ->
                                    navController.navigate(
                                        "issue/$issueId"
                                    )
                                }
                            )
                        }

                        composable("library") {
                            val importState by
                                    importReadingListViewModel
                                        .state
                                        .collectAsState()

                            LibraryScreen(
                                viewModel = homeViewModel,
                                onCreateReadingListClick = {
                                    navController.navigate(
                                        "createReadingList"
                                    )
                                },
                                onImportReadingListClick = {
                                    importReadingListLauncher
                                        .launch(
                                            arrayOf(
                                                "application/json",
                                                "text/plain"
                                            )
                                        )
                                },
                                importState = importState,
                                onImportResultConsumed = {
                                    importReadingListViewModel
                                        .clearResult()
                                },
                                onReadingListClick = {
                                    readingListId, startPosition ->

                                    navController.navigate(
                                        "readingList/$readingListId?startPosition=$startPosition"
                                    )
                                }
                            )
                        }

                        composable("collection") {
                            val collectionViewModel: CollectionViewModel =
                                viewModel(
                                    factory = CollectionViewModelFactory(
                                        (application as ComicReadingCompanionApplication)
                                            .container
                                            .comicRepository
                                    )
                                )

                            CollectionScreen(
                                viewModel = collectionViewModel,
                                onIssueClick = { issueId ->
                                    navController.navigate(
                                        "issue/$issueId"
                                    )
                                }
                            )
                        }

                        composable("readingHistory") {
                            ReadingHistoryScreen(
                                viewModel = homeViewModel,
                                onIssueClick = { issueId ->
                                    navController.navigate(
                                        "issue/$issueId"
                                    )
                                },
                                onBackClick = safeNavigateBack
                            )
                        }

                        composable(
                            route = "readingList/{readingListId}?startPosition={startPosition}",
                            arguments = listOf(
                                navArgument("readingListId") {
                                    type = NavType.LongType
                                },
                                navArgument("startPosition") {
                                    type = NavType.IntType
                                    defaultValue = -1
                                }
                            )
                        ) { backStackEntry ->

                            val readingListId =
                                backStackEntry.arguments
                                    ?.getLong("readingListId")
                                    ?: return@composable

                            val startPosition =
                                backStackEntry.arguments
                                    ?.getInt("startPosition")
                                    ?: -1

                            LaunchedEffect(readingListId) {
                                homeViewModel.recordReadingListOpened((readingListId))
                            }

                            val detailViewModel:
                                    ReadingListDetailViewModel = viewModel(
                                factory =
                                    ReadingListDetailViewModelFactory(
                                        repository =
                                            (application as ComicReadingCompanionApplication)
                                                .container
                                                .comicRepository,
                                        readingListUiPreferences =
                                            (application as ComicReadingCompanionApplication)
                                                .container
                                                .readingListUiPreferences
                                    )
                            )

                            ReadingListDetailScreen(
                                readingListId = readingListId,
                                startPosition = startPosition,
                                viewModel = detailViewModel,
                                onIssueClick = { issueId ->
                                    navController.navigate(
                                        "issue/$issueId"
                                    )
                                },
                                onBackClick = safeNavigateBack,
                                onExportReadingListClick = { id, title ->
                                    pendingExportReadingListId = id

                                    exportReadingListLauncher.launch(
                                        readingListExportFileName(title)
                                    )
                                },
                                onReadingListDuplicated = { duplicatedReadingListId ->
                                    navController.navigate(
                                        "readingList/$duplicatedReadingListId?startPosition=-1"
                                    )
                                }
                            )
                        }

                        composable(
                            route = "publisher/{publisherId}",
                            arguments = listOf(
                                navArgument("publisherId") {
                                    type = NavType.LongType
                                }
                            )
                        ) { backStackEntry ->

                            val publisherId =
                                backStackEntry.arguments
                                    ?.getLong("publisherId")
                                    ?: return@composable

                            val publisherDetailViewModel:
                                    PublisherDetailViewModel = viewModel(
                                factory =
                                    PublisherDetailViewModelFactory(
                                        (application as ComicReadingCompanionApplication)
                                            .container
                                            .comicRepository
                                    )
                            )

                            PublisherDetailScreen(
                                publisherId = publisherId,
                                viewModel = publisherDetailViewModel,
                                onSeriesClick = { seriesId ->
                                    navController.navigate(
                                        "series/$seriesId"
                                    )
                                },
                                onBackClick = safeNavigateBack
                            )
                        }

                        composable(
                            route = "series/{seriesId}",
                            arguments = listOf(
                                navArgument("seriesId") {
                                    type = NavType.LongType
                                }
                            )
                        ) { backStackEntry ->

                            val seriesId =
                                backStackEntry.arguments
                                    ?.getLong("seriesId")
                                    ?: return@composable

                            val container =
                                (application as ComicReadingCompanionApplication)
                                    .container

                            val seriesDetailViewModel: SeriesDetailViewModel =
                                viewModel(
                                    factory = SeriesDetailViewModelFactory(
                                        repository = container.comicRepository
                                    )
                                )

                            val refreshViewModel: ComicVineSeriesRefreshViewModel =
                                viewModel(
                                    factory = ComicVineSeriesRefreshViewModelFactory(
                                        apiKeyStore = container.comicVineApiKeyStore,
                                        apiClient = container.comicVineApiClient,
                                        repository = container.comicRepository
                                    )
                                )

                            SeriesDetailScreen(
                                seriesId = seriesId,
                                viewModel = seriesDetailViewModel,
                                onPublisherClick = { publisherId ->
                                    navController.navigate(
                                        "publisher/$publisherId"
                                    )
                                },
                                onIssueClick = { issueId ->
                                    navController.navigate(
                                        "issue/$issueId"
                                    )
                                },
                                onBackClick = safeNavigateBack,
                                onEditClick = {
                                    navController.navigate("editSeries/$seriesId")
                                },
                                refreshViewModel = refreshViewModel
                            )
                        }

                        composable(
                            route = "editSeries/{seriesId}",
                            arguments = listOf(
                                navArgument("seriesId") {
                                    type = NavType.LongType
                                }
                            )
                        ) { backStackEntry ->

                            val seriesId =
                                backStackEntry.arguments
                                    ?.getLong("seriesId")
                                    ?: return@composable

                            val container =
                                (application as ComicReadingCompanionApplication)
                                    .container

                            val editSeriesViewModel: EditSeriesViewModel =
                                viewModel(
                                    factory = EditSeriesViewModelFactory(
                                        repository = container.comicRepository
                                    )
                                )

                            EditSeriesScreen(
                                seriesId = seriesId,
                                viewModel = editSeriesViewModel,
                                onBackClick = safeNavigateBack
                            )
                        }

                        composable(
                            route = "issue/{issueId}",
                            arguments = listOf(
                                navArgument("issueId") {
                                    type = NavType.LongType
                                }
                            )
                        ) { backStackEntry ->

                            val issueId =
                                backStackEntry.arguments
                                    ?.getLong("issueId")
                                    ?: return@composable

                            val issueDetailViewModel:
                                    IssueDetailViewModel = viewModel(
                                factory =
                                    IssueDetailViewModelFactory(
                                        (application as ComicReadingCompanionApplication)
                                            .container
                                            .comicRepository
                                    )
                            )

                            val addIssueToReadingListViewModel:
                                    AddIssueToReadingListViewModel =
                                viewModel(
                                    factory =
                                        AddIssueToReadingListViewModelFactory(
                                            issueId = issueId,
                                            repository =
                                                (application as ComicReadingCompanionApplication)
                                                    .container
                                                    .comicRepository
                                        )
                                )

                            IssueDetailScreen(
                                issueId = issueId,
                                viewModel = issueDetailViewModel,
                                addToReadingListViewModel =
                                    addIssueToReadingListViewModel,
                                onSeriesClick = { seriesId ->
                                    navController.navigate(
                                        "series/$seriesId"
                                    )
                                },
                                onReadingListClick = { readingListId, position ->
                                    navController.navigate(
                                        "readingList/$readingListId?startPosition=$position"
                                    )
                                },
                                onBackClick = safeNavigateBack
                            )
                        }
                    }
                }

                pendingRestoreBackup?.let { backup ->
                    AlertDialog(
                        onDismissRequest = {
                            pendingRestoreJson = null
                            pendingRestoreFileName = null
                            pendingRestoreBackup = null
                        },
                        title = {
                            Text("Replace current data?")
                        },
                        text = {
                            Text(
                                text = buildString {
                                    appendLine(
                                        "File: ${pendingRestoreFileName ?: "selected file"}"
                                    )
                                    appendLine()
                                    appendLine("This backup contains:")
                                    appendLine("${backup.publishers.size} publishers")
                                    appendLine("${backup.universes.size} universes")
                                    appendLine("${backup.series.size} series")
                                    appendLine("${backup.issues.size} issues")
                                    appendLine("${backup.readingLists.size} reading lists")
                                    appendLine("${backup.readingProgress.size} reading-progress records")
                                    appendLine("${backup.issueOwnership.size} ownership records")
                                    appendLine()
                                    append(
                                        "Restoring will replace your current database " +
                                        "contents. Changes made since this backup " +
                                        "will be lost. This cannot be undone."
                                    )
                                },
                                modifier = Modifier
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val jsonText = pendingRestoreJson
                                    val fileName = pendingRestoreFileName ?: "selected file"

                                    pendingRestoreJson = null
                                    pendingRestoreFileName = null
                                    pendingRestoreBackup = null

                                    if (jsonText != null) {
                                        lifecycleScope.launch {
                                            try {
                                                val restorer =
                                                    (application as ComicReadingCompanionApplication)
                                                        .container
                                                        .comicAppBackupRestorer

                                                val restored =
                                                    withContext(Dispatchers.IO) {
                                                        restorer.restoreJson(
                                                            jsonText = jsonText,
                                                            sourceDescription = "backup file '$fileName'"
                                                        )
                                                    }

                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Backup restored: " +
                                                    "${restored.series.size} series, " +
                                                    "${restored.issues.size} issues, " +
                                                    "${restored.readingLists.size} lists.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } catch (exception: Exception) {
                                                restoreErrorMessage =
                                                    exception.message
                                                        ?: "Could not restore backup."
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text("REPLACE DATA")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    pendingRestoreJson = null
                                    pendingRestoreFileName = null
                                    pendingRestoreBackup = null
                                }
                            ) {
                                Text("CANCEL")
                            }
                        }
                    )
                }

                restoreErrorMessage?.let { errorMessage ->
                    AlertDialog(
                        onDismissRequest = {
                            restoreErrorMessage = null
                        },
                        title = {
                            Text("Restore failed")
                        },
                        text = {
                            Text(
                                text = errorMessage,
                                modifier = Modifier
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    restoreErrorMessage = null
                                }
                            ) {
                                Text("CLOSE")
                            }
                        }
                    )
                }

                backupValidationError?.let { errorMessage ->
                    AlertDialog(
                        onDismissRequest = {
                            backupValidationError = null
                        },
                        title = {
                            Text("Backup validation failed")
                        },
                        text = {
                            Text(
                                text = errorMessage,
                                modifier = Modifier
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    backupValidationError = null
                                }
                            ) {
                                Text("Close")
                            }
                        }
                    )
                }
            }
        }
    }
}