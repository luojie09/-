package com.secretbase.app.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.secretbase.app.data.message.SecretBaseUsers
import com.secretbase.app.ui.common.SecretBaseCardSurface
import com.secretbase.app.ui.common.SecretBaseInputSurface
import com.secretbase.app.ui.common.SecretBasePageBackground
import com.secretbase.app.ui.common.SecretBasePageTopBar
import com.secretbase.app.ui.common.SecretBasePrimaryButton
import com.secretbase.app.ui.theme.CherryPink
import com.secretbase.app.ui.theme.InkBlack
import com.secretbase.app.ui.theme.SoftPink
import com.secretbase.app.ui.theme.SurfaceWhite
import com.secretbase.app.ui.theme.WarmGray

@Composable
fun IdentitySettingsScreen(
    currentUserId: String,
    coupleId: String?,
    @DrawableRes sheepAvatarRes: Int?,
    @DrawableRes chickAvatarRes: Int?,
    requiresPairingCode: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSwitchIdentity: (String, String) -> Unit,
) {
    var selectedUserId by rememberSaveable(currentUserId) { mutableStateOf(currentUserId) }
    var pairingCode by rememberSaveable { mutableStateOf("") }
    val avatarFor: (String) -> Int? = { userId ->
        if (userId == SecretBaseUsers.SHEEP_ID) sheepAvatarRes else chickAvatarRes
    }

    SecretBasePageBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            SecretBasePageTopBar(
                title = "身份设置",
                onBack = onBack,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                SecretBaseCardSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        IdentityAvatar(
                            avatarRes = avatarFor(currentUserId),
                            fallback = if (currentUserId == SecretBaseUsers.SHEEP_ID) "🐑" else "🐥",
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "当前是${SecretBaseUsers.nameFor(currentUserId)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = InkBlack,
                            )
                            Text(
                                text = coupleId?.let { "已连接 · ${it.take(8)}" } ?: "本地预览模式",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarmGray,
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "选择这台设备的身份",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = InkBlack,
                    )
                    IdentityChoice(
                        name = "小羊",
                        avatarRes = sheepAvatarRes,
                        fallback = "🐑",
                        selected = selectedUserId == SecretBaseUsers.SHEEP_ID,
                        onClick = { selectedUserId = SecretBaseUsers.SHEEP_ID },
                    )
                    IdentityChoice(
                        name = "小耶",
                        avatarRes = chickAvatarRes,
                        fallback = "🐥",
                        selected = selectedUserId == SecretBaseUsers.CHICK_ID,
                        onClick = { selectedUserId = SecretBaseUsers.CHICK_ID },
                    )
                }

                if (requiresPairingCode) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "配对码",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = InkBlack,
                        )
                        SecretBaseInputSurface(modifier = Modifier.fillMaxWidth()) {
                            BasicTextField(
                                value = pairingCode,
                                onValueChange = { pairingCode = it.take(64) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = InkBlack),
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (pairingCode.isEmpty()) {
                                        Text(
                                            text = "输入配对码以确认切换",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = WarmGray,
                                        )
                                    }
                                    inner()
                                },
                            )
                        }
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CherryPink,
                    )
                }

                SecretBasePrimaryButton(
                    text = if (isSaving) "正在确认..." else "保存身份",
                    enabled = !isSaving && (!requiresPairingCode || pairingCode.isNotBlank()),
                    onClick = { onSwitchIdentity(selectedUserId, pairingCode) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "身份决定留言、回复和心情记录的署名。切换前请确认你选择的是自己。",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray,
                )
                Spacer(modifier = Modifier.padding(bottom = 12.dp))
            }
        }
    }
}

@Composable
private fun IdentityChoice(
    name: String,
    @DrawableRes avatarRes: Int?,
    fallback: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = if (selected) SoftPink.copy(alpha = 0.38f) else SurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) CherryPink.copy(alpha = 0.38f) else Color(0xFFF1E8EB),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IdentityAvatar(avatarRes = avatarRes, fallback = fallback)
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = InkBlack,
            )
            Text(
                text = if (selected) "已选择" else "",
                style = MaterialTheme.typography.labelLarge,
                color = CherryPink,
            )
        }
    }
}

@Composable
private fun IdentityAvatar(
    @DrawableRes avatarRes: Int?,
    fallback: String,
) {
    Surface(
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = SoftPink.copy(alpha = 0.32f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (avatarRes != null) {
                Image(
                    painter = painterResource(avatarRes),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(text = fallback, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
