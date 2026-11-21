package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
import com.example.contacts.DeviceContactsManager
import com.example.crypto.E2eeKeyManager
import com.example.data.model.CallSession
import com.example.data.model.CallTarget
import com.example.data.model.Conversation
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.buildDirectConversationId
import com.example.data.repository.CallSignalingRepository
import com.example.data.repository.MessagingRepository
import com.example.notifications.InAppMessageBanner
import com.example.notifications.MessageNotificationCoordinator
import com.example.notifications.NotificationHelper
import com.example.notifications.RealtimeMessagingService
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.TelegramOtpClient
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOut
import com.example.ui.components.ContactsPermissionPromptDialog
import com.example.ui.components.IosInAppNotificationBanner
import com.example.ui.components.TelegramChatWallpaper
import com.example.ui.components.TelegramLogoMark
import com.example.ui.components.TelegramPreferencesState
import com.example.ui.components.UniversMotion
import com.example.ui.components.rememberIosHaptics
import com.example.ui.screens.CallHostOverlay
import com.example.ui.screens.ChatRoomScreen
import com.example.ui.screens.ChatsScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PeopleScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TelegramDrawerContent
import com.example.ui.screens.UserProfileDetailScreen
import com.example.ui.theme.AppTheme
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.viewmodel.CallViewModel
import com.example.ui.viewmodel.ChatRoomViewModel
import com.example.ui.viewmodel.MainMessagingViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object Routes {
    const val CHATS = "chats"
    const val PEOPLE = "people"
    const val PROFILE = "profile"
    const val NOTIFICATIONS = "notifications"
    const val CHAT_ROOM = "chat_room/{conversationId}"
    const val USER_PROFILE = "user_profile/{userId}"

    fun chatRoom(conversationId: String): String = "chat_room/$conversationId"
    fun userProfile(userId: String): String = "user_profile/$userId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannels(this)
        handleNotificationDeepLink(intent)
        setContent {
            val context = LocalContext.current
            val themePrefs = remember(context) { TelegramPreferencesState(context) }
            val resolvedDark = themePrefs.nightModeOverride ?: isSystemInDarkTheme()

            AppTheme(
                darkTheme = resolvedDark
            ) {
                PulseChatApp(
                    themePrefs = themePrefs,
                    isDark = resolvedDark
                )
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
        val joinedUserId = intent?.getStringExtra(NotificationHelper.EXTRA_JOINED_USER_ID)
        if (!joinedUserId.isNullOrBlank()) {
            MessageNotificationCoordinator.emitDeepLinkUser(joinedUserId)
        }
    }
}

@Composable
fun PulseChatApp(
    themePrefs: TelegramPreferencesState? = null,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val context = LocalContext.current
    val resolvedPrefs = themePrefs ?: remember(context) { TelegramPreferencesState(context) }
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    Box(modifier = Modifier.fillMaxSize()) {
        if (user == null) {
            LaunchedEffect(Unit) {
                MessageNotificationCoordinator.stopObserving()

            }
            AuthScreen(onAuthSuccess = {})
        } else {
            key(user.uid) {
                AuthenticatedShell(
                    currentUserId = user.uid,
                    themePrefs = resolvedPrefs,
                    isDark = isDark
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedShell(
    currentUserId: String,
    themePrefs: TelegramPreferencesState,
    isDark: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val mainViewModel: MainMessagingViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                MainMessagingViewModel(
                    repository = MessagingRepository(db),
                    currentUserId = currentUserId,
                    callSignalingRepository = CallSignalingRepository(db),
                    localE2eePublicKey = E2eeKeyManager.getPublicIdentityKeyBase64(app, currentUserId)
                )
            }
        }
    )

    val callViewModel: CallViewModel = viewModel(
        key = "call_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                CallViewModel(app, CallSignalingRepository(db), currentUserId)
            }
        }
    )

    val currentUserProfile by mainViewModel.currentUserProfile.collectAsStateWithLifecycle()
    val privateUser by mainViewModel.privateUser.collectAsStateWithLifecycle()
    val blockedUserIds by mainViewModel.blockedUserIds.collectAsStateWithLifecycle()
    val callHistory by mainViewModel.callHistory.collectAsStateWithLifecycle()
    val conversationsState by mainViewModel.conversationsState.collectAsStateWithLifecycle()
    val peopleState by mainViewModel.peopleState.collectAsStateWithLifecycle()
    val searchQuery by mainViewModel.searchQuery.collectAsStateWithLifecycle()
    val chatFilter by mainViewModel.chatFilter.collectAsStateWithLifecycle()
    val peopleSearchQuery by mainViewModel.peopleSearchQuery.collectAsStateWithLifecycle()
    val actionMessage by mainViewModel.actionMessage.collectAsStateWithLifecycle()
    val isBusy by mainViewModel.isBusy.collectAsStateWithLifecycle()
    val isUploadingPhoto by mainViewModel.isUploadingPhoto.collectAsStateWithLifecycle()
    val syncedContactHashes by mainViewModel.syncedContactHashes.collectAsStateWithLifecycle()
    val localContactNamesByHash by mainViewModel.localContactNamesByHash.collectAsStateWithLifecycle()
    val isSyncingContacts by mainViewModel.isSyncingContacts.collectAsStateWithLifecycle()
    val incomingCall by callViewModel.incomingCall.collectAsStateWithLifecycle()
    val callState by callViewModel.callState.collectAsStateWithLifecycle()
    val callNotice by callViewModel.callNotice.collectAsStateWithLifecycle()

    LaunchedEffect(blockedUserIds) {
        callViewModel.updateBlockedUserIds(blockedUserIds)
    }

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

    var hasContactsPermission by remember {
        mutableStateOf(DeviceContactsManager.hasContactsPermission(context))
    }
    var showContactsPromptDialog by rememberSaveable(currentUserId) { mutableStateOf(false) }
    val requestContactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasContactsPermission = granted || DeviceContactsManager.hasContactsPermission(context)
        if (hasContactsPermission) {
            mainViewModel.syncDeviceContacts(context, showFeedback = true)
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    "Contacts access skipped — you can sync anytime in Contacts or search by @username."
                )
            }
        }
    }
    val triggerContactsSyncFlow: () -> Unit = {
        if (DeviceContactsManager.hasContactsPermission(context)) {
            hasContactsPermission = true
            mainViewModel.syncDeviceContacts(context, showFeedback = true)
        } else {
            showContactsPromptDialog = true
        }
    }

    LaunchedEffect(currentUserId, hasContactsPermission) {
        if (hasContactsPermission) {
            mainViewModel.syncDeviceContacts(context, showFeedback = false)
        } else if (!DeviceContactsManager.hasSeenContactsPrompt(context, currentUserId)) {
            delay(1100L)
            DeviceContactsManager.markContactsPromptSeen(context, currentUserId)
            showContactsPromptDialog = true
        }
    }

    DisposableEffect(lifecycleOwner, mainViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    mainViewModel.setOnlineStatus(true)
                    MessageNotificationCoordinator.setAppForegroundState(true)
                    hasNotificationPermission = NotificationHelper.hasNotificationPermission(context)
                    hasContactsPermission = DeviceContactsManager.hasContactsPermission(context)
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

    LaunchedEffect(callNotice) {
        val notice = callNotice
        if (notice != null) {
            snackbarHostState.showSnackbar(notice)
            callViewModel.clearCallNotice()
        }
    }

    var pendingCallAction by remember { mutableStateOf<PendingCallAction?>(null) }
    var callPermissionMessage by remember { mutableStateOf<String?>(null) }

    val executePendingCall: (PendingCallAction) -> Unit = { action ->
        when (action) {
            is PendingCallAction.Outgoing -> {
                val profile = currentUserProfile
                if (profile == null) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Syncing your profile, please try again in a moment.")
                    }
                } else {
                    callViewModel.startOutgoingCall(
                        callerProfile = profile,
                        target = action.target,
                        isVideoCall = action.isVideoCall
                    )
                }
            }
            is PendingCallAction.Accept -> {
                callViewModel.acceptIncomingCall(action.session)
            }
        }
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val action = pendingCallAction ?: return@rememberLauncherForActivityResult
        pendingCallAction = null
        val allGranted = results.values.all { it }
        if (allGranted) {
            executePendingCall(action)
        } else {
            callPermissionMessage = if (action.needsCamera) {
                "Camera and microphone permissions are required for video calls."
            } else {
                "Microphone permission is required for voice calls."
            }
        }
    }

    val launchWithCallPermissions: (PendingCallAction) -> Unit = { action ->
        val required = if (action.needsCamera) {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }
        val missing = required.filter { perm ->
            ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            executePendingCall(action)
        } else {
            pendingCallAction = action
            callPermissionLauncher.launch(missing.toTypedArray())
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
    val performSignOut: () -> Unit = {
        callViewModel.hangUp()
        MessageNotificationCoordinator.stopObserving()
        TelegramOtpClient.clearSavedSessionJwt(context)
        signOut(
            scope = coroutineScope,
            beforeSignOut = { mainViewModel.setOnlineStatusSuspend(false) }
        )
    }

    val allConversations = (conversationsState as? UiState.Success)?.data.orEmpty()
    val connectedUserIds = remember(allConversations, currentUserId, blockedUserIds) {
        allConversations
            .flatMap { it.participantIds }
            .filter { it != currentUserId && it !in blockedUserIds }
            .toSet()
    }
    val totalUnread = remember(allConversations, currentUserId) {
        allConversations.sumOf { it.unreadCountFor(currentUserId) }
    }
    val allPeople = (peopleState as? UiState.Success)?.data.orEmpty()
    val allContactHashes = remember(syncedContactHashes, privateUser?.contactHashes) {
        syncedContactHashes + privateUser?.contactHashes.orEmpty()
    }
    val matchedContactUserIds = remember(allPeople, allContactHashes, currentUserId, blockedUserIds) {
        if (allContactHashes.isEmpty()) {
            emptySet()
        } else {
            allPeople
                .filter {
                    it.userId != currentUserId &&
                        it.userId !in blockedUserIds &&
                        it.phoneHash.isNotBlank() &&
                        it.phoneHash in allContactHashes
                }
                .map { it.userId }
                .toSet()
        }
    }
    val readReceiptsEnabled = privateUser?.readReceiptsEnabled ?: true
    val pendingDeepLinkId by MessageNotificationCoordinator.pendingDeepLinkConversationId.collectAsStateWithLifecycle()
    val pendingDeepLinkUserId by MessageNotificationCoordinator.pendingDeepLinkUserId.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    var expandedSelectedChatId by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedViewingUserId by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedShowNotifications by rememberSaveable { mutableStateOf(false) }
    var activeTab by rememberSaveable { mutableStateOf(Routes.CHATS) }
    var newGroupTrigger by remember { mutableIntStateOf(0) }
    var callHistoryTrigger by remember { mutableIntStateOf(0) }

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
            LaunchedEffect(pendingDeepLinkUserId) {
                val targetUserId = pendingDeepLinkUserId
                if (!targetUserId.isNullOrBlank()) {
                    expandedViewingUserId = targetUserId
                    activeTab = Routes.PEOPLE
                    MessageNotificationCoordinator.consumeDeepLinkUser()
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
                        isDark = isDark,
                        onToggleNightMode = { themePrefs.toggleNightMode(isDark) },
                        onSelectRoute = {
                            expandedViewingUserId = null
                            expandedShowNotifications = false
                            activeTab = it
                        }
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    val viewingUserId = expandedViewingUserId
                    if (viewingUserId != null) {
                        val viewedProfile = remember(viewingUserId, allPeople, allConversations, currentUserProfile) {
                            resolveTargetUserProfile(viewingUserId, allPeople, allConversations, currentUserProfile)
                        }
                        UserProfileDetailScreen(
                            profile = viewedProfile,
                            isBlocked = viewingUserId in blockedUserIds,
                            onBack = { expandedViewingUserId = null },
                            onStartChat = { person ->
                                expandedViewingUserId = null
                                mainViewModel.startDirectChat(person) { convId ->
                                    expandedSelectedChatId = convId
                                    activeTab = Routes.CHATS
                                }
                            },
                            onVoiceCall = { person ->
                                launchWithCallPermissions(
                                    PendingCallAction.Outgoing(
                                        target = CallTarget(
                                            userId = person.userId,
                                            displayName = person.displayName,
                                            photoUrl = person.photoUrl,
                                            conversationId = buildDirectConversationId(currentUserId, person.userId)
                                        ),
                                        isVideoCall = false
                                    )
                                )
                            },
                            onVideoCall = { person ->
                                launchWithCallPermissions(
                                    PendingCallAction.Outgoing(
                                        target = CallTarget(
                                            userId = person.userId,
                                            displayName = person.displayName,
                                            photoUrl = person.photoUrl,
                                            conversationId = buildDirectConversationId(currentUserId, person.userId)
                                        ),
                                        isVideoCall = true
                                    )
                                )
                            },
                            onToggleBlock = { person ->
                                mainViewModel.setUserBlocked(person.userId, person.userId !in blockedUserIds)
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    } else if (expandedShowNotifications) {
                        NotificationsScreen(
                            currentUserId = currentUserId,
                            conversations = allConversations,
                            people = allPeople,
                            callHistory = callHistory,
                            blockedUserIds = blockedUserIds,
                            notificationsEnabled = notificationsEnabled,
                            hasPhoneNotificationPermission = hasNotificationPermission,
                            inAppBannersEnabled = themePrefs.inAppBannersEnabled,
                            showMessagePreviews = themePrefs.showMessagePreviews,
                            vibrateEnabled = themePrefs.vibrateEnabled,
                            onBack = { expandedShowNotifications = false },
                            onOpenConversation = { convId ->
                                expandedShowNotifications = false
                                expandedSelectedChatId = convId
                                activeTab = Routes.CHATS
                            },
                            onToggleMuteConversation = mainViewModel::toggleMuteConversation,
                            onMarkAllRead = mainViewModel::markAllConversationsRead,
                            onCallBack = { target, isVideo ->
                                launchWithCallPermissions(PendingCallAction.Outgoing(target, isVideo))
                            },
                            onToggleNotificationsEnabled = { enabled ->
                                if (enabled && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                mainViewModel.updatePrivacyPreferences(readReceiptsEnabled, enabled)
                            },
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onToggleInAppBanners = themePrefs::updateInAppBanners,
                            onToggleShowMessagePreviews = themePrefs::updateShowMessagePreviews,
                            onToggleVibrate = themePrefs::updateVibrate,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    } else if (activeTab == Routes.CHATS) {
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
                                onDeleteConversation = { conversation, onResult ->
                                    if (expandedSelectedChatId == conversation.id) {
                                        expandedSelectedChatId = null
                                    }
                                    mainViewModel.deleteOrLeaveConversation(conversation, onResult)
                                },
                                onProfileClick = { activeTab = Routes.PROFILE },
                                onOpenNotifications = { expandedShowNotifications = true },
                                blockedUserIds = blockedUserIds,
                                callHistory = callHistory,
                                onTogglePinConversation = mainViewModel::togglePinConversation,
                                onToggleMuteConversation = mainViewModel::toggleMuteConversation,
                                onToggleMarkUnread = mainViewModel::toggleMarkUnread,
                                onStartCall = { target, isVideo ->
                                    launchWithCallPermissions(PendingCallAction.Outgoing(target, isVideo))
                                },
                                externalNewGroupTrigger = newGroupTrigger,
                                externalCallHistoryTrigger = callHistoryTrigger
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
                                        onBack = { expandedSelectedChatId = null },
                                        onStartCall = { target, isVideo ->
                                            launchWithCallPermissions(PendingCallAction.Outgoing(target, isVideo))
                                        },
                                        allConversations = allConversations,
                                        blockedUserIds = blockedUserIds,
                                        onTogglePinConversation = mainViewModel::togglePinConversation,
                                        onToggleMuteConversation = mainViewModel::toggleMuteConversation,
                                        onToggleBlockUser = mainViewModel::setUserBlocked,
                                        onOpenUserProfile = { targetUid -> expandedViewingUserId = targetUid }
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
                            },
                            onCallPerson = { person, isVideo ->
                                launchWithCallPermissions(
                                    PendingCallAction.Outgoing(
                                        target = CallTarget(
                                            userId = person.userId,
                                            displayName = person.displayName,
                                            photoUrl = person.photoUrl,
                                            conversationId = buildDirectConversationId(currentUserId, person.userId)
                                        ),
                                        isVideoCall = isVideo
                                    )
                                )
                            },
                            onViewProfile = { person -> expandedViewingUserId = person.userId },
                            blockedUserIds = blockedUserIds,
                            connectedUserIds = connectedUserIds,
                            hasContactsPermission = hasContactsPermission,
                            isSyncingContacts = isSyncingContacts,
                            matchedContactUserIds = matchedContactUserIds,
                            localContactNamesByHash = localContactNamesByHash,
                            onRequestContactsSync = triggerContactsSyncFlow
                        )
                    } else {
                        ProfileScreen(
                            userProfile = currentUserProfile,
                            privateUser = privateUser,
                            isBusy = isBusy,
                            onSaveProfile = mainViewModel::saveProfile,
                            onUpdatePrivacy = mainViewModel::updatePrivacyPreferences,
                            onSignOut = performSignOut,
                            isUploadingPhoto = isUploadingPhoto,
                            onUploadProfilePhoto = mainViewModel::uploadProfilePhoto,
                            onRemoveProfilePhoto = mainViewModel::removeProfilePhoto,
                            hasPhoneNotificationPermission = hasNotificationPermission,
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            blockedUserIds = blockedUserIds,
                            allPeople = allPeople,
                            onUnblockUser = { targetId -> mainViewModel.setUserBlocked(targetId, false) },
                            isNightMode = isDark,
                            onToggleNightMode = { themePrefs.toggleNightMode(isDark) },
                            onOpenNotifications = { expandedShowNotifications = true },
                            unreadNotificationsCount = totalUnread,
                            onOpenSavedMessages = currentUserProfile?.let { me ->
                                {
                                    mainViewModel.startDirectChat(me) { convId ->
                                        expandedSelectedChatId = convId
                                        activeTab = Routes.CHATS
                                    }
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
            LaunchedEffect(pendingDeepLinkUserId) {
                val targetUserId = pendingDeepLinkUserId
                if (!targetUserId.isNullOrBlank()) {
                    navController.navigate(Routes.userProfile(targetUserId)) {
                        launchSingleTop = true
                    }
                    MessageNotificationCoordinator.consumeDeepLinkUser()
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
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val userEmailOrPhone = privateUser?.email?.takeIf { it.isNotBlank() }
                ?: Firebase.auth.currentUser?.phoneNumber

            val scaffoldContent: @Composable () -> Unit = {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        AnimatedVisibility(
                            visible = showTopLevelNav && !isMedium,
                            enter = fadeIn(tween(180, easing = UniversMotion.FluidOutEasing)) +
                                expandVertically(tween(220, easing = UniversMotion.FluidOutEasing)),
                            exit = fadeOut(tween(140)) +
                                shrinkVertically(tween(180, easing = UniversMotion.FluidOutEasing))
                        ) {
                            UniversBottomNavigationBar(
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
                                isDark = isDark,
                                onToggleNightMode = { themePrefs.toggleNightMode(isDark) },
                                onSelectRoute = navigateTopLevel
                            )
                            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }

                        NavHost(
                            navController = navController,
                            startDestination = Routes.CHATS,
                            enterTransition = {
                                fadeIn(tween(UniversMotion.TAB_SWITCH_MS, easing = UniversMotion.FluidOutEasing)) +
                                    scaleIn(
                                        initialScale = 0.985f,
                                        animationSpec = tween(UniversMotion.TAB_SWITCH_MS, easing = UniversMotion.FluidOutEasing)
                                    )
                            },
                            exitTransition = {
                                val pushingDetail = targetState.destination.route in setOf(
                                    Routes.CHAT_ROOM,
                                    Routes.NOTIFICATIONS,
                                    Routes.USER_PROFILE
                                )
                                if (pushingDetail) {
                                    slideOutHorizontally(
                                        animationSpec = tween(UniversMotion.PUSH_ENTER_MS, easing = UniversMotion.FluidOutEasing),
                                        targetOffsetX = { -(it * 0.22f).toInt() }
                                    ) + fadeOut(
                                        animationSpec = tween(UniversMotion.PUSH_ENTER_MS, easing = UniversMotion.SoftInOutEasing),
                                        targetAlpha = 0.82f
                                    )
                                } else {
                                    fadeOut(tween(130, easing = UniversMotion.SoftInOutEasing))
                                }
                            },
                            popEnterTransition = {
                                val poppingFromDetail = initialState.destination.route in setOf(
                                    Routes.CHAT_ROOM,
                                    Routes.NOTIFICATIONS,
                                    Routes.USER_PROFILE
                                )
                                if (poppingFromDetail) {
                                    slideInHorizontally(
                                        animationSpec = tween(UniversMotion.PUSH_EXIT_MS, easing = UniversMotion.FluidOutEasing),
                                        initialOffsetX = { -(it * 0.22f).toInt() }
                                    ) + fadeIn(
                                        animationSpec = tween(UniversMotion.PUSH_EXIT_MS, easing = UniversMotion.FluidOutEasing),
                                        initialAlpha = 0.82f
                                    )
                                } else {
                                    fadeIn(tween(UniversMotion.TAB_SWITCH_MS, easing = UniversMotion.FluidOutEasing))
                                }
                            },
                            popExitTransition = {
                                fadeOut(tween(130, easing = UniversMotion.SoftInOutEasing))
                            },
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
                                    onDeleteConversation = { conversation, onResult ->
                                        mainViewModel.deleteOrLeaveConversation(conversation, onResult)
                                    },
                                    onProfileClick = { navigateTopLevel(Routes.PROFILE) },
                                    onOpenDrawer = if (!isMedium) {
                                        { coroutineScope.launch { drawerState.open() } }
                                    } else null,
                                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                                    blockedUserIds = blockedUserIds,
                                    callHistory = callHistory,
                                    onTogglePinConversation = mainViewModel::togglePinConversation,
                                    onToggleMuteConversation = mainViewModel::toggleMuteConversation,
                                    onToggleMarkUnread = mainViewModel::toggleMarkUnread,
                                    onStartCall = { target, isVideo ->
                                        launchWithCallPermissions(PendingCallAction.Outgoing(target, isVideo))
                                    },
                                    externalNewGroupTrigger = newGroupTrigger,
                                    externalCallHistoryTrigger = callHistoryTrigger
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
                                    },
                                    onCallPerson = { person, isVideo ->
                                        launchWithCallPermissions(
                                            PendingCallAction.Outgoing(
                                                target = CallTarget(
                                                    userId = person.userId,
                                                    displayName = person.displayName,
                                                    photoUrl = person.photoUrl,
                                                    conversationId = buildDirectConversationId(currentUserId, person.userId)
                                                ),
                                                isVideoCall = isVideo
                                            )
                                        )
                                    },
                                    onViewProfile = { person ->
                                        navController.navigate(Routes.userProfile(person.userId))
                                    },
                                    blockedUserIds = blockedUserIds,
                                    connectedUserIds = connectedUserIds,
                                    hasContactsPermission = hasContactsPermission,
                                    isSyncingContacts = isSyncingContacts,
                                    matchedContactUserIds = matchedContactUserIds,
                                    localContactNamesByHash = localContactNamesByHash,
                                    onRequestContactsSync = triggerContactsSyncFlow,
                                    showBackButton = false,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Routes.PROFILE) {
                                ProfileScreen(
                                    userProfile = currentUserProfile,
                                    privateUser = privateUser,
                                    isBusy = isBusy,
                                    onSaveProfile = mainViewModel::saveProfile,
                                    onUpdatePrivacy = mainViewModel::updatePrivacyPreferences,
                                    onSignOut = performSignOut,
                                    isUploadingPhoto = isUploadingPhoto,
                                    onUploadProfilePhoto = mainViewModel::uploadProfilePhoto,
                                    onRemoveProfilePhoto = mainViewModel::removeProfilePhoto,
                                    hasPhoneNotificationPermission = hasNotificationPermission,
                                    onRequestNotificationPermission = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    },
                                    blockedUserIds = blockedUserIds,
                                    allPeople = allPeople,
                                    onUnblockUser = { targetId -> mainViewModel.setUserBlocked(targetId, false) },
                                    showBackButton = false,
                                    onBack = { navController.popBackStack() },
                                    isNightMode = isDark,
                                    onToggleNightMode = { themePrefs.toggleNightMode(isDark) },
                                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                                    unreadNotificationsCount = totalUnread,
                                    onOpenSavedMessages = currentUserProfile?.let { me ->
                                        {
                                            mainViewModel.startDirectChat(me) { convId ->
                                                navController.navigate(Routes.chatRoom(convId))
                                            }
                                        }
                                    }
                                )
                            }

                            composable(
                                route = Routes.NOTIFICATIONS,
                                enterTransition = {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Start,
                                        tween(UniversMotion.PUSH_ENTER_MS, easing = UniversMotion.FluidOutEasing)
                                    ) + fadeIn(tween(200, easing = UniversMotion.FluidOutEasing))
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(UniversMotion.PUSH_ENTER_MS, easing = UniversMotion.FluidOutEasing),
                                        targetOffsetX = { -(it * 0.22f).toInt() }
                                    ) + fadeOut(tween(200), targetAlpha = 0.84f)
                                },
                                popEnterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(UniversMotion.PUSH_EXIT_MS, easing = UniversMotion.FluidOutEasing),
                                        initialOffsetX = { -(it * 0.22f).toInt() }
                                    ) + fadeIn(tween(200), initialAlpha = 0.84f)
                                },
                                popExitTransition = {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.End,
                                        tween(UniversMotion.PUSH_EXIT_MS, easing = UniversMotion.FluidOutEasing)
                                    ) + fadeOut(tween(180, easing = UniversMotion.SoftInOutEasing))
                                }
                            ) {
                                NotificationsScreen(
                                    currentUserId = currentUserId,
                                    conversations = allConversations,
                                    people = allPeople,
                                    callHistory = callHistory,
                                    blockedUserIds = blockedUserIds,
                                    notificationsEnabled = notificationsEnabled,
                                    hasPhoneNotificationPermission = hasNotificationPermission,
                                    inAppBannersEnabled = themePrefs.inAppBannersEnabled,
                                    showMessagePreviews = themePrefs.showMessagePreviews,
                                    vibrateEnabled = themePrefs.vibrateEnabled,
                                    onBack = { navController.popBackStack() },
                                    onOpenConversation = { convId ->
                                        navController.navigate(Routes.chatRoom(convId)) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onToggleMuteConversation = mainViewModel::toggleMuteConversation,
                                    onMarkAllRead = mainViewModel::markAllConversationsRead,
                                    onCallBack = { target, isVideo ->
                                        launchWithCallPermissions(PendingCallAction.Outgoing(target, isVideo))
                                    },
                                    onToggleNotificationsEnabled = { enabled ->
                                        if (enabled && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        mainViewModel.updatePrivacyPreferences(readReceiptsEnabled, enabled)
                                    },
                                    onRequestNotificationPermission = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    },
                                    onToggleInAppBanners = themePrefs::updateInAppBanners,
                                    onToggleShowMessagePreviews = themePrefs::updateShowMessagePreviews,
                                    onToggleVibrate = themePrefs::updateVibrate
                                )
                            }

                            composable(
                                route = Routes.USER_PROFILE,
                                arguments = listOf(navArgument("userId") { type = NavType.StringType }),
                                enterTransition = {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Start,
                                        tween(UniversMotion.PUSH_ENTER_MS, easing = UniversMotion.FluidOutEasing)
                                    ) + fadeIn(tween(200, easing = UniversMotion.FluidOutEasing))
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(UniversMotion.PUSH_ENTER_MS, easing = UniversMotion.FluidOutEasing),
                                        targetOffsetX = { -(it * 0.22f).toInt() }
                                    ) + fadeOut(tween(200), targetAlpha = 0.84f)
                                },
                                popEnterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(UniversMotion.PUSH_EXIT_MS, easing = UniversMotion.FluidOutEasing),
                                        initialOffsetX = { -(it * 0.22f).toInt() }
                                    ) + fadeIn(tween(200), initialAlpha = 0.84f)
                                },
                                popExitTransition = {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.End,
                                        tween(UniversMotion.PUSH_EXIT_MS, easing = UniversMotion.FluidOutEasing)
                                    ) + fadeOut(tween(180, easing = UniversMotion.SoftInOutEasing))
                                }
                            ) { entry ->
                                val targetUserId = entry.arguments?.getString("userId").orEmpty()
                                val targetProfile = remember(targetUserId, allPeople, allConversations, currentUserProfile) {
                                    resolveTargetUserProfile(targetUserId, allPeople, allConversations, currentUserProfile)
                                }
                                UserProfileDetailScreen(
                                    profile = targetProfile,
                                    isBlocked = targetUserId in blockedUserIds,
                                    onBack = { navController.popBackStack() },
                                    onStartChat = { person ->
                                        mainViewModel.startDirectChat(person) { convId ->
                                            navController.navigate(Routes.chatRoom(convId)) {
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    onVoiceCall = { person ->
                                        launchWithCallPermissions(
                                            PendingCallAction.Outgoing(
                                                target = CallTarget(
                                                    userId = person.userId,
                                                    displayName = person.displayName,
                                                    photoUrl = person.photoUrl,
                                                    conversationId = buildDirectConversationId(currentUserId, person.userId)
                                                ),
                                                isVideoCall = false
                                            )
                                        )
                                    },
                                    onVideoCall = { person ->
                                        launchWithCallPermissions(
                                            PendingCallAction.Outgoing(
                                                target = CallTarget(
                                                    userId = person.userId,
                                                    displayName = person.displayName,
                                                    photoUrl = person.photoUrl,
                                                    conversationId = buildDirectConversationId(currentUserId, person.userId)
                                                ),
                                                isVideoCall = true
                                            )
                                        )
                                    },
                                    onToggleBlock = { person ->
                                        mainViewModel.setUserBlocked(person.userId, person.userId !in blockedUserIds)
                                    }
                                )
                            }

                            composable(
                                route = Routes.CHAT_ROOM,
                                arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
                                enterTransition = {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Start,
                                        tween(UniversMotion.PUSH_ENTER_MS, easing = UniversMotion.FluidOutEasing)
                                    )
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(UniversMotion.PUSH_ENTER_MS, easing = UniversMotion.FluidOutEasing),
                                        targetOffsetX = { -(it * 0.22f).toInt() }
                                    ) + fadeOut(tween(200), targetAlpha = 0.84f)
                                },
                                popEnterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(UniversMotion.PUSH_EXIT_MS, easing = UniversMotion.FluidOutEasing),
                                        initialOffsetX = { -(it * 0.22f).toInt() }
                                    ) + fadeIn(tween(200), initialAlpha = 0.84f)
                                },
                                popExitTransition = {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.End,
                                        tween(UniversMotion.PUSH_EXIT_MS, easing = UniversMotion.FluidOutEasing)
                                    )
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
                                    onBack = { navController.popBackStack() },
                                    onStartCall = { target, isVideo ->
                                        launchWithCallPermissions(PendingCallAction.Outgoing(target, isVideo))
                                    },
                                    allConversations = allConversations,
                                    blockedUserIds = blockedUserIds,
                                    onTogglePinConversation = mainViewModel::togglePinConversation,
                                    onToggleMuteConversation = mainViewModel::toggleMuteConversation,
                                    onToggleBlockUser = mainViewModel::setUserBlocked,
                                    onOpenUserProfile = { targetUid ->
                                        navController.navigate(Routes.userProfile(targetUid))
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (!isMedium) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = currentRoute == Routes.CHATS,
                    drawerContent = {
                        TelegramDrawerContent(
                            currentUserProfile = currentUserProfile,
                            userEmailOrPhone = userEmailOrPhone,
                            onToggleNightMode = { themePrefs.toggleNightMode(isDark) },
                            onNewGroupClick = {
                                coroutineScope.launch { drawerState.close() }
                                newGroupTrigger += 1
                            },
                            onContactsClick = {
                                coroutineScope.launch { drawerState.close() }
                                navigateTopLevel(Routes.PEOPLE)
                            },
                            onCallsClick = {
                                coroutineScope.launch { drawerState.close() }
                                callHistoryTrigger += 1
                            },
                            onSavedMessagesClick = {
                                coroutineScope.launch { drawerState.close() }
                                currentUserProfile?.let { me ->
                                    mainViewModel.startDirectChat(me) { convId ->
                                        navController.navigate(Routes.chatRoom(convId))
                                    }
                                }
                            },
                            onSettingsClick = {
                                coroutineScope.launch { drawerState.close() }
                                navigateTopLevel(Routes.PROFILE)
                            },
                            onSignOutClick = {
                                coroutineScope.launch { drawerState.close() }
                                performSignOut()
                            },
                            onNotificationsClick = {
                                coroutineScope.launch { drawerState.close() }
                                navController.navigate(Routes.NOTIFICATIONS)
                            },
                            unreadNotificationsCount = totalUnread
                        )
                    }
                ) {
                    scaffoldContent()
                }
            } else {
                scaffoldContent()
            }
        }

        CallHostOverlay(
            currentUserId = currentUserId,
            incomingCall = incomingCall,
            callState = callState,
            onAcceptIncoming = { session ->
                launchWithCallPermissions(PendingCallAction.Accept(session))
            },
            onDeclineIncoming = callViewModel::declineIncomingCall,
            onHangUp = callViewModel::hangUp,
            onToggleMute = callViewModel::toggleMute,
            onToggleSpeaker = callViewModel::toggleSpeaker,
            onToggleCamera = callViewModel::toggleCamera,
            onSwitchCamera = callViewModel::switchCamera,
            onCycleQuality = callViewModel::cycleVideoQualityPreset,
            onSetMinimized = callViewModel::setMinimized,
            onAttachLocalRenderer = callViewModel::attachLocalRenderer,
            onDetachLocalRenderer = callViewModel::detachLocalRenderer,
            onAttachRemoteRenderer = callViewModel::attachRemoteRenderer,
            onDetachRemoteRenderer = callViewModel::detachRemoteRenderer
        )

        IosInAppNotificationBanner(
            banner = if (themePrefs.inAppBannersEnabled) activeBanner else null,
            showMessagePreview = themePrefs.showMessagePreviews,
            onOpenConversation = { convId ->
                activeBanner = null
                MessageNotificationCoordinator.emitDeepLinkConversation(convId)
            },
            onDismiss = { activeBanner = null },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        callPermissionMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { callPermissionMessage = null },
                title = { Text("Permission needed for calls") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { callPermissionMessage = null }) {
                        Text("OK")
                    }
                }
            )
        }

        if (showContactsPromptDialog) {
            ContactsPermissionPromptDialog(
                onAllowContacts = {
                    showContactsPromptDialog = false
                    DeviceContactsManager.markContactsPromptSeen(context, currentUserId)
                    requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                },
                onDismiss = {
                    showContactsPromptDialog = false
                    DeviceContactsManager.markContactsPromptSeen(context, currentUserId)
                }
            )
        }
    }
}

private sealed interface PendingCallAction {
    val needsCamera: Boolean

    data class Outgoing(val target: CallTarget, val isVideoCall: Boolean) : PendingCallAction {
        override val needsCamera: Boolean get() = isVideoCall
    }

    data class Accept(val session: CallSession) : PendingCallAction {
        override val needsCamera: Boolean get() = session.isVideoCall
    }
}

@Composable
private fun EmptyExpandedChatPane() {
    val isDark = LocalIsTelegramDark.current
    Box(modifier = Modifier.fillMaxSize()) {
        TelegramChatWallpaper()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = if (isDark) Color(0xCC121426) else Color(0x881A1C33)
            ) {
                Text(
                    text = "Select a chat to start messaging",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun UniversBottomNavigationBar(
    selectedRoute: String?,
    totalUnread: Long,
    onSelectRoute: (String) -> Unit
) {
    val haptics = rememberIosHaptics()
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onBackground,
        selectedTextColor = MaterialTheme.colorScheme.onBackground,
        indicatorColor = Color.Transparent,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val chatsSelected = selectedRoute == Routes.CHATS
    val peopleSelected = selectedRoute == Routes.PEOPLE
    val profileSelected = selectedRoute == Routes.PROFILE

    Column {
        HorizontalDivider(
            thickness = 0.6.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                selected = chatsSelected,
                onClick = {
                    if (!chatsSelected) haptics.selectionTick()
                    onSelectRoute(Routes.CHATS)
                },
                colors = itemColors,
                icon = {
                    BadgedBox(
                        badge = {
                            if (totalUnread > 0L) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.onBackground,
                                    contentColor = MaterialTheme.colorScheme.background
                                ) {
                                    Text(if (totalUnread > 99L) "99+" else totalUnread.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (chatsSelected) Icons.AutoMirrored.Rounded.Chat else Icons.AutoMirrored.Outlined.Chat,
                            contentDescription = "Chats"
                        )
                    }
                },
                label = {
                    Text(
                        text = "Chats",
                        fontWeight = if (chatsSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                modifier = Modifier.testTag("bottom_nav_chats")
            )
            NavigationBarItem(
                selected = peopleSelected,
                onClick = {
                    if (!peopleSelected) haptics.selectionTick()
                    onSelectRoute(Routes.PEOPLE)
                },
                colors = itemColors,
                icon = {
                    Icon(
                        imageVector = if (peopleSelected) Icons.Rounded.Groups else Icons.Outlined.Groups,
                        contentDescription = "Contacts"
                    )
                },
                label = {
                    Text(
                        text = "Contacts",
                        fontWeight = if (peopleSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                modifier = Modifier.testTag("bottom_nav_people")
            )
            NavigationBarItem(
                selected = profileSelected,
                onClick = {
                    if (!profileSelected) haptics.selectionTick()
                    onSelectRoute(Routes.PROFILE)
                },
                colors = itemColors,
                icon = {
                    Icon(
                        imageVector = if (profileSelected) Icons.Rounded.Settings else Icons.Outlined.Settings,
                        contentDescription = "Settings"
                    )
                },
                label = {
                    Text(
                        text = "Settings",
                        fontWeight = if (profileSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                modifier = Modifier.testTag("bottom_nav_profile")
            )
        }
    }
}

@Composable
private fun PulseNavigationRail(
    selectedRoute: String?,
    totalUnread: Long,
    isDark: Boolean,
    onToggleNightMode: () -> Unit,
    onSelectRoute: (String) -> Unit
) {
    val haptics = rememberIosHaptics()
    val railItemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onBackground,
        selectedTextColor = MaterialTheme.colorScheme.onBackground,
        indicatorColor = Color.Transparent,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.background,
        header = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(8.dp))
                TelegramLogoMark(size = 40.dp)
            }
        }
    ) {
        NavigationRailItem(
            selected = selectedRoute == Routes.CHATS,
            onClick = {
                if (selectedRoute != Routes.CHATS) haptics.selectionTick()
                onSelectRoute(Routes.CHATS)
            },
            colors = railItemColors,
            icon = {
                BadgedBox(
                    badge = {
                        if (totalUnread > 0L) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.onBackground,
                                contentColor = MaterialTheme.colorScheme.background
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
            label = { Text("Chats", fontWeight = FontWeight.Medium) },
            modifier = Modifier.testTag("nav_chats")
        )
        NavigationRailItem(
            selected = selectedRoute == Routes.PEOPLE,
            onClick = {
                if (selectedRoute != Routes.PEOPLE) haptics.selectionTick()
                onSelectRoute(Routes.PEOPLE)
            },
            colors = railItemColors,
            icon = {
                Icon(
                    imageVector = if (selectedRoute == Routes.PEOPLE) Icons.Rounded.Groups else Icons.Outlined.Groups,
                    contentDescription = "Contacts"
                )
            },
            label = { Text("Contacts", fontWeight = FontWeight.Medium) },
            modifier = Modifier.testTag("nav_people")
        )
        NavigationRailItem(
            selected = selectedRoute == Routes.PROFILE,
            onClick = {
                if (selectedRoute != Routes.PROFILE) haptics.selectionTick()
                onSelectRoute(Routes.PROFILE)
            },
            colors = railItemColors,
            icon = {
                Icon(
                    imageVector = if (selectedRoute == Routes.PROFILE) Icons.Rounded.Settings else Icons.Outlined.Settings,
                    contentDescription = "Settings"
                )
            },
            label = { Text("Settings", fontWeight = FontWeight.Medium) },
            modifier = Modifier.testTag("nav_profile")
        )

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = {
                haptics.selectionTick()
                onToggleNightMode()
            },
            modifier = Modifier
                .padding(bottom = 12.dp)
                .size(48.dp)
                .testTag("rail_night_mode_toggle")
        ) {
            Icon(
                imageVector = if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                contentDescription = if (isDark) "Switch to light mode" else "Switch to dark mode",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun resolveTargetUserProfile(
    targetUserId: String,
    allPeople: List<UserProfile>,
    allConversations: List<Conversation>,
    currentUserProfile: UserProfile?
): UserProfile? {
    if (targetUserId.isBlank()) return null
    allPeople.firstOrNull { it.userId == targetUserId }?.let { return it }
    if (currentUserProfile?.userId == targetUserId) return currentUserProfile
    val fallbackConv = allConversations.firstOrNull { targetUserId in it.participantIds } ?: return null
    val name = fallbackConv.participantNames[targetUserId]?.takeIf { it.isNotBlank() } ?: "Member"
    val photo = fallbackConv.participantPhotos[targetUserId].orEmpty()
    return UserProfile(
        id = targetUserId,
        userId = targetUserId,
        displayName = name,
        handle = "",
        bio = "",
        photoUrl = photo,
        isOnline = false,
        isDiscoverable = false
    )
}

