package com.kalinbetschedule.data
import android.content.Context
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
object ScheduleRepository {
    private const val FILE_NAME = "schedule.json"
    val clients = mutableStateListOf<Client>()
    val services = mutableStateListOf<Service>()
    val slots = mutableStateListOf<Slot>()
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set
    private var nextId = 1L
    private var file: File? = null
    private var loaded = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Any()
    @Volatile
    private var pendingJson: String? = null
    fun init(context: Context) {
        if (loaded) return
        loaded = true
        val target = File(context.applicationContext.filesDir, FILE_NAME)
        file = target
        val text = runCatching { if (target.exists()) target.readText() else null }.getOrNull()
        if (!text.isNullOrBlank()) {
            runCatching { restore(JSONObject(text)) }
        }
    }
    private val clientIndex by derivedStateOf { clients.associateBy { it.id } }
    private val serviceIndex by derivedStateOf { services.associateBy { it.id } }
    fun clientById(id: Long?): Client? = id?.let(clientIndex::get)
    fun serviceById(id: Long?): Service? = id?.let(serviceIndex::get)
    fun slotById(id: Long?): Slot? = id?.let { key -> slots.firstOrNull { it.id == key } }
    fun visitsCount(clientId: Long): Int =
        slots.count { it.kind == SlotKind.APPOINTMENT && it.clientId == clientId }
    fun timedSlotsFor(date: LocalDate): List<Slot> =
        slots.filter { it.date == date && it.timed }.sortedBy { it.startMinute }
    fun untimedSlotsFor(date: LocalDate): List<Slot> =
        slots.filter { it.date == date && !it.timed }.sortedBy { it.id }
    fun appointmentsOf(clientId: Long): List<Slot> =
        slots.filter { it.kind == SlotKind.APPOINTMENT && it.clientId == clientId }
            .sortedWith(compareByDescending<Slot> { it.date }.thenByDescending { it.startMinute })
    fun hasOverlap(date: LocalDate, start: Int, end: Int, excludeId: Long? = null): Boolean =
        slots.any { it.date == date && it.id != excludeId && it.overlaps(start, end) }
    fun estimatedTotal(slot: Slot): Int {
        val base = serviceById(slot.serviceId)?.priceRub ?: 0
        val extras = slot.extraServiceIds.sumOf { serviceById(it)?.priceRub ?: 0 }
        return base + extras + slot.tipsRub
    }
    fun nextServiceOf(service: Service?): Service? = when (service?.nextProcedure) {
        null, NextProcedure.NONE -> null
        NextProcedure.SAME -> service
        NextProcedure.SPECIFIC -> serviceById(service.nextServiceId)
    }
    fun followUpsFor(
        slot: Slot,
        extraServiceIds: List<Long> = slot.extraServiceIds
    ): List<FollowUp> {
        val result = mutableListOf<FollowUp>()
        val planned = mutableSetOf<Pair<Long, LocalDate>>()
        (listOfNotNull(slot.serviceId) + extraServiceIds).forEach { doneId ->
            val done = serviceById(doneId) ?: return@forEach
            val next = nextServiceOf(done) ?: return@forEach
            val date = slot.date.plusDays(done.nextOffsetDays.toLong())
            if (planned.add(next.id to date)) result += FollowUp(next, date)
        }
        return result
    }
    fun addClient(name: String, phone: String): Client {
        val client = Client(nextId++, name.trim(), phone.trim())
        clients += client
        persist()
        return client
    }
    fun updateClient(client: Client) {
        val index = clients.indexOfFirst { it.id == client.id }
        if (index >= 0) clients[index] = client
        persist()
    }
    fun deleteClient(id: Long) {
        clients.removeAll { it.id == id }
        for (i in slots.indices) {
            if (slots[i].clientId == id) slots[i] = slots[i].copy(clientId = null)
        }
        persist()
    }
    private fun clientKeys(): Pair<MutableSet<String>, MutableSet<String>> {
        val phones = mutableSetOf<String>()
        val names = mutableSetOf<String>()
        clients.forEach { client ->
            normalizePhone(client.phone).takeIf { it.isNotEmpty() }?.let(phones::add)
            client.name.trim().lowercase().takeIf { it.isNotEmpty() }?.let(names::add)
        }
        return phones to names
    }
    fun knownContactIds(contacts: List<PhoneContact>): Set<String> {
        val (phones, names) = clientKeys()
        return contacts.mapNotNullTo(mutableSetOf()) { contact ->
            val digits = normalizePhone(contact.phone)
            val known = if (digits.isNotEmpty()) digits in phones
            else contact.name.trim().lowercase() in names
            contact.id.takeIf { known }
        }
    }
    fun importContacts(contacts: List<PhoneContact>): Int {
        val (phones, names) = clientKeys()
        var added = 0
        contacts.forEach { contact ->
            val name = contact.name.trim()
            val phone = contact.phone.trim()
            if (name.isEmpty() && phone.isEmpty()) return@forEach
            val digits = normalizePhone(phone)
            val fresh = if (digits.isNotEmpty()) phones.add(digits) else names.add(name.lowercase())
            if (!fresh) return@forEach
            clients += Client(nextId++, name.ifEmpty { phone }, phone)
            added++
        }
        if (added > 0) persist()
        return added
    }
    fun addService(
        name: String,
        durationMinutes: Int,
        priceRub: Int,
        nextProcedure: NextProcedure = NextProcedure.SAME,
        nextServiceId: Long? = null,
        nextOffsetDays: Int = DEFAULT_NEXT_OFFSET_DAYS
    ): Service {
        val service = Service(
            id = nextId++,
            name = name.trim(),
            durationMinutes = durationMinutes,
            priceRub = priceRub,
            nextProcedure = nextProcedure,
            nextServiceId = nextServiceId,
            nextOffsetDays = nextOffsetDays
        )
        services += service
        persist()
        return service
    }
    fun updateService(service: Service) {
        val index = services.indexOfFirst { it.id == service.id }
        if (index >= 0) services[index] = service
        persist()
    }
    fun deleteService(id: Long) {
        services.removeAll { it.id == id }
        for (i in services.indices) {
            if (services[i].nextServiceId == id) {
                services[i] = services[i].copy(
                    nextProcedure = NextProcedure.SAME,
                    nextServiceId = null
                )
            }
        }
        persist()
    }
    fun addAppointment(
        date: LocalDate,
        startMinute: Int,
        endMinute: Int,
        clientId: Long?,
        serviceId: Long?,
        description: String
    ): Slot {
        val slot = Slot(
            id = nextId++,
            date = date,
            startMinute = startMinute,
            endMinute = endMinute,
            kind = SlotKind.APPOINTMENT,
            clientId = clientId,
            serviceId = serviceId,
            description = description.trim()
        )
        slots += slot
        persist()
        return slot
    }
    fun addBusy(date: LocalDate, startMinute: Int, endMinute: Int, description: String): Slot {
        val slot = Slot(
            id = nextId++,
            date = date,
            startMinute = startMinute,
            endMinute = endMinute,
            kind = SlotKind.BUSY,
            description = description.trim()
        )
        slots += slot
        persist()
        return slot
    }
    fun updateSlot(slot: Slot) {
        val index = slots.indexOfFirst { it.id == slot.id }
        if (index >= 0) {
            slots[index] = if (slot.status == SlotStatus.COMPLETED) {
                slot.copy(totalRub = estimatedTotal(slot))
            } else {
                slot.copy(totalRub = 0)
            }
        }
        persist()
    }
    fun deleteSlot(id: Long) {
        slots.removeAll { it.id == id }
        persist()
    }
    fun completeAppointment(id: Long, extraServiceIds: List<Long>, tipsRub: Int) {
        val index = slots.indexOfFirst { it.id == id }
        if (index < 0) return
        val updated = slots[index].copy(
            extraServiceIds = extraServiceIds,
            tipsRub = tipsRub,
            status = SlotStatus.COMPLETED
        )
        val completed = updated.copy(totalRub = estimatedTotal(updated))
        slots[index] = completed
        createFollowUps(completed)
        persist()
    }
    private fun createFollowUps(completed: Slot) {
        followUpsFor(completed).forEach { followUp ->
            slots += Slot(
                id = nextId++,
                date = followUp.date,
                startMinute = 0,
                endMinute = 0,
                kind = SlotKind.APPOINTMENT,
                clientId = completed.clientId,
                serviceId = followUp.service.id,
                timed = false
            )
        }
    }
    fun changeTheme(mode: ThemeMode) {
        themeMode = mode
        persist()
    }
    var revision by mutableIntStateOf(0)
        private set
    val isEmpty: Boolean get() = clients.isEmpty() && services.isEmpty() && slots.isEmpty()
    fun exportJson(): String = snapshotJson().toString()
    fun importJson(text: String): Boolean {
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return false
        if (!root.has("clients") && !root.has("services") && !root.has("slots")) return false
        return runCatching {
            restore(root)
            persist()
            true
        }.getOrDefault(false)
    }
    private fun persist() {
        revision++
        val target = file ?: return
        pendingJson = snapshotJson().toString()
        scope.launch {
            synchronized(writeLock) {
                val data = pendingJson ?: return@synchronized
                pendingJson = null
                runCatching { target.writeText(data) }
            }
        }
    }
    private fun snapshotJson(): JSONObject = JSONObject().apply {
        put("nextId", nextId)
        put("theme", themeMode.name)
        put("clients", JSONArray().also { array ->
            clients.forEach {
                array.put(
                    JSONObject()
                        .put("id", it.id)
                        .put("name", it.name)
                        .put("phone", it.phone)
                )
            }
        })
        put("services", JSONArray().also { array ->
            services.forEach {
                array.put(
                    JSONObject()
                        .put("id", it.id)
                        .put("name", it.name)
                        .put("duration", it.durationMinutes)
                        .put("price", it.priceRub)
                        .put("nextProcedure", it.nextProcedure.name)
                        .put("nextServiceId", it.nextServiceId ?: JSONObject.NULL)
                        .put("nextOffset", it.nextOffsetDays)
                )
            }
        })
        put("slots", JSONArray().also { array ->
            slots.forEach { slot ->
                array.put(
                    JSONObject()
                        .put("id", slot.id)
                        .put("date", slot.date.toEpochDay())
                        .put("start", slot.startMinute)
                        .put("end", slot.endMinute)
                        .put("kind", slot.kind.name)
                        .put("clientId", slot.clientId ?: JSONObject.NULL)
                        .put("serviceId", slot.serviceId ?: JSONObject.NULL)
                        .put("description", slot.description)
                        .put("status", slot.status.name)
                        .put("extras", JSONArray().also { e -> slot.extraServiceIds.forEach(e::put) })
                        .put("tips", slot.tipsRub)
                        .put("total", slot.totalRub)
                        .put("timed", slot.timed)
                )
            }
        })
    }
    private fun restore(root: JSONObject) {
        clients.clear()
        services.clear()
        slots.clear()
        nextId = root.optLong("nextId", 1L).coerceAtLeast(1L)
        themeMode = runCatching { ThemeMode.valueOf(root.optString("theme", "SYSTEM")) }
            .getOrDefault(ThemeMode.SYSTEM)
        root.optJSONArray("clients")?.forEachObject {
            clients += Client(
                id = it.getLong("id"),
                name = it.optString("name"),
                phone = it.optString("phone")
            )
        }
        root.optJSONArray("services")?.forEachObject {
            services += Service(
                id = it.getLong("id"),
                name = it.optString("name"),
                durationMinutes = it.optInt("duration", SEGMENT_MINUTES),
                priceRub = it.optInt("price"),
                nextProcedure = runCatching {
                    NextProcedure.valueOf(it.optString("nextProcedure"))
                }.getOrDefault(NextProcedure.SAME),
                nextServiceId = if (it.isNull("nextServiceId")) null else it.getLong("nextServiceId"),
                nextOffsetDays = it.optInt("nextOffset", DEFAULT_NEXT_OFFSET_DAYS)
            )
        }
        root.optJSONArray("slots")?.forEachObject { obj ->
            val extras = mutableListOf<Long>()
            obj.optJSONArray("extras")?.let { array ->
                for (i in 0 until array.length()) extras += array.optLong(i)
            }
            slots += Slot(
                id = obj.getLong("id"),
                date = LocalDate.ofEpochDay(obj.getLong("date")),
                startMinute = obj.getInt("start"),
                endMinute = obj.getInt("end"),
                kind = runCatching { SlotKind.valueOf(obj.optString("kind")) }
                    .getOrDefault(SlotKind.BUSY),
                clientId = if (obj.isNull("clientId")) null else obj.getLong("clientId"),
                serviceId = if (obj.isNull("serviceId")) null else obj.getLong("serviceId"),
                description = obj.optString("description"),
                status = runCatching { SlotStatus.valueOf(obj.optString("status")) }
                    .getOrDefault(SlotStatus.PLANNED),
                extraServiceIds = extras,
                tipsRub = obj.optInt("tips"),
                totalRub = obj.optInt("total"),
                timed = obj.optBoolean("timed", true)
            )
        }
        val maxId = maxOf(
            clients.maxOfOrNull { it.id } ?: 0L,
            services.maxOfOrNull { it.id } ?: 0L,
            slots.maxOfOrNull { it.id } ?: 0L
        )
        nextId = maxOf(nextId, maxId + 1)
    }
    private inline fun JSONArray.forEachObject(action: (JSONObject) -> Unit) {
        for (i in 0 until length()) optJSONObject(i)?.let(action)
    }
}
