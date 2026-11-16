package net.kdt.pojavlaunch.aerix.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.aerix.components.liquidGlass
import net.kdt.pojavlaunch.aerix.theme.AerixSurface

/** Owner selection stays separate from the client installers and their instance data. */
enum class ClientOwnerPage { OWNERS, SAHIL, MX_INSPIRE }

@Composable
fun ClientHubScreen(activity: FragmentActivity, page: ClientOwnerPage, onPageChange: (ClientOwnerPage) -> Unit) {
    val back = { onPageChange(ClientOwnerPage.OWNERS) }
    BackHandler(enabled = page != ClientOwnerPage.OWNERS, onBack = back)
    when (page) {
        ClientOwnerPage.SAHIL -> AerixClientScreen(activity, onBack = back)
        ClientOwnerPage.MX_INSPIRE -> FastClientScreen(activity, onBack = back)
        ClientOwnerPage.OWNERS -> AerixPage(title = "Clients", subtitle = null) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Row(
                        modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        OwnerCard(
                            photo = R.drawable.client_owner_sahil,
                            label = "Aerix client",
                            onClick = { onPageChange(ClientOwnerPage.SAHIL) },
                            modifier = Modifier.weight(1f)
                        )
                        OwnerCard(
                            photo = R.drawable.client_owner_mx_inspire,
                            label = "Fast client 3.0",
                            onClick = { onPageChange(ClientOwnerPage.MX_INSPIRE) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerCard(@DrawableRes photo: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .liquidGlass(shape = shape, tint = AerixSurface.glassTint, strength = 0.90f)
            .clip(shape)
            .background(AerixSurface.panel.copy(alpha = 0.78f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(photo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(76.dp).clip(CircleShape)
                .background(Color.Black)
                .border(1.dp, AerixSurface.accent.copy(alpha = 0.65f), CircleShape)
        )
        Spacer(Modifier.height(9.dp))
        Text(label, color = AerixSurface.textPrimary, fontSize = 12.sp, lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
fun ClientOwnerBackButton(onBack: () -> Unit) {
    TextButton(onClick = onBack) {
        Icon(painterResource(R.drawable.aerix_ic_arrow_back), contentDescription = null,
            tint = AerixSurface.textPrimary, modifier = Modifier.size(18.dp))
        Text("Back", color = AerixSurface.textPrimary)
    }
}
