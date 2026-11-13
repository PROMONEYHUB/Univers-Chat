package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.UiState
import com.example.data.repository.MessagingRepository
import com.example.notifications.InAppMessageBanner
import com.example.notifications.MessageNotificationCoordinator
import com.example.notifications.NotificationHelper
import com.example.notifications.RealtimeMessagingService
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOut
import com.example.ui.components.IosInAppNotificationBanner
import com.example.ui.screens.ChatRoomScreen
import com.example.ui.screens.ChatsScreen
import com.example.ui.screens.PeopleScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.PulseChatTheme
import com.example.ui.viewmodel.ChatRoomViewModel
import com.example.ui.viewmodel.MainMessagingViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

object Routes {
    const val CHATS = "chats"
    const val PEOPLE = "people"
    const val PROFILE = "profile"
    const val CHAT_ROOM = "chat_room/{conversationId}"

    fun chatRoom(conversationId: String): String = "chat_room/$conversationId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannels(this)
        handleNotificationDeepLink(intent)
        setContent {
            PulseChatTheme {
                PulseChatApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationDeepLink(intent)
    }

    private fun handleNotificationDeepLink(intent: Intent?) {
        val convId = intent?.getStringExtra(NotificationHelper.EXTRA_CONVERSATION_ID)
        if (!convId.isNullOrBlank()) {
            MessageNotificationCoordinator.emitDeepLinkConversation(convId)
        }
    }
}

@Composable
fun PulseChatApp() {
    val context = LocalContext.current
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        LaunchedEffect(Unit) {
            MessageNotificationCoordinator.stopObserving()
            RealtimeMessagingService.syncServiceState(context, shouldRun = false)
        }
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedShell(currentUserId = user.uid)
        }
    }
}

@Composable
private fun AuthenticatedShell(currentUserId: String) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val mainViewModel: MainMessagingViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                MainMessagingViewModel(MessagingRepository(db), currentUserId)
            }
        }
    )

    val currentUserProfile by mainViewModel.currentUserProfile.collectAsStateWithLifecycle()
    val privateUser by mainViewModel.privateUser.collectAsStateWithLifecycle()
    val conversationsState by mainViewModel.conversationsState.collectAsStateWithLifecycle()
    val peopleState by mainViewModel.peopleState.collectAsStateWithLifecycle()
    val searchQuery by mainViewModel.searchQuery.collectAsStateWithLifecycle()
    val chatFilter by mainViewModel.chatFilter.collectAsStateWithLifecycle()
    val peopleSearchQuery by mainViewModel.peopleSearchQuery.collectAsStateWithLifecycle()
    val actionMessage by mainViewModel.actionMessage.collectAsStateWithLifecycle()
    val isBusy by mainViewModel.isBusy.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val notificationsEnabled = privateUser?.notificationsEnabled ?: true
    var hasNotificationPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }
    val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted || NotificationHelper.hasNotificationPermission(context)
    }
    var requestedNotificationOnce by rememberSaveable(currentUserId) { mutableStateOf(false) }

    LaunchedEffect(currentUserId) {
        NotificationHelper.ensureChannels(context)
        MessageNotificationCoordinator.startObservingForUser(context, currentUserId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission &&
            !requestedNotificationOnce
        ) {
            requestedNotificationOnce = true
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(currentUserId, notificationsEnabled, hasNotificationPermission) {
        RealtimeMessagingService.syncServiceState(
            context = context,
            shouldRun = notificationsEnabled && hasNotificationPermission
        )
    }

    DisposableEffect(lifecycleOwner, mainViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    mainViewModel.setOnlineStatus(true)
                    MessageNotificationCoordinator.setAppForegroundState(true)
                    hasNotificationPermission = NotificationHelper.hasNotificationPermission(context)
                }
                Lifecycle.Event.ON_STOP -> {
                    mainViewModel.setOnlineStatus(false)
                    MessageNotificationCoordinator.setAppForegroundState(false)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            mainViewModel.clearActionMessage()
        }
    }

    var activeBanner by remember { mutableStateOf<InAppMessageBanner?>(null) }
    LaunchedEffect(currentUserId) {
        MessageNotificationCoordinator.inAppBanners.collect { banner ->
            activeBanner = banner
        }
    }
    LaunchedEffect(activeBanner) {
        if (activeBanner != null) {
            delay(4500L)
            activeBanner = null
        }
    }

    val totalUnread = remember(conversationsState, currentUserId) {
        val list = (conversationsState as? UiState.Success)?.data.orEmpty()
        list.sumOf { it.unreadCountFor(currentUserId) }
    }
    val allPeople = (peopleState as? UiState.Success)?.data.orEmpty()
    val readReceiptsEnabled = privateUser?.readReceiptsEnabled ?: true
    val pendingDeepLinkId by MessageNotificationCoordinator.pendingDeepLinkConversationId.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    var expandedSelectedChatId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeTab by rememberSaveable { mutableStateOf(Routes.CHATS) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 840.dp
        val isMedium = maxWidth >= 600.dp && !isExpanded

        val currentVisibleConversationId = if (isExpanded) {
            if (activeTab == Routes.CHATS) expandedSelectedChatId else null
        } else if (currentRoute == Routes.CHAT_ROOM) {
            backStackEntry?.arguments?.getString("conversationId")
        } else {
            null
        }
        LaunchedEffect(currentVisibleConversationId) {
            MessageNotificationCoordinator.setActiveConversationId(currentVisibleConversationId)
            if (!currentVisibleConversationId.isNullOrBlank()) {
                NotificationHelper.cancelConversationNotification(context, currentVisibleConversationId)
            }
        }

        if (isExpanded) {
            LaunchedEffect(pendingDeepLinkId) {
                val targetId = pendingDeepLinkId
                if (!targetId.isNullOrBlank()) {
                    expandedSelectedChatId = targetId
                    activeTab = Routes.CHATS
                    MessageNotificationCoordinator.consumeDeepLinkConversation()
                }
            }

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    PulseNavigationRail(
                        selectedRoute = activeTab,
                        totalUnread = totalUnread,
                        onSelectRoute = { activeTab = it }
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    if (activeTab == Routes.CHATS) {
                        Box(modifier = Modifier.width(380.dp).fillMaxHeight()) {
                            ChatsScreen(
                                currentUserId = currentUserId,
                                currentUserProfile = currentUserProfile,
                                conversationsState = conversationsState,
                                peopleState = peopleState,
                                searchQuery = searchQuery,
                                selectedFilter = chatFilter,
                                selectedConversationId = expandedSelectedChatId,
                                onSearchQueryChange = mainViewModel::updateSearchQuery,
                                onSelectFilter = mainViewModel::selectChatFilter,
                                onOpenConversation = { expandedSelectedChatId = it },
                                onStartDirectChat = { person ->
                                    mainViewModel.startDirectChat(person) { expandedSelectedChatId = it }
                                },
                                onCreateGroupChat = { title, topic, members ->
                                    mainViewModel.createGroupChat(title, topic, members) { expandedSelectedChatId = it }
                                },
                                onProfileClick = { activeTab = Routes.PROFILE }
                            )
                        }
                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            val selectedId = expandedSelectedChatId
                            if (selectedId != null) {
                                key(selectedId) {
                                    val chatRoomVm: ChatRoomViewModel = viewModel(
                                        key = "${currentUserId}_$selectedId",
                                        factory = viewModelFactory {
                                            initializer {
                                                val app = checkNotNull(this[APPLICATION_KEY])
                                                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                                                ChatRoomViewModel(MessagingRepository(db), selectedId, currentUserId)
                                            }
                                        }
                                    )
                                    ChatRoomScreen(
                                        viewModel = chatRoomVm,
                                        currentUserProfile = currentUserProfile,
                                        allPeople = allPeople,
                                        readReceiptsEnabled = readReceiptsEnabled,
                                        showBackButton = false,
                                        onBack = { expandedSelectedChatId = null }
                                    )
                                }
                            } else {
                                EmptyExpandedChatPane()
                            }
                        }
                    } else if (activeTab == Routes.PEOPLE) {
                        PeopleScreen(
                            currentUserId = currentUserId,
                            currentUserProfile = currentUserProfile,
                            peopleState = peopleState,
                            searchQuery = peopleSearchQuery,
                            onSearchQueryChange = mainViewModel::updatePeopleSearchQuery,
                            onMessagePerson = { person ->
                                mainViewModel.startDirectChat(person) { convId ->
                                    expandedSelectedChatId = convId
                                    activeTab = Routes.CHATS
                                }
                            }
                        )
                    } else {
                        ProfileScreen(
                            userProfile = currentUserProfile,
                            privateUser = privateUser,
                            isBusy = isBusy,
                            onSaveProfile = mainViewModel::saveProfile,
                            onUpdatePrivacy = mainViewModel::updatePrivacyPreferences,
                            onSignOut = {
                                MessageNotificationCoordinator.stopObserving()
                                RealtimeMessagingService.syncServiceState(context, shouldRun = false)
                                mainViewModel.setOnlineStatus(false)
                                signOut(credentialManager, coroutineScope)
                            },
                            hasPhoneNotificationPermission = hasNotificationPermission,
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )
                    }
                }
            }
        } else {
            LaunchedEffect(pendingDeepLinkId) {
                val targetId = pendingDeepLinkId
                if (!targetId.isNullOrBlank()) {
                    navController.navigate(Routes.chatRoom(targetId)) {
                        launchSingleTop = true
                    }
                    MessageNotificationCoordinator.consumeDeepLinkConversation()
                }
            }
            val showTopLevelNav = currentRoute in setOf(Routes.CHATS, Routes.PEOPLE, Routes.PROFILE)
            val navigateTopLevel: (String) -> Unit = { route ->
                navController.navigate(route) {
                    popUpTo(Routes.CHATS) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (showTopLevelNav && !isMedium) {
                        PulseBottomBar(
                            selectedRoute = currentRoute,
                            totalUnread = totalUnread,
                            onSelectRoute = navigateTopLevel
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (showTopLevelNav && isMedium) {
                        PulseNavigationRail(
                            selectedRoute = currentRoute,
                            totalUnread = totalUnread,
                            onSelectRoute = navigateTopLevel
                        )
                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    NavHost(
                        navController = navController,
                        startDestination = Routes.CHATS,
                        enterTransition = { fadeIn(tween(180)) },
                        exitTransition = { fadeOut(tween(150)) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        composable(Routes.CHATS) {
                            ChatsScreen(
                                currentUserId = currentUserId,
                                currentUserProfile = currentUserProfile,
                                conversationsState = conversationsState,
                                peopleState = peopleState,
                                searchQuery = searchQuery,
                                selectedFilter = chatFilter,
                                selectedConversationId = null,
                                onSearchQueryChange = mainViewModel::updateSearchQuery,
                                onSelectFilter = mainViewModel::selectChatFilter,
                                onOpenConversation = { convId ->
                                    navController.navigate(Routes.chatRoom(convId))
                                },
                                onStartDirectChat = { person ->
                                    mainViewModel.startDirectChat(person) { convId ->
                                        navController.navigate(Routes.chatRoom(convId))
                                    }
                                },
                                onCreateGroupChat = { title, topic, members ->
                                    mainViewModel.createGroupChat(title, topic, members) { convId ->
                                        navController.navigate(Routes.chatRoom(convId))
                                    }
                                },
                                onProfileClick = { navigateTopLevel(Routes.PROFILE) }
                            )
                        }

                        composable(Routes.PEOPLE) {
                            PeopleScreen(
                                currentUserId = currentUserId,
                                currentUserProfile = currentUserProfile,
                                peopleState = peopleState,
                                searchQuery = peopleSearchQuery,
                                onSearchQueryChange = mainViewModel::updatePeopleSearchQuery,
                                onMessagePerson = { person ->
                                    mainViewModel.startDirectChat(person) { convId ->
                                        navController.navigate(Routes.chatRoom(convId))
                                    }
                                }
                            )
                        }

                        composable(Routes.PROFILE) {
                            ProfileScreen(
                                userProfile = currentUserProfile,
                                privateUser = privateUser,
                                isBusy = isBusy,
                                onSaveProfile = mainViewModel::saveProfile,
                                onUpdatePrivacy = mainViewModel::updatePrivacyPreferences,
                                onSignOut = {
                                    MessageNotificationCoordinator.stopObserving()
                                    RealtimeMessagingService.syncServiceState(context, shouldRun = false)
                                    mainViewModel.setOnlineStatus(false)
                                    signOut(credentialManager, coroutineScope)
                                },
                                hasPhoneNotificationPermission = hasNotificationPermission,
                                onRequestNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            )
                        }

                        composable(
                            route = Routes.CHAT_ROOM,
                            arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
                            enterTransition = {
                                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(240)) + fadeIn(tween(200))
                            },
                            popExitTransition = {
                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(220)) + fadeOut(tween(180))
                            }
                        ) { entry ->
                            val conversationId = entry.arguments?.getString("conversationId").orEmpty()
                            val chatRoomVm: ChatRoomViewModel = viewModel(
                                key = "${currentUserId}_$conversationId",
                                factory = viewModelFactory {
                                    initializer {
                                        val app = checkNotNull(this[APPLICATION_KEY])
                                        val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                                        ChatRoomViewModel(MessagingRepository(db), conversationId, currentUserId)
                                    }
                                }
                            )
                            ChatRoomScreen(
                                viewModel = chatRoomVm,
                                currentUserProfile = currentUserProfile,
                                allPeople = allPeople,
                                readReceiptsEnabled = readReceiptsEnabled,
                                showBackButton = true,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }

        IosInAppNotificationBanner(
            banner = activeBanner,
            onOpenConversation = { convId ->
                activeBanner = null
                MessageNotificationCoordinator.emitDeepLinkConversation(convId)
            },
            onDismiss = { activeBanner = null },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun EmptyExpandedChatPane() {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Forum,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Select a conversation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pick a thread from the left to jump right in",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PulseBottomBar(
    selectedRoute: String?,
    totalUnread: Long,
    onSelectRoute: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .border(
                    width = 0.8.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IosDockItem(
                    selected = selectedRoute == Routes.CHATS,
                    label = "Chats",
                    icon = if (selectedRoute == Routes.CHATS) Icons.AutoMirrored.Rounded.Chat else Icons.AutoMirrored.Outlined.Chat,
                    badgeCount = totalUnread,
                    onClick = { onSelectRoute(Routes.CHATS) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_chats")
                )
                IosDockItem(
                    selected = selectedRoute == Routes.PEOPLE,
                    label = "Contacts",
                    icon = if (selectedRoute == Routes.PEOPLE) Icons.Rounded.Groups else Icons.Outlined.Groups,
                    badgeCount = 0L,
                    onClick = { onSelectRoute(Routes.PEOPLE) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_people")
                )
                IosDockItem(
                    selected = selectedRoute == Routes.PROFILE,
                    label = "Settings",
                    icon = if (selectedRoute == Routes.PROFILE) Icons.Rounded.Person else Icons.Outlined.Person,
                    badgeCount = 0L,
                    onClick = { onSelectRoute(Routes.PROFILE) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_profile")
                )
            }
        }
    }
}

@Composable
private fun IosDockItem(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeCount: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f) else Color.Transparent,
        label = "dock_pill"
    )
    val tintColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "dock_tint"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .background(pillColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BadgedBox(
            badge = {
                if (badgeCount > 0L) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = Color.White
                    ) {
                        Text(if (badgeCount > 99L) "99+" else badgeCount.toString())
                    }
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tintColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tintColor
        )
    }
}

@Composable
private fun PulseNavigationRail(
    selectedRoute: String?,
    totalUnread: Long,
    onSelectRoute: (String) -> Unit
) {
    val railItemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        NavigationRailItem(
            selected = selectedRoute == Routes.CHATS,
            onClick = { onSelectRoute(Routes.CHATS) },
            colors = railItemColors,
            icon = {
                BadgedBox(
                    badge = {
                        if (totalUnread > 0L) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color.White
                            ) {
                                Text(if (totalUnread > 99L) "99+" else totalUnread.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (selectedRoute == Routes.CHATS) Icons.AutoMirrored.Rounded.Chat else Icons.AutoMirrored.Outlined.Chat,
                        contentDescription = "Chats"
                    )
                }
            },
            label = { Text("Chats", fontWeight = FontWeight.SemiBold) }
        )
        NavigationRailItem(
            selected = selectedRoute == Routes.PEOPLE,
            onClick = { onSelectRoute(Routes.PEOPLE) },
            colors = railItemColors,
            icon = {
                Icon(
                    imageVector = if (selectedRoute == Routes.PEOPLE) Icons.Rounded.Groups else Icons.Outlined.Groups,
                    contentDescription = "Contacts"
                )
            },
            label = { Text("Contacts", fontWeight = FontWeight.SemiBold) }
        )
        NavigationRailItem(
            selected = selectedRoute == Routes.PROFILE,
            onClick = { onSelectRoute(Routes.PROFILE) },
            colors = railItemColors,
            icon = {
                Icon(
                    imageVector = if (selectedRoute == Routes.PROFILE) Icons.Rounded.Person else Icons.Outlined.Person,
                    contentDescription = "Settings"
                )
            },
            label = { Text("Settings", fontWeight = FontWeight.SemiBold) }
        )
    }
}
