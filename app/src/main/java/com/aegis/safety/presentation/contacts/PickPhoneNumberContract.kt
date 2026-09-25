package com.aegis.safety.presentation.contacts

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContract

/**
 * Picks a phone number from the system contacts app.
 *
 * Unlike ActivityResultContracts.PickContact() (which returns a *contact* URI),
 * this returns a *phone* URI — the query result contains BOTH the display name
 * and the phone number in a single row. This is the most reliable way to
 * import a contact across all Android OEMs (Samsung, Xiaomi, OnePlus, stock…).
 *
 * Requires no READ_CONTACTS permission — the picker grants temporary,
 * per-contact read access via the returned URI.
 */
class PickPhoneNumberContract : ActivityResultContract<Void?, Intent?>() {

    override fun createIntent(context: Context, input: Void?): Intent {
        return Intent(Intent.ACTION_PICK).apply {
            type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Intent? {
        return if (resultCode == Activity.RESULT_OK) intent else null
    }
}