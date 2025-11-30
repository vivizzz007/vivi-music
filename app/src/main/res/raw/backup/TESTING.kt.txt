package raw.backup

fun SettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    var latestVersion by remember { mutableStateOf("") }
    var isUpdateAvailable by remember { mutableStateOf(false) }
    var showUpdateCard by remember { mutableStateOf(false) }
    var userImageUri by remember { mutableStateOf(loadImageUri(context)) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        userImageUri = uri
        saveImageUri(context, uri)
    }

    val greeting = getGreetingBasedOnTime()

    LaunchedEffect(Unit) {
        checkForUpdateFromGitHubAll { highestVersion ->
            val cleanLatestVersion = highestVersion.removePrefix("v")
            val newVersionAvailable = isNewerVersion(cleanLatestVersion, BuildConfig.VERSION_NAME)
            latestVersion = cleanLatestVersion
            isUpdateAvailable = newVersionAvailable && cleanLatestVersion != BuildConfig.VERSION_NAME
            showUpdateCard = isUpdateAvailable
        }
    }

    data class SettingItem(
        val title: String,
        val iconRes: Int,
        val route: String,
        val description: String = "",
        val keywords: List<String> = emptyList()
    )

    val settingsItems = listOf(
        SettingItem(
            title = stringResource(R.string.update),
            iconRes = if (isUpdateAvailable) R.drawable.updateon_icon else R.drawable.update_icon,
            route = "settings/update",
            description = if (isUpdateAvailable) "Update available" else "Check for app updates",
            keywords = listOf("update", "version", "upgrade", "new")
        ),
        SettingItem(
            title = stringResource(R.string.appearance),
            iconRes = R.drawable.theme_icon,
            route = "settings/appearance",
            description = "Customize theme and colors",
            keywords = listOf("theme", "color", "dark mode", "light mode")
        ),
        SettingItem(
            title = stringResource(R.string.content),
            iconRes = R.drawable.content_icon,
            route = "settings/content",
            description = "Content preferences and settings",
            keywords = listOf("content", "hide explicit", "notification")
        ),
        SettingItem(
            title = stringResource(R.string.player_and_audio),
            iconRes = R.drawable.play_icon,
            route = "settings/player",
            description = "Player and audio settings",
            keywords = listOf("player", "audio", "sound", "music")
        ),
        SettingItem(
            title = stringResource(R.string.storage),
            iconRes = R.drawable.storage_icon,
            route = "settings/storage",
            description = "Manage storage and cache",
            keywords = listOf("storage", "cache", "memory")
        ),
        SettingItem(
            title = stringResource(R.string.privacy),
            iconRes = R.drawable.security_icon,
            route = "settings/privacy",
            description = "Privacy and security settings",
            keywords = listOf("privacy", "security", "permissions")
        ),
        SettingItem(
            title = stringResource(R.string.backup_restore),
            iconRes = R.drawable.backups_icon,
            route = "settings/backup_restore",
            description = "Backup and restore your data",
            keywords = listOf("backup", "restore", "data")
        ),
        SettingItem(
            title = stringResource(R.string.about),
            iconRes = R.drawable.info_icon,
            route = "settings/about",
            description = "About this app and version info",
            keywords = listOf("about", "version", "details")
        )
    )

    fun matchesQuery(item: SettingItem, query: String): Boolean {
        if (query.isBlank()) return true
        val tokens = query.trim().lowercase().split("\\s+".toRegex())
        return tokens.all { token ->
            item.title.lowercase().contains(token) ||
                    item.description.lowercase().contains(token) ||
                    item.keywords.any { it.lowercase().contains(token) }
        }
    }

    val filteredItems = settingsItems.filter { item ->
        matchesQuery(item, searchQuery)
    }

    @Composable
    fun SearchDialog(
        showDialog: Boolean,
        onDismiss: () -> Unit,
        searchQuery: String,
        onSearchQueryChange: (String) -> Unit,
        filteredItems: List<SettingItem>,
        onItemClick: (String) -> Unit
    ) {
        if (showDialog) {
            val focusRequester = remember { FocusRequester() }

            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                BackHandler(enabled = true) {
                    onDismiss()
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    painterResource(R.drawable.arrow_back),
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .focusRequester(focusRequester),
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                "Search settings...",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            AnimatedVisibility(
                                visible = searchQuery.isNotEmpty(),
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        painterResource(R.drawable.search_off),
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Divider()

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            if (filteredItems.isEmpty() && searchQuery.isNotEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "No results found",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                items(filteredItems) { item ->
                                    ListItem(
                                        headlineContent = { Text(item.title) },
                                        supportingContent = item.description.takeIf { it.isNotEmpty() }?.let {
                                            { Text(it) }
                                        },
                                        leadingContent = {
                                            Icon(
                                                painterResource(item.iconRes),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        modifier = Modifier.clickable {
                                            onItemClick(item.route)
                                            onDismiss()
                                        }
                                    )
                                    Divider()
                                }
                            }
                        }
                    }
                }
            }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // User Profile Card with Greeting
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(120.dp),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .clickable {
                                imagePickerLauncher.launch("image/*")
                            }
                    ) {
                        if (userImageUri != null) {
                            AsyncImage(
                                model = userImageUri,
                                contentDescription = "User profile image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.vivimusic),
                                contentDescription = "Default profile image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        AnimatedVisibility(
                            visible = !showUpdateCard,
                            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                                animationSpec = tween(500), initialOffsetY = { it / 2 }
                            )
                        ) {
                            Text(
                                text = "👋 Welcome to Settings",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 18.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = showUpdateCard,
                            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                                animationSpec = tween(500), initialOffsetY = { it / 2 }
                            )
                        ) {
                            Text(
                                text = "🚀 Update available: v$latestVersion",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 18.sp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable(enabled = showUpdateCard) {
                                        if (showUpdateCard) navController.navigate("settings/update")
                                    }
                            )
                        }
                    }
                }
            }

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    placeholder = {
                        Text(
                            "Search settings...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painterResource(R.drawable.search_icon),
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSearchDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    enabled = false,
                    readOnly = true
                )
            }

            // Settings List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                settingsItems.forEach { item ->
                    item {
                        Column {
                            ListItem(
                                headlineContent = { Text(item.title) },
                                leadingContent = {
                                    Icon(
                                        painterResource(item.iconRes),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingContent = {
                                    Icon(
                                        painterResource(R.drawable.arrow_forward),
                                        contentDescription = "Navigate",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                modifier = Modifier
                                    .clickable { navController.navigate(item.route) }
                                    .padding(vertical = 4.dp)
                            )
                            Divider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }

    SearchDialog(
        showDialog = showSearchDialog,
        onDismiss = {
            showSearchDialog = false
            searchQuery = ""
        },
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        filteredItems = filteredItems,
        onItemClick = { route ->
            navController.navigate(route)
        }
    )
}

@Composable
fun getGreetingBasedOnTime(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 5..11 -> "Good Morning 🌅"
        hour in 12..17 -> "Good Afternoon 🌞"
        hour in 18..20 -> "Good Evening 🌆"
        else -> "Good Night 🌙"
    }
}

suspend fun checkForUpdateFromGitHubAll(onResult: (String) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/vivizzz007/vivi-music/releases")
            val connection = url.openConnection().apply {
                setRequestProperty("User-Agent", "ViviMusicApp")
            }
            connection.connect()
            val json = connection.getInputStream().bufferedReader().use { it.readText() }
            val releases = JSONArray(json)
            var highestVersion = BuildConfig.VERSION_NAME
            for (i in 0 until releases.length()) {
                val tag = releases.getJSONObject(i).getString("tag_name")
                if (tag.startsWith("v")) {
                    val clean = tag.removePrefix("v")
                    if (isNewerVersion(clean, highestVersion)) {
                        highestVersion = clean
                    }
                }
            }
            onResult("v$highestVersion")
        } catch (e: Exception) {
            e.printStackTrace()
            onResult("v${BuildConfig.VERSION_NAME}")
        }
    }
}

fun saveImageUri(context: Context, uri: Uri?) {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putString("user_image_uri", uri?.toString())
    editor.apply()
}

fun loadImageUri(context: Context): Uri? {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val uriString = sharedPreferences.getString("user_image_uri", null)
    return uriString?.let { Uri.parse(it) }
}

fun isNewerVersion(version1: String, version2: String): Boolean {
    val v1 = version1.split(".").map { it.toInt() }
    val v2 = version2.split(".").map { it.toInt() }
    
    for (i in 0 until maxOf(v1.size, v2.size)) {
        val num1 = v1.getOrElse(i) { 0 }
        val num2 = v2.getOrElse(i) { 0 }
        
        if (num1 > num2) return true
        if (num1 < num2) return false
    }
    
    return false
}