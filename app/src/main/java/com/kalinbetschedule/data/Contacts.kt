package com.kalinbetschedule.data
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
data class PhoneContact(
    val id: String,
    val name: String,
    val phone: String
)
fun hasContactsPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
        PackageManager.PERMISSION_GRANTED
fun readPhoneContacts(context: Context): List<PhoneContact> {
    if (!hasContactsPermission(context)) return emptyList()
    val idColumn = ContactsContract.CommonDataKinds.Phone.CONTACT_ID
    val nameColumn = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY
    val numberColumn = ContactsContract.CommonDataKinds.Phone.NUMBER
    val result = LinkedHashMap<String, PhoneContact>()
    runCatching {
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(idColumn, nameColumn, numberColumn),
            null,
            null,
            "$nameColumn COLLATE LOCALIZED ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(idColumn)
            val nameIndex = cursor.getColumnIndexOrThrow(nameColumn)
            val numberIndex = cursor.getColumnIndexOrThrow(numberColumn)
            while (cursor.moveToNext()) {
                val id = cursor.getString(idIndex) ?: continue
                if (result.containsKey(id)) continue
                val name = cursor.getString(nameIndex)?.trim().orEmpty()
                val phone = cursor.getString(numberIndex)?.trim().orEmpty()
                if (name.isEmpty() && phone.isEmpty()) continue
                result[id] = PhoneContact(id, name.ifEmpty { phone }, phone)
            }
        }
    }
    return result.values.toList()
}
fun normalizePhone(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    return if (digits.length > 10) digits.takeLast(10) else digits
}
