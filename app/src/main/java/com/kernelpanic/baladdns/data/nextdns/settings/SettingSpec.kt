package com.kernelpanic.baladdns.data.nextdns.settings

import com.google.gson.JsonElement

@JvmInline
value class SettingId(val value: String)

data class ApiBinding(val page: String, val path: List<String>)

data class SettingsPageSpec(
    val page: String,
    val settings: List<ProfileSettingSpec<*>>,
) {
    init {
        require(settings.all { it.api.page == page }) {
            "All settings on $page must use that page's API binding"
        }
    }
}

data class LocaleBinding(
    val titlePath: List<String> = emptyList(),
    val descriptionPath: List<String>? = null,
    val titleRes: Int? = null,
    val descriptionRes: Int? = null,
) {
    init {
        require(titlePath.isNotEmpty() || titleRes != null)
    }
}

data class ConfirmationSpec(
    val titlePath: List<String>,
    val bodyPath: List<String>,
    val destructive: Boolean = false,
)

data class SelectOption<T : Any>(
    val value: T,
    val labelPath: List<String>,
    val descriptionPath: List<String>? = null,
    val iconKey: String? = null,
)

sealed interface ProfileSettingSpec<T : Any> {
    val id: SettingId
    val api: ApiBinding
    val locale: LocaleBinding
    val confirmation: ConfirmationSpec?
        get() = null
    val visibleWhen: ((Map<SettingId, JsonElement>) -> Boolean)?
        get() = null

    fun decode(raw: JsonElement): T?
    fun encode(value: T): Any
}

data class BooleanSettingSpec(
    override val id: SettingId,
    override val api: ApiBinding,
    override val locale: LocaleBinding,
    val inverted: Boolean = false,
    override val visibleWhen: ((Map<SettingId, JsonElement>) -> Boolean)? = null,
) : ProfileSettingSpec<Boolean> {
    override fun decode(raw: JsonElement): Boolean? =
        raw.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }
            ?.asBoolean
            ?.let { if (inverted) !it else it }

    override fun encode(value: Boolean): Any = if (inverted) !value else value
}

data class IntSelectSettingSpec(
    override val id: SettingId,
    override val api: ApiBinding,
    override val locale: LocaleBinding,
    val options: List<SelectOption<Int>>,
    override val confirmation: ConfirmationSpec? = null,
    override val visibleWhen: ((Map<SettingId, JsonElement>) -> Boolean)? = null,
) : ProfileSettingSpec<Int> {
    override fun decode(raw: JsonElement): Int? =
        raw.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
            ?.asInt

    override fun encode(value: Int): Any = value
}

data class StringSelectSettingSpec(
    override val id: SettingId,
    override val api: ApiBinding,
    override val locale: LocaleBinding,
    val options: List<SelectOption<String>>,
    override val confirmation: ConfirmationSpec? = null,
    override val visibleWhen: ((Map<SettingId, JsonElement>) -> Boolean)? = null,
) : ProfileSettingSpec<String> {
    override fun decode(raw: JsonElement): String? =
        raw.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
            ?.asString

    override fun encode(value: String): Any = value
}
