package com.example.deepseekwidget.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepseekwidget.ui.theme.DisplayFontFamily
import com.example.deepseekwidget.ui.theme.LocalPalette

data class PlatformLinkItem(
    val title: String,
    val subtitle: String,
    val tag: String,
    val url: String
)

val DEEPSEEK_PLATFORM_LINKS = listOf(
    PlatformLinkItem(
        title = "用量与账单明细",
        subtitle = "查看每日 Token 消耗明细、调用次数与历史账单",
        tag = "USAGE",
        url = "https://platform.deepseek.com/usage"
    ),
    PlatformLinkItem(
        title = "API Keys 凭据管理",
        subtitle = "创建新密钥、复制凭证或吊销闲置 API Key",
        tag = "API KEYS",
        url = "https://platform.deepseek.com/api_keys"
    ),
    PlatformLinkItem(
        title = "账户充值中心",
        subtitle = "充值账户余额、管理支付方式与开具增值税发票",
        tag = "TOP UP",
        url = "https://platform.deepseek.com/top_up"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepSeekPlatformSheet(
    onDismiss: () -> Unit
) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        contentColor = palette.primaryInk,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 2.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(palette.tertiaryInk.copy(alpha = 0.35f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(palette.accent, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "DeepSeek 开放平台快捷入口",
                    color = palette.primaryInk,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "轻触直接唤起浏览器访问 DeepSeek 官方控制台相应页面",
                color = palette.secondaryInk,
                fontSize = 12.sp,
            )

            Spacer(Modifier.height(18.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DEEPSEEK_PLATFORM_LINKS.forEach { item ->
                    PlatformLinkCard(
                        item = item,
                        onClick = {
                            openUrl(context, item.url)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlatformLinkCard(
    item: PlatformLinkItem,
    onClick: () -> Unit
) {
    val palette = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.fieldFill)
            .border(1.dp, palette.hairline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    color = palette.primaryInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.chipFill)
                        .border(1.dp, palette.hairline, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.tag,
                        color = palette.accent,
                        fontFamily = DisplayFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.subtitle,
                color = palette.secondaryInk,
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "↗",
            color = palette.accent,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "无法启动浏览器打开链接", Toast.LENGTH_SHORT).show()
    }
}
