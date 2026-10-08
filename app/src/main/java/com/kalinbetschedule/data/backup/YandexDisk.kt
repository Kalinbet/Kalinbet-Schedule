package com.kalinbetschedule.data.backup
internal object YandexDisk {
    private const val API = "https://cloud-api.yandex.net/v1/disk/resources"
    private const val PATH = "app:/schedule.json"
    fun upload(token: String, json: String) {
        val target = Http.getJson("$API/upload?path=$PATH&overwrite=true", token)
        val href = target.optString("href").takeIf { it.isNotBlank() }
            ?: error("Диск не выдал ссылку для загрузки")
        Http.put(href, json)
    }
    fun download(token: String): String? {
        val source = runCatching { Http.getJson("$API/download?path=$PATH", token) }
            .getOrElse { return null }
        val href = source.optString("href").takeIf { it.isNotBlank() } ?: return null
        return Http.getText(href)
    }
}
