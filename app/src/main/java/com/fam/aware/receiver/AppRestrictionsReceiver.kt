package com.fam.aware.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.RestrictionEntry
import android.os.Bundle
import androidx.core.os.BundleCompat
import com.fam.aware.R
import com.fam.aware.data.repository.RESTRICTION_KEY_PERSISTENT_ACCESS

/**
 * Официальный провайдер ограничений приложения (Android app restrictions).
 *
 * Это штатный механизм Android, через который родитель или администратор
 * устройства управляет приложением: система (или MDM/Family Link) запрашивает
 * список поддерживаемых ограничений, а значение ключа
 * [RESTRICTION_KEY_PERSISTENT_ACCESS] приложение читает через
 * `RestrictionsManager.getApplicationRestrictions()`.
 *
 * Receiver защищён системным разрешением `android.permission.GET_RESTRICTION_ENTRIES`,
 * поэтому вызвать его может только система. Приложение при этом:
 *  - не становится Device Owner или Profile Owner;
 *  - не использует `DevicePolicyManager`;
 *  - не может само выставить себе ограничение — значение задаёт только администратор;
 *  - не мешает отозвать разрешение или удалить приложение.
 */
class AppRestrictionsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_GET_RESTRICTION_ENTRIES) return

        val entry = RestrictionEntry(RESTRICTION_KEY_PERSISTENT_ACCESS, false).apply {
            title = context.getString(R.string.restriction_pa_entry_title)
            description = context.getString(R.string.restriction_pa_entry_description)
        }

        val extras = Bundle().apply {
            BundleCompat.putParcelableArrayList(
                this,
                Intent.EXTRA_RESTRICTIONS_LIST,
                arrayListOf(entry),
            )
        }
        resultExtras = extras
    }
}
