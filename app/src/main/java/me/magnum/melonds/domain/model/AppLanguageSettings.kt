package me.magnum.melonds.domain.model

import java.util.Locale

const val PREF_KEY_APP_LANGUAGE = "app_language"
const val APP_LANGUAGE_SYSTEM_TAG = ""

data class AppLanguageOption(
    val languageTag: String,
    val nativeLabel: String,
)

object AppLanguageSettings {
    val SUPPORTED_LANGUAGES: List<AppLanguageOption> = listOf(
        AppLanguageOption("en", "English"),
        AppLanguageOption("es", "Español"),
        AppLanguageOption("fr", "Français"),
        AppLanguageOption("id", "Bahasa Indonesia"),
        AppLanguageOption("it", "Italiano"),
        AppLanguageOption("pt-BR", "Português (Brasil)"),
        AppLanguageOption("ru", "Русский"),
        AppLanguageOption("zh-Hans", "简体中文"),
    )

    private val DIRECT_LANGUAGE_TAGS = setOf("en", "es", "fr", "it", "ru")

    fun resolveSelectedLanguageTag(applicationLocaleTags: String?): String =
        resolvePreferenceValue(applicationLocaleTags) ?: APP_LANGUAGE_SYSTEM_TAG

    fun resolvePreferenceValue(applicationLocaleTags: String?): String? {
        if (applicationLocaleTags.isNullOrBlank()) {
            return APP_LANGUAGE_SYSTEM_TAG
        }

        var sawNonBlankToken = false
        for (rawToken in applicationLocaleTags.split(',')) {
            val normalized = rawToken.trim().replace('_', '-')
            if (normalized.isEmpty()) {
                continue
            }
            sawNonBlankToken = true

            SUPPORTED_LANGUAGES.firstOrNull {
                it.languageTag.equals(normalized, ignoreCase = true)
            }?.let { return it.languageTag }

            val subtags = normalized.lowercase(Locale.ROOT).split('-')
            val lang = subtags.firstOrNull().orEmpty()
            val second = subtags.getOrNull(1)
            val third = subtags.getOrNull(2)

            when (lang) {
                "id", "in" -> return "id"
                "zh" -> {
                    if (second != "hant" && (
                            subtags.size == 1 ||
                                second == "hans" ||
                                second == "cn" ||
                                second == "sg" ||
                                third == "cn" ||
                                third == "sg"
                            )
                    ) {
                        return "zh-Hans"
                    }
                }
                "pt" -> {
                    if (subtags.size == 1 || second == "br" || third == "br") {
                        return "pt-BR"
                    }
                }
                else -> {
                    if (lang in DIRECT_LANGUAGE_TAGS) {
                        return lang
                    }
                }
            }
        }

        return if (sawNonBlankToken) null else APP_LANGUAGE_SYSTEM_TAG
    }

    fun toApplicationLocaleTags(selectedValue: String?): String =
        resolvePreferenceValue(selectedValue) ?: APP_LANGUAGE_SYSTEM_TAG

    fun shouldApplyApplicationLocales(
        currentApplicationLocaleTags: String?,
        selectedValue: String?,
    ): Boolean {
        val currentTags = currentApplicationLocaleTags.orEmpty().trim().replace('_', '-')
        val targetTags = toApplicationLocaleTags(selectedValue)
        return !currentTags.equals(targetTags, ignoreCase = true)
    }

    fun buildEntryValues(): Array<CharSequence> = buildList<CharSequence> {
        add(APP_LANGUAGE_SYSTEM_TAG)
        SUPPORTED_LANGUAGES.forEach { add(it.languageTag) }
    }.toTypedArray()

    fun buildEntries(systemDefaultLabel: String): Array<CharSequence> = buildList<CharSequence> {
        add(systemDefaultLabel)
        SUPPORTED_LANGUAGES.forEach { add(it.nativeLabel) }
    }.toTypedArray()

    fun resolveDisplayLabel(applicationLocaleTags: String?, systemDefaultLabel: String): String {
        val rawTags = applicationLocaleTags.orEmpty()
        val firstTag = rawTags.split(',')
            .asSequence()
            .map { it.trim().replace('_', '-') }
            .firstOrNull { it.isNotEmpty() }
            ?: return systemDefaultLabel

        val matchedTag = resolvePreferenceValue(rawTags)
        if (!matchedTag.isNullOrEmpty()) {
            return SUPPORTED_LANGUAGES.first { it.languageTag == matchedTag }.nativeLabel
        }

        val locale = Locale.forLanguageTag(firstTag)
        val displayName = locale.getDisplayName(locale)
        return if (
            displayName.isNotBlank() &&
            !displayName.equals("und", ignoreCase = true) &&
            !locale.language.equals("und", ignoreCase = true)
        ) {
            displayName.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(locale) else ch.toString()
            }
        } else {
            firstTag
        }
    }
}
