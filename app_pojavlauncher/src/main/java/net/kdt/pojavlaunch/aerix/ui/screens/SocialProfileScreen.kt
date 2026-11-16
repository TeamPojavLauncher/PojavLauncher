/*
 * Aerix Launcher UI layer for PojavLauncher.
 * One person's profile: banner, logo, counts, bio, ranks - and the edit form for your own.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package net.kdt.pojavlaunch.aerix.ui.screens

import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.aerix.bridge.PojavAccountBridge
import net.kdt.pojavlaunch.aerix.bridge.PojavInstanceBridge
import net.kdt.pojavlaunch.aerix.client.AerixClientCosmetics
import net.kdt.pojavlaunch.aerix.client.AerixClientStore
import net.kdt.pojavlaunch.aerix.components.AerixSkinPreview3D
import net.kdt.pojavlaunch.aerix.social.SocialBackend
import net.kdt.pojavlaunch.aerix.social.SocialPresenceMode
import net.kdt.pojavlaunch.aerix.social.SocialProfile
import net.kdt.pojavlaunch.aerix.social.SocialRanks
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import net.kdt.pojavlaunch.aerix.theme.AerixThemeManager

private enum class ProfileView { CARD, EDIT, PEOPLE, COSMETICS }

private enum class Relation(val title: String) {
    FOLLOWING("Following"),
    FOLLOWERS("Followers"),
    FRIENDS("Friends")
}

/**
 * The profile page, with the edit form and the followers/following lists as pages inside it.
 *
 * @param username whose profile; null means your own
 */
@Composable
fun SocialProfilePage(
    activity: FragmentActivity,
    username: String?,
    onBack: () -> Unit
) {
    var profile by remember(username) { mutableStateOf<SocialProfile?>(null) }
    var loading by remember(username) { mutableStateOf(true) }
    var view by remember(username) { mutableStateOf(ProfileView.CARD) }
    var relation by remember(username) { mutableStateOf(Relation.FOLLOWING) }
    var reportOpen by remember { mutableStateOf(false) }
    var reportText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val isMe = username.isNullOrBlank() || profile?.isMe == true

    suspend fun reload() {
        loading = true
        profile = if (username.isNullOrBlank()) {
            SocialBackend.loadMyProfile()
        } else {
            SocialBackend.loadProfileCard(username)
        }
        loading = false
    }

    LaunchedEffect(username) { reload() }
    BackHandler(enabled = view != ProfileView.CARD) { view = ProfileView.CARD }

    AnimatedContent(
        targetState = view,
        transitionSpec = {
            val forward = targetState.ordinal >= initialState.ordinal
            val enter = slideInHorizontally(tween(230, easing = FastOutSlowInEasing)) { full ->
                if (forward) (full * 0.12f).toInt() else -(full * 0.12f).toInt()
            } + fadeIn(tween(180))
            val exit = slideOutHorizontally(tween(180)) { full ->
                if (forward) -(full * 0.08f).toInt() else (full * 0.08f).toInt()
            } + fadeOut(tween(140))
            enter togetherWith exit
        },
        label = "profileView"
    ) { current ->
        when (current) {
            ProfileView.CARD -> ProfileCard(
                activity = activity,
                profile = profile,
                loading = loading,
                isMe = isMe,
                onBack = onBack,
                onEdit = { view = ProfileView.EDIT },
                onCosmetics = { view = ProfileView.COSMETICS },
                onRelation = { picked ->
                    relation = picked
                    view = ProfileView.PEOPLE
                },
                onFollow = {
                    val target = profile ?: return@ProfileCard
                    scope.launch {
                        if (SocialBackend.setFollow(target.id, !target.iFollow)) reload()
                    }
                },
                onAddFriend = {
                    val target = profile ?: return@ProfileCard
                    scope.launch {
                        if (SocialBackend.addFriendByName(target.username ?: return@launch)) {
                            Toast.makeText(activity, "Friend request sent", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(activity, SocialBackend.lastError ?: "Could not send", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onReport = { reportOpen = true }
            )

            ProfileView.EDIT -> SocialProfileForm(
                activity = activity,
                profile = profile,
                editing = profile?.onboarded == true,
                onClose = { view = ProfileView.CARD },
                onSaved = {
                    view = ProfileView.CARD
                    scope.launch { reload() }
                }
            )

            ProfileView.COSMETICS -> ProfileCosmeticsPage(activity = activity) { view = ProfileView.CARD }

            ProfileView.PEOPLE -> SocialPeoplePage(
                activity = activity,
                relation = relation.name,
                title = relation.title,
                onBack = { view = ProfileView.CARD }
            )
        }
    }

    if (reportOpen) {
        AlertDialog(
            onDismissRequest = { reportOpen = false },
            title = { Text("Report ${profile?.displayName ?: "this player"}") },
            text = {
                Column {
                    Text(
                        "Tell us what happened. Only the launcher team can read this.",
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    SocialGlassInput(
                        value = reportText,
                        onValueChange = { reportText = it.take(500) },
                        label = "What happened?"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = profile
                    reportOpen = false
                    if (target != null && reportText.trim().length >= 3) {
                        scope.launch {
                            val sent = SocialBackend.report(target.id, reportText.trim())
                            Toast.makeText(
                                activity,
                                if (sent) "Report sent" else "Could not send the report",
                                Toast.LENGTH_SHORT
                            ).show()
                            reportText = ""
                        }
                    }
                }) { Text("Send") }
            },
            dismissButton = {
                TextButton(onClick = { reportOpen = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProfileCard(
    activity: FragmentActivity,
    profile: SocialProfile?,
    loading: Boolean,
    isMe: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCosmetics: () -> Unit,
    onRelation: (Relation) -> Unit,
    onFollow: () -> Unit,
    onAddFriend: () -> Unit,
    onReport: () -> Unit
) {
    val accent = AerixThemeManager.currentAccent()
    var statusDialogOpen by remember { mutableStateOf(false) }
    val effectiveRole = if (isMe) (SocialBackend.myRole ?: profile?.role) else profile?.role

    if (statusDialogOpen && isMe) {
        SocialStatusDialog(
            onDismiss = { statusDialogOpen = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AerixSpacing.lg)
    ) {
        SocialPageBar(
            title = profile?.displayName ?: if (loading) "Profile" else "Profile",
            role = effectiveRole,
            onBack = onBack,
            trailing = {
                if (isMe) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SocialButton(
                            label = "Change status",
                            filled = false,
                            onClick = { statusDialogOpen = true }
                        )
                        val incomplete = profile?.onboarded != true
                        SocialButton(
                            label = if (incomplete) "Finish setup" else "Edit profile",
                            filled = incomplete,
                            onClick = onEdit
                        )
                    }
                }
            }
        )
        Spacer(Modifier.height(AerixSpacing.sm))

        if (profile == null) {
            SocialPanel(Modifier.fillMaxWidth()) {
                Text(
                    if (loading) "Loading the profile…" else "This profile could not be opened.",
                    fontSize = 12.sp,
                    color = AerixSurface.textSecondary,
                    modifier = Modifier.padding(18.dp)
                )
            }
            return@Column
        }

        // Banner, with the round logo sitting half over its bottom edge.
        Box(modifier = Modifier.fillMaxWidth()) {
            SocialRemoteImage(
                url = profile.bannerUrl,
                fallbackIcon = R.drawable.aerix_ic_format_paint_outlined,
                modifier = Modifier.fillMaxWidth().height(96.dp),
                shape = RoundedCornerShape(AerixRadii.panel),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = AerixSpacing.md, bottom = 0.dp)
                    .size(72.dp)
            ) {
                SocialRemoteImage(
                    url = profile.avatarUrl,
                    fallbackIcon = R.drawable.aerix_ic_person_outlined,
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape
                )
                val avatarMode = when {
                    isMe -> SocialBackend.effectivePresenceMode
                    profile.online -> profile.presenceMode
                    else -> null
                }
                SocialStatusDot(
                    mode = avatarMode,
                    size = 15.dp,
                    borderWidth = 2.dp,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
        Spacer(Modifier.height(AerixSpacing.lgPlus))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(0.67f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        SocialNameWithRole(
                            name = profile.displayName,
                            role = effectiveRole,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AerixSurface.textPrimary
                        )
                        Text(
                            profile.handle.ifBlank { "@" + (profile.mcUsername ?: "player") },
                            fontSize = 11.sp,
                            color = AerixSurface.textMuted
                        )
                    }
                    if (!profile.isMe) {
                        SocialButton(if (profile.iFollow) "Following" else "Follow", filled = !profile.iFollow, onClick = onFollow)
                    }
                }

                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(AerixSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!profile.country.isNullOrBlank()) SocialTag(profile.country, AerixSurface.glassBlue)
                    val myEffMode = SocialBackend.effectivePresenceMode
                    val activeLooking = if (isMe) SocialBackend.myLookingToPlay else profile.lookingToPlay
                    if (activeLooking && (!isMe || (myEffMode != null && myEffMode != SocialPresenceMode.INVISIBLE))) {
                        SocialTag("Looking to play", AerixSurface.success)
                    }
                    if (isMe) {
                        val tagLabel = when {
                            myEffMode == null -> "Offline"
                            SocialBackend.isAutoPresence -> "${myEffMode.label} (Auto)"
                            else -> myEffMode.label
                        }
                        Box(modifier = Modifier.clickable { statusDialogOpen = true }) {
                            SocialTag(tagLabel, presenceModeColor(myEffMode))
                        }
                    } else if (profile.online) {
                        SocialTag(profile.presenceMode.label, presenceModeColor(profile.presenceMode))
                    } else if (profile.away) {
                        SocialTag("Away", AerixSurface.warning)
                    } else {
                        SocialTag("Offline", AerixSurface.textMuted)
                    }
                }
                val shownStatus = if (isMe) (SocialBackend.myCustomStatus ?: profile.statusText) else profile.statusText
                if (!shownStatus.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        shownStatus,
                        fontSize = 11.5.sp,
                        color = AerixSurface.textSecondary,
                        modifier = if (isMe) Modifier.clickable { statusDialogOpen = true } else Modifier
                    )
                }

            }
            if (isMe) {
                ProfileCharacter(
                    modifier = Modifier.weight(0.24f, fill = false).widthIn(max = 148.dp)
                        .fillMaxWidth().height(130.dp),
                    onClick = onCosmetics
                )
                Spacer(Modifier.weight(0.09f))
            }
        }

        Spacer(Modifier.height(AerixSpacing.sm))

        // The three counts, side by side, each one opening its list.
        SocialPanel(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CountCell(profile.following, Relation.FOLLOWING.title) { onRelation(Relation.FOLLOWING) }
                CountCell(profile.followers, Relation.FOLLOWERS.title) { onRelation(Relation.FOLLOWERS) }
                CountCell(profile.friends, Relation.FRIENDS.title) { onRelation(Relation.FRIENDS) }
            }
        }

        Spacer(Modifier.height(AerixSpacing.sm))

        SocialPanel(Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Bio", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AerixSurface.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    profile.bio?.takeIf { it.isNotBlank() }
                        ?: "No bio yet.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = if (profile.bio.isNullOrBlank()) AerixSurface.textMuted else AerixSurface.textSecondary
                )
            }
        }

        if (profile.ranks.isNotEmpty()) {
            Spacer(Modifier.height(AerixSpacing.sm))
            SocialPanel(Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Ranks", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AerixSurface.textPrimary)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(AerixSpacing.xs)) {
                        profile.ranks.take(4).forEach { key ->
                            val rank = SocialRanks.of(key)
                            SocialTag(rank.label, Color(rank.tint))
                        }
                    }
                    if (profile.ranks.size > 4) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "+${profile.ranks.size - 4} more",
                            fontSize = 9.sp,
                            color = AerixSurface.textMuted
                        )
                    }
                }
            }
        }

        if (!profile.skinUrl.isNullOrBlank() || !profile.capeUrl.isNullOrBlank()) {
            Spacer(Modifier.height(AerixSpacing.sm))
            SocialPanel(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(AerixSpacing.md)
                ) {
                    if (!profile.skinUrl.isNullOrBlank()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SocialRemoteImage(
                                url = profile.skinUrl,
                                fallbackIcon = R.drawable.aerix_ic_person_outlined,
                                modifier = Modifier.size(56.dp),
                                shape = RoundedCornerShape(AerixRadii.control),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                            Text("Skin", fontSize = 9.sp, color = AerixSurface.textMuted)
                        }
                    }
                    if (!profile.capeUrl.isNullOrBlank()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SocialRemoteImage(
                                url = profile.capeUrl,
                                fallbackIcon = R.drawable.aerix_ic_format_paint_outlined,
                                modifier = Modifier.size(56.dp),
                                shape = RoundedCornerShape(AerixRadii.control),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                            Text("Cape", fontSize = 9.sp, color = AerixSurface.textMuted)
                        }
                    }
                }
            }
        }

        if (!profile.isMe) {
            val scope = rememberCoroutineScope()
            val friendEntry = SocialBackend.friends.firstOrNull { it.id == profile.id && !it.pendingIncoming && !it.pendingOutgoing }
            val blockedNow = SocialBackend.isBlocked(profile.id)
            var mutualInfo by remember(profile.id) { mutableStateOf(0 to emptyList<String>()) }
            var nicknameDialogOpen by remember { mutableStateOf(false) }
            var nicknameDraft by remember(profile.id) { mutableStateOf(SocialBackend.getFriendNickname(profile.id).orEmpty()) }
            var confirmBlockOpen by remember { mutableStateOf(false) }

            LaunchedEffect(profile.id) {
                mutualInfo = SocialBackend.loadMutualFriends(profile.id)
            }

            if (mutualInfo.first > 0) {
                Spacer(Modifier.height(AerixSpacing.xs))
                val preview = mutualInfo.second.take(3).joinToString(", ")
                SocialTag(
                    text = if (preview.isNotEmpty()) "${mutualInfo.first} mutual: $preview" else "${mutualInfo.first} mutual friends",
                    tint = AerixThemeManager.currentAccent()
                )
            }

            Spacer(Modifier.height(AerixSpacing.sm))
            Row(
                modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
            ) {
                if (friendEntry == null && !blockedNow) {
                    SocialButton("Add friend", filled = true, onClick = onAddFriend)
                }
                if (friendEntry != null) {
                    SocialButton(
                        label = if (friendEntry.pinned) "★ Pinned" else "☆ Favourite",
                        filled = friendEntry.pinned
                    ) {
                        SocialBackend.toggleFriendPin(profile.id)
                    }
                    SocialButton(
                        label = if (!friendEntry.nickname.isNullOrBlank()) "Nickname: ${friendEntry.nickname}" else "Set Nickname",
                        filled = false
                    ) {
                        nicknameDraft = SocialBackend.getFriendNickname(profile.id).orEmpty()
                        nicknameDialogOpen = true
                    }
                }
                SocialButton(
                    label = if (blockedNow) "Unblock" else "Block",
                    filled = false
                ) {
                    if (blockedNow) {
                        scope.launch {
                            if (SocialBackend.unblock(profile.id)) {
                                Toast.makeText(activity, "Unblocked ${profile.displayName}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        confirmBlockOpen = true
                    }
                }
                SocialButton("Report", filled = false, onClick = onReport)
            }

            if (nicknameDialogOpen) {
                AlertDialog(
                    onDismissRequest = { nicknameDialogOpen = false },
                    title = { Text("Nickname for ${profile.displayName}") },
                    text = {
                        SocialGlassInput(
                            value = nicknameDraft,
                            onValueChange = { nicknameDraft = it.take(32) },
                            label = "Custom nickname (leave empty to reset)"
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            SocialBackend.setFriendNickname(profile.id, nicknameDraft)
                            nicknameDialogOpen = false
                        }) { Text("Save") }
                    },
                    dismissButton = {
                        TextButton(onClick = { nicknameDialogOpen = false }) { Text("Cancel") }
                    }
                )
            }

            if (confirmBlockOpen) {
                AlertDialog(
                    onDismissRequest = { confirmBlockOpen = false },
                    title = { Text("Block ${profile.displayName}?") },
                    text = {
                        Text(
                            "Blocking removes your friendship and stops direct messages and friend requests in both directions.",
                            fontSize = 12.sp
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmBlockOpen = false
                            scope.launch {
                                val ok = SocialBackend.block(profile.id)
                                Toast.makeText(
                                    activity,
                                    if (ok) "Blocked ${profile.displayName}" else "Could not block player",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }) { Text("Block", color = AerixSurface.danger) }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmBlockOpen = false }) { Text("Cancel") }
                    }
                )
            }
        }
        if (isMe) {
            Spacer(Modifier.height(AerixSpacing.sm))
            SocialPanel(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Invite code",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AerixSurface.textPrimary
                        )
                        Text(
                            SocialBackend.inviteCode() ?: "—",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AerixThemeManager.currentAccent()
                        )
                    }
                    SocialButton("Copy link", filled = false) {
                        val code = SocialBackend.inviteCode() ?: return@SocialButton
                        val clipboard = activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                as android.content.ClipboardManager
                        clipboard.setPrimaryClip(
                            android.content.ClipData.newPlainText(
                                "Aerix invite",
                                "aerix-social://friend/$code"
                            )
                        )
                        Toast.makeText(activity, "Invite link copied", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        Spacer(Modifier.height(AerixSpacing.sm))
        Text(
            if (isMe) "Your profile is only reachable by your exact username or this code." else
                "Found by username only — this profile is not listed anywhere.",
            fontSize = 9.sp,
            color = AerixSurface.textMuted
        )
    }
}

@Composable
private fun ProfileCharacter(modifier: Modifier, onClick: () -> Unit) {
    val account = PojavAccountBridge.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val clientCape = PojavInstanceBridge.selected?.isAerixClient == true
    var azimuth by remember(account?.key) { mutableFloatStateOf(25f) }
    var pitch by remember(account?.key) { mutableFloatStateOf(10f) }
    var clientReady by remember(clientCape) { mutableStateOf(!clientCape) }
    LaunchedEffect(clientCape) {
        if (clientCape) AerixClientStore.init(context)
        clientReady = true
    }
    Box(
        modifier = modifier.clipToBounds()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Rotate character or tap to change skin and cape" }
            .pointerInput(account?.key) {
                detectDragGestures { change, amount ->
                    change.consume()
                    azimuth = (azimuth + amount.x * 0.55f).mod(360f)
                    pitch = (pitch - amount.y * 0.35f).coerceIn(-35f, 55f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (clientReady) {
            AerixSkinPreview3D(
                account = account,
                azimuth = azimuth.roundToInt(),
                pitch = pitch.roundToInt(),
                interactionEnabled = false,
                useClientCape = clientCape,
                showOfficialCape = true,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
internal fun ProfileCosmeticsPage(activity: FragmentActivity, setupFlow: Boolean = false, onBack: () -> Unit) {
    val account = PojavAccountBridge.current
    val selectedClient = PojavInstanceBridge.selected?.isAerixClient == true
    val scope = rememberCoroutineScope()
    var cloudBusy by remember { mutableStateOf(false) }
    var cloudAction by remember { mutableStateOf<String?>(null) }
    var gameAccount by remember { mutableStateOf<SocialBackend.AerixGameAccount?>(null) }
    var gameLoaded by remember { mutableStateOf(false) }
    var gameError by remember { mutableStateOf<String?>(null) }
    var gameDialog by remember { mutableStateOf<String?>(null) }
    var gameNameDraft by remember { mutableStateOf("") }
    var skinTarget by remember { mutableStateOf<net.kdt.pojavlaunch.aerix.bridge.AerixAccount?>(null) }
    var capeTarget by remember { mutableStateOf<net.kdt.pojavlaunch.aerix.bridge.AerixAccount?>(null) }
    val skinPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = skinTarget
        skinTarget = null
        if (uri != null && target != null) PojavAccountBridge.equipSkinFromUri(activity, target, uri) { ok ->
            if (ok && setupFlow && PojavAccountBridge.current?.key == target.key) cloudAction = "upload-skin"
        }
    }
    val capePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = capeTarget
        capeTarget = null
        if (uri != null && target != null) PojavAccountBridge.equipCapeFromUri(activity, target, uri) { ok ->
            if (ok && setupFlow && PojavAccountBridge.current?.key == target.key) cloudAction = "upload-cape"
        }
    }
    LaunchedEffect(Unit) {
        AerixClientStore.init(activity)
        if (SocialBackend.signedIn) SocialBackend.loadMyProfile()
    }

    LaunchedEffect(SocialBackend.signedIn, SocialBackend.myUserId()) {
        gameAccount = null
        gameLoaded = false
        gameError = null
        if (SocialBackend.signedIn) {
            val owner = SocialBackend.myUserId()
            SocialBackend.clearError()
            val loaded = SocialBackend.loadGameAccount()
            if (SocialBackend.myUserId() == owner) {
                gameAccount = loaded
                gameError = SocialBackend.lastError
                gameLoaded = true
            }
        }
    }

    gameDialog?.let { action ->
        val claiming = action == "claim"
        AlertDialog(
            onDismissRequest = { gameDialog = null },
            title = { Text(if (claiming) "Choose your Aerix game name" else "Share your game appearance?") },
            text = {
                if (claiming) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your game name and UUID are public and the name is permanent. You can use an existing offline name to keep its offline UUID. Aerix servers verify your account; Microsoft servers still require Microsoft ownership.")
                    SocialGlassInput(value = gameNameDraft, onValueChange = { gameNameDraft = it.take(16) }, label = "Game name", supporting = "3–16 ASCII letters, numbers, or underscores")
                } else Text("Anyone who knows your game UUID can look up your published skin and cape, and the image URLs include your Aerix user ID. Other players need a compatible client.")
            },
            confirmButton = {
                TextButton(enabled = !claiming || gameNameDraft.matches(Regex("^[A-Za-z0-9_]{3,16}$")), onClick = {
                    gameDialog = null
                    val owner = SocialBackend.myUserId() ?: return@TextButton
                    val chosenName = gameNameDraft
                    cloudBusy = true
                    SocialBackend.clearError()
                    scope.launch {
                        val success = if (claiming) {
                            val claimed = SocialBackend.claimGameAccount(chosenName, owner)
                            if (SocialBackend.myUserId() == owner) gameAccount = claimed
                            if (claimed != null && setupFlow && SocialBackend.myUserId() == owner) {
                                net.kdt.pojavlaunch.authenticator.AuthType.AERIX.createAuth().createAccount(PojavAccountBridge, "")
                            }
                            claimed != null
                        } else if (!claiming) {
                            SocialBackend.setGameSharing(true, owner).also { ok ->
                                if (ok && SocialBackend.myUserId() == owner) gameAccount = SocialBackend.loadGameAccount()
                            }
                        } else false
                        cloudBusy = false
                        if (!success) Toast.makeText(activity, SocialBackend.lastError ?: "Could not save; try again", Toast.LENGTH_LONG).show()
                    }
                }) { Text(if (claiming) "Claim permanently" else "Share") }
            },
            dismissButton = { TextButton(onClick = { gameDialog = null }) { Text("Cancel") } }
        )
    }

    cloudAction?.let { action ->
        val uploading = action.startsWith("upload-")
        val kind = if (action.endsWith("cape")) SocialBackend.CloudTexture.CAPE else SocialBackend.CloudTexture.SKIN
        val label = if (kind == SocialBackend.CloudTexture.SKIN) "skin" else "cape"
        AlertDialog(
            onDismissRequest = { cloudAction = null },
            title = { Text(if (uploading) "Publish $label to Aerix?" else "Apply Aerix profile $label?") },
            text = {
                Text(if (uploading)
                    "Your selected $label will be uploaded to public Aerix storage and used on your social profile. If game appearance sharing is enabled, compatible players can also see it."
                else "This backs up and replaces the selected Minecraft account's local $label. The account and worlds are not changed.")
            },
            confirmButton = {
                TextButton(onClick = {
                    cloudAction = null
                    val target = account ?: return@TextButton
                    val owner = SocialBackend.myUserId()
                    val slim = PojavAccountBridge.isSlimModelFor(activity, target.source)
                    cloudBusy = true
                    SocialBackend.clearError()
                    scope.launch {
                        if (uploading) {
                            val bytes = withContext(Dispatchers.IO) {
                                val file = if (kind == SocialBackend.CloudTexture.SKIN)
                                    PojavAccountBridge.customSkinFile(target.source)
                                else PojavAccountBridge.customCapeFile(target.source)
                                if (!file.isFile || file.length() <= 0 || file.length() > 2 * 1024 * 1024) null
                                else runCatching { file.readBytes() }.getOrNull()
                            }
                            val saved = bytes != null && owner != null &&
                                SocialBackend.publishGameTexture(bytes, owner, kind, slim)
                            cloudBusy = false
                            if (saved && setupFlow && gameAccount?.sharing == false) gameDialog = "share"
                            Toast.makeText(activity, if (saved) "$label published to your Aerix profile"
                                else SocialBackend.lastError ?: "Equip a valid custom $label first", Toast.LENGTH_LONG).show()
                        } else {
                            val appearance = SocialBackend.downloadOwnGameTexture(kind)
                            if (appearance == null || owner == null || SocialBackend.myUserId() != owner ||
                                PojavAccountBridge.current?.key != target.key) {
                                cloudBusy = false
                                Toast.makeText(activity, SocialBackend.lastError ?: "Account changed; try again", Toast.LENGTH_LONG).show()
                            } else {
                                val temp = withContext(Dispatchers.IO) {
                                    runCatching {
                                        java.io.File.createTempFile("aerix-cloud-$label-", ".png", activity.cacheDir)
                                            .apply { writeBytes(appearance.bytes) }
                                    }.getOrNull()
                                }
                                val backedUp = if (temp == null) false else withContext(Dispatchers.IO) {
                                    runCatching {
                                        val previous = if (kind == SocialBackend.CloudTexture.SKIN)
                                            PojavAccountBridge.customSkinFile(target.source)
                                        else PojavAccountBridge.customCapeFile(target.source)
                                        if (previous.isFile) {
                                            val dir = java.io.File(PojavAccountBridge.skinsDirectory(), "cloud-backups")
                                            if (!dir.isDirectory && !dir.mkdirs()) throw java.io.IOException("Cannot create backup directory")
                                            val backup = java.io.File(dir, "Before_Aerix_${label}_${java.util.UUID.randomUUID()}.png")
                                            previous.copyTo(backup, overwrite = false)
                                        }
                                        true
                                    }.getOrDefault(false)
                                }
                                if (temp == null || !backedUp || SocialBackend.myUserId() != owner ||
                                    PojavAccountBridge.current?.key != target.key) {
                                    temp?.delete()
                                    cloudBusy = false
                                    Toast.makeText(activity, "Could not safely restore; nothing was changed", Toast.LENGTH_LONG).show()
                                } else if (kind == SocialBackend.CloudTexture.SKIN) {
                                    PojavAccountBridge.equipSkinFromUri(activity, target, Uri.fromFile(temp)) { ok ->
                                        if (ok && appearance.slim != null) PojavAccountBridge.setSlimModel(activity, target, appearance.slim)
                                        temp.delete()
                                        cloudBusy = false
                                    }
                                } else {
                                    PojavAccountBridge.equipCapeFromUri(activity, target, Uri.fromFile(temp)) {
                                        temp.delete()
                                        cloudBusy = false
                                    }
                                }
                            }
                        }
                    }
                }) { Text(if (uploading) "Publish" else "Apply") }
            },
            dismissButton = { TextButton(onClick = { cloudAction = null }) { Text("Cancel") } }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AerixSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
    ) {
        SocialPageBar(title = if (setupFlow) "Aerix account" else "Skin & Cape", onBack = onBack)
        if (setupFlow) Text("Choose a game name, sign in for Minecraft, then select and publish your skin and cape. Enable appearance sharing to show them on compatible servers.", fontSize = 11.sp, color = AerixSurface.textSecondary)
        SocialPanel(Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Aerix game account", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AerixSurface.textPrimary)
                if (!SocialBackend.signedIn) {
                    Text("Sign in to Aerix to create an authenticated game account.", fontSize = 11.sp, color = AerixSurface.textSecondary)
                } else if (!gameLoaded) {
                    Text("Loading game account…", fontSize = 11.sp, color = AerixSurface.textSecondary)
                } else if (gameError != null) {
                    Text("Could not load game account: $gameError. Reopen this page to retry.", fontSize = 11.sp, color = AerixSurface.textSecondary)
                } else if (gameAccount == null) {
                    Text("Choose a permanent game name, then sign in as an Aerix-authenticated account.", fontSize = 11.sp, color = AerixSurface.textSecondary)
                    SocialButton("Choose Aerix game name", filled = false, enabled = !cloudBusy) {
                        gameNameDraft = account?.username.orEmpty().take(16)
                        gameDialog = "claim"
                    }
                } else {
                    val identity = gameAccount!!
                    Text("Name: ${identity.name}  •  ${if (identity.sharing) "Appearance shared" else "Appearance private"}",
                        fontSize = 11.sp, color = AerixSurface.textSecondary)
                    SocialButton("Sign in with Aerix", filled = false, enabled = !cloudBusy) {
                        net.kdt.pojavlaunch.authenticator.AuthType.AERIX.createAuth().createAccount(PojavAccountBridge, "")
                    }
                    SocialButton(if (identity.sharing) "Stop sharing game appearance" else "Share game appearance",
                        filled = false, enabled = !cloudBusy) {
                        if (!identity.sharing) gameDialog = "share"
                        else {
                            val owner = SocialBackend.myUserId()
                            if (owner != null) {
                                cloudBusy = true
                                scope.launch {
                                    val ok = SocialBackend.setGameSharing(false, owner)
                                    if (ok && SocialBackend.myUserId() == owner) gameAccount = SocialBackend.loadGameAccount()
                                    cloudBusy = false
                                    if (!ok) Toast.makeText(activity, SocialBackend.lastError ?: "Could not stop sharing", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                    Text("Works on servers configured for Aerix authentication. Microsoft-authenticated servers still require a Microsoft account.",
                        fontSize = 11.sp, color = AerixSurface.textSecondary)
                }
            }
        }
        if (account == null) {
            SocialPanel(Modifier.fillMaxWidth()) {
                Text("Select a Minecraft account to change your skin and cape.",
                    color = AerixSurface.textSecondary, modifier = Modifier.padding(16.dp))
            }
            return@Column
        }
        if (setupFlow && (account.authType != net.kdt.pojavlaunch.authenticator.AuthType.AERIX ||
                account.source.aerixOwnerId != SocialBackend.myUserId())) {
            SocialPanel(Modifier.fillMaxWidth()) {
                Text("Tap Sign in with Aerix above to select this game account before choosing a skin or cape.",
                    color = AerixSurface.textSecondary, modifier = Modifier.padding(16.dp))
            }
            return@Column
        }
        SocialPanel(Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Skin", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AerixSurface.textPrimary)
                if (setupFlow) {
                    val revision = PojavAccountBridge.wardrobeRevision
                    val slim = remember(account.key, revision) { PojavAccountBridge.isSlimModelFor(activity, account.source) }
                    Text("Arm model: ${if (slim) "Slim" else "Wide"}", fontSize = 11.sp, color = AerixSurface.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SocialButton("Wide", filled = !slim) {
                            PojavAccountBridge.setSlimModel(activity, account, false)
                            if (slim && PojavAccountBridge.customSkinFile(account.source).isFile) cloudAction = "upload-skin"
                        }
                        SocialButton("Slim", filled = slim) {
                            PojavAccountBridge.setSlimModel(activity, account, true)
                            if (!slim && PojavAccountBridge.customSkinFile(account.source).isFile) cloudAction = "upload-skin"
                        }
                    }
                }
                Text("Choose a Minecraft skin image for ${account.username}.", fontSize = 11.sp, color = AerixSurface.textSecondary)
                SocialButton("Change skin", filled = false) {
                    skinTarget = account
                    runCatching { skinPicker.launch(arrayOf("image/png")) }
                        .onFailure { skinTarget = null; Toast.makeText(activity, "Unable to open file picker", Toast.LENGTH_SHORT).show() }
                }
                Text("Aerix account: publish this skin to your profile or restore it on this device. " +
                    "Published images are public. Skins published here include your Wide or Slim choice.",
                    fontSize = 11.sp, color = AerixSurface.textSecondary)
                SocialButton(if (cloudBusy) "Working…" else "Publish current skin", filled = false,
                    enabled = SocialBackend.signedIn && !cloudBusy) { cloudAction = "upload-skin" }
                SocialButton("Apply profile skin", filled = false,
                    enabled = SocialBackend.signedIn && !cloudBusy) { cloudAction = "download-skin" }
            }
        }
        SocialPanel(Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cape", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AerixSurface.textPrimary)
                Text(
                    if (selectedClient) "Aerix Client uses the selected Client cape below. An imported cape is used by other instances."
                    else "Import a cape for this account, or choose an Aerix Client cape below.",
                    fontSize = 11.sp, color = AerixSurface.textSecondary
                )
                SocialButton("Import cape PNG", filled = false) {
                    capeTarget = account
                    runCatching { capePicker.launch(arrayOf("image/png")) }
                        .onFailure { capeTarget = null; Toast.makeText(activity, "Unable to open file picker", Toast.LENGTH_SHORT).show() }
                }
                Text("Publish or restore your imported cape with your Aerix account. Save or restore the animated Client cape choice separately below.",
                    fontSize = 11.sp, color = AerixSurface.textSecondary)
                SocialButton(if (cloudBusy) "Working…" else "Publish imported cape", filled = false,
                    enabled = SocialBackend.signedIn && !cloudBusy) { cloudAction = "upload-cape" }
                SocialButton("Apply profile cape", filled = false,
                    enabled = SocialBackend.signedIn && !cloudBusy) { cloudAction = "download-cape" }
                Text("Aerix Client capes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AerixSurface.textPrimary)
                Text("Save or restore your animated Client cape choice separately from the imported cape image.",
                    fontSize = 11.sp, color = AerixSurface.textSecondary)
                SocialButton("Save Client cape to Aerix", filled = false,
                    enabled = SocialBackend.signedIn && gameAccount != null && !cloudBusy) {
                    val owner = SocialBackend.myUserId()
                    if (owner != null) {
                        val capeId = AerixClientStore.equippedCapeId
                        cloudBusy = true
                        SocialBackend.clearError()
                        scope.launch {
                            val saved = SocialBackend.saveClientCape(capeId, owner)
                            cloudBusy = false
                            Toast.makeText(activity, if (saved) "Client cape saved" else SocialBackend.lastError ?: "Unable to save Client cape", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                SocialButton("Restore Client cape", filled = false,
                    enabled = SocialBackend.signedIn && gameAccount != null && !cloudBusy) {
                    val owner = SocialBackend.myUserId()
                    if (owner != null) {
                        cloudBusy = true
                        SocialBackend.clearError()
                        scope.launch {
                            val id = SocialBackend.loadClientCape(owner)
                            val cape = AerixClientCosmetics.capes.firstOrNull { it.id == id }
                            if (SocialBackend.myUserId() == owner && cape != null && AerixClientCosmetics.canEquipCape(cape)) {
                                AerixClientStore.equippedCapeId = cape.id
                                AerixClientStore.persist()
                                net.kdt.pojavlaunch.PojavApplication.sExecutorService.execute {
                                    synchronized(AerixClientCosmetics) {
                                        AerixClientCosmetics.syncCapeAssetsToAllClientInstances(activity.applicationContext)
                                    }
                                }
                                Toast.makeText(activity, "Client cape restored", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(activity, SocialBackend.lastError ?: "No available Client cape saved", Toast.LENGTH_LONG).show()
                            }
                            cloudBusy = false
                        }
                    }
                }
                SocialButton(
                    label = "No cape" + if (AerixClientStore.equippedCapeId == AerixClientStore.CAPE_NONE) " ✓" else "",
                    filled = AerixClientStore.equippedCapeId == AerixClientStore.CAPE_NONE
                ) {
                    AerixClientStore.equippedCapeId = AerixClientStore.CAPE_NONE
                    AerixClientStore.persist()
                    val context = activity.applicationContext
                    net.kdt.pojavlaunch.PojavApplication.sExecutorService.execute {
                        synchronized(AerixClientCosmetics) {
                            AerixClientCosmetics.syncCapeAssetsToAllClientInstances(context)
                        }
                    }
                }
                AerixClientCosmetics.capes.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { cape ->
                            val unlocked = AerixClientCosmetics.canEquipCape(cape)
                            val selected = AerixClientCosmetics.effectiveCapeSpec()?.id == cape.id
                            SocialButton(
                                label = cape.title + if (!unlocked) " (locked)" else if (selected) " ✓" else "",
                                modifier = Modifier.weight(1f), filled = selected, enabled = unlocked
                            ) {
                                if (AerixClientCosmetics.canEquipCape(cape)) {
                                    AerixClientStore.equippedCapeId = cape.id
                                    AerixClientStore.persist()
                                    val context = activity.applicationContext
                                    net.kdt.pojavlaunch.PojavApplication.sExecutorService.execute {
                                        synchronized(AerixClientCosmetics) {
                                            AerixClientCosmetics.syncCapeAssetsToAllClientInstances(context)
                                        }
                                    }
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CountCell(count: Int, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .clickable(onClick = onClick)
    ) {
        Text("$count", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = AerixSurface.textPrimary)
        Text(label, fontSize = 10.sp, color = AerixSurface.textMuted)
    }
}

@Composable
private fun SocialPeoplePage(
    activity: FragmentActivity,
    relation: String,
    title: String,
    onBack: () -> Unit
) {
    var people by remember(relation) { mutableStateOf<List<SocialProfile>>(emptyList()) }
    var loading by remember(relation) { mutableStateOf(true) }
    var openUser by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(relation) {
        loading = true
        people = when (relation) {
            "FOLLOWERS" -> SocialBackend.loadFollowers()
            "FOLLOWING" -> SocialBackend.loadFollowing()
            else -> SocialBackend.friends.filter { !it.pendingIncoming && !it.pendingOutgoing }.map { friend ->
                SocialProfile(
                    id = friend.id,
                    username = friend.username ?: friend.mcUsername,
                    name = friend.effectiveName,
                    mcUsername = friend.mcUsername,
                    avatarUrl = friend.avatarUrl,
                    lastSeenMs = friend.lastSeenMs,
                    role = friend.effectiveRole,
                    presenceMode = friend.presenceMode
                )
            }
        }
        loading = false
    }

    if (openUser != null) {
        SocialProfilePage(activity = activity, username = openUser) { openUser = null }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(AerixSpacing.lg)) {
        SocialPageBar(title = title, onBack = onBack)
        Spacer(Modifier.height(AerixSpacing.sm))
        if (loading) {
            Text("Loading…", fontSize = 11.sp, color = AerixSurface.textMuted)
            return@Column
        }
        if (people.isEmpty()) {
            SocialPanel(Modifier.fillMaxWidth()) {
                Text(
                    "Nobody here yet.",
                    fontSize = 11.sp,
                    color = AerixSurface.textSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }
            return@Column
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(AerixSpacing.xs)) {
            itemsIndexed(people) { _, person ->
                SocialPersonRow(
                    avatarUrl = person.avatarUrl,
                    title = person.displayName,
                    subtitle = person.handle.ifBlank { person.mcUsername },
                    badge = if (person.online) person.presenceMode.label else null,
                    role = person.role,
                    onClick = { person.username?.let { openUser = it } }
                )
            }
        }
    }
}
