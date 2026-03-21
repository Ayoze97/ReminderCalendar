package com.remindercalendar

import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource

sealed class NotificationMethod(
    val id: String,
    val name: Any,
    val icon: Any,
    val warning: Int? = null,
    val intentBuilder: (String, String, String, String) -> Intent
) {
    object SMS : NotificationMethod(
        id= "sms",
        name = "SMS",
        icon = Icons.Default.Sms,
        intentBuilder = { msg, tel,_, _ ->
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$tel")).apply {
                putExtra("sms_body", msg)
            }
        }
    )

    object Mail : NotificationMethod(
        id = "mail",
        name = R.string.pref_send_method_mail,
        icon = Icons.Default.Email,
        warning = R.string.stored_mail_warning,
        intentBuilder = { msg, _, email, sub ->
            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply {
                putExtra(Intent.EXTRA_SUBJECT, sub)
                putExtra(Intent.EXTRA_TEXT, msg)
            }
        }
    )
    object WhatsApp : NotificationMethod(
        id = "whatsapp",
        name = "WhatsApp",
        icon = R.drawable.whatsapp_icon,
        intentBuilder = { msg, tel, _, _ ->
            val uri = Uri.parse("whatsapp://send?phone=$tel&text=${Uri.encode(msg)}")
            Intent(Intent.ACTION_VIEW, uri)
        }
    )

    object Telegram : NotificationMethod(
        id ="telegram",
        name= "Telegram",
        icon = R.drawable.telegram_icon,
        intentBuilder = { msg, tel, _, _ ->
            val uri = Uri.parse("tg://msg?text=${Uri.encode(msg)}&to=+$tel")
            Intent(Intent.ACTION_VIEW, uri)
        }
    )

    companion object {
        fun all() = listOf(SMS, Mail, WhatsApp, Telegram)
    }

    @Composable
    fun DrawIcon(modifier: Modifier = Modifier, contentDescription: String? = null) {
        when (icon) {
            is Int -> Icon(painterResource(icon), contentDescription, modifier)
            is ImageVector -> Icon(icon, contentDescription, modifier)
        }
    }
}