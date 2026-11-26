package me.magnum.melonds.domain.model

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class AppLanguageSettingsTest {
    @Test
    fun emptyOrBlankLocalesResolveToSystemDefault() {
        assertEquals(APP_LANGUAGE_SYSTEM_TAG, AppLanguageSettings.resolvePreferenceValue(null))
        assertEquals(APP_LANGUAGE_SYSTEM_TAG, AppLanguageSettings.resolvePreferenceValue(""))
        assertEquals(APP_LANGUAGE_SYSTEM_TAG, AppLanguageSettings.resolvePreferenceValue("   "))
        assertEquals(APP_LANGUAGE_SYSTEM_TAG, AppLanguageSettings.resolvePreferenceValue(" , "))
    }

    @Test
    fun exactSupportedTagsResolveCaseInsensitively() {
        for (option in AppLanguageSettings.SUPPORTED_LANGUAGES) {
            assertEquals(
                option.languageTag,
                AppLanguageSettings.resolvePreferenceValue(option.languageTag),
            )
            assertEquals(
                option.languageTag,
                AppLanguageSettings.resolvePreferenceValue(option.languageTag.uppercase()),
            )
            assertEquals(
                option.languageTag,
                AppLanguageSettings.resolvePreferenceValue(option.languageTag.lowercase()),
            )
        }
    }

    @Test
    fun regionalAndLegacyTagsNormalizeToSupportedLocales() {
        assertEquals("fr", AppLanguageSettings.resolvePreferenceValue("fr-FR"))
        assertEquals("en", AppLanguageSettings.resolvePreferenceValue("en-US"))
        assertEquals("es", AppLanguageSettings.resolvePreferenceValue("es-419"))
        assertEquals("it", AppLanguageSettings.resolvePreferenceValue("it-IT"))
        assertEquals("ru", AppLanguageSettings.resolvePreferenceValue("ru-RU"))
        assertEquals("id", AppLanguageSettings.resolvePreferenceValue("in"))
        assertEquals("id", AppLanguageSettings.resolvePreferenceValue("in-ID"))
        assertEquals("id", AppLanguageSettings.resolvePreferenceValue("id-ID"))
        assertEquals("zh-Hans", AppLanguageSettings.resolvePreferenceValue("zh"))
        assertEquals("zh-Hans", AppLanguageSettings.resolvePreferenceValue("zh-CN"))
        assertEquals("zh-Hans", AppLanguageSettings.resolvePreferenceValue("zh-SG"))
        assertEquals("zh-Hans", AppLanguageSettings.resolvePreferenceValue("zh-Hans-CN"))
        assertEquals("pt-BR", AppLanguageSettings.resolvePreferenceValue("pt"))
        assertEquals("pt-BR", AppLanguageSettings.resolvePreferenceValue("pt_BR"))
    }

    @Test
    fun unsupportedExplicitOverridesReturnNullWhileFallbackListsMatchFirstSupported() {
        assertNull(AppLanguageSettings.resolvePreferenceValue("zh-Hant"))
        assertNull(AppLanguageSettings.resolvePreferenceValue("zh-Hant-TW"))
        assertNull(AppLanguageSettings.resolvePreferenceValue("zh-Hant-CN"))
        assertNull(AppLanguageSettings.resolvePreferenceValue("zh-TW"))
        assertNull(AppLanguageSettings.resolvePreferenceValue("zh-HK"))
        assertNull(AppLanguageSettings.resolvePreferenceValue("zh-MO"))
        assertNull(AppLanguageSettings.resolvePreferenceValue("de-DE"))
        assertNull(AppLanguageSettings.resolvePreferenceValue("ja-JP"))
        assertNull(AppLanguageSettings.resolvePreferenceValue("pt-PT"))
        assertEquals("fr", AppLanguageSettings.resolvePreferenceValue("de-DE,fr-FR"))
        assertEquals("pt-BR", AppLanguageSettings.resolvePreferenceValue("pt-PT,pt_BR"))
    }

    @Test
    fun selectingSystemDefaultClearsExplicitUnmatchedOverrides() {
        assertTrue(AppLanguageSettings.shouldApplyApplicationLocales("pt-PT", ""))
        assertTrue(AppLanguageSettings.shouldApplyApplicationLocales("de-DE", ""))
        assertFalse(AppLanguageSettings.shouldApplyApplicationLocales("", ""))
        assertFalse(AppLanguageSettings.shouldApplyApplicationLocales("fr", "fr"))
        assertTrue(AppLanguageSettings.shouldApplyApplicationLocales("fr-FR", "fr"))
        assertTrue(AppLanguageSettings.shouldApplyApplicationLocales("pt-PT", "pt-BR"))
    }

    @Test
    fun toApplicationLocaleTagsCanonicalizesAndSanitizesValues() {
        assertEquals("pt-BR", AppLanguageSettings.toApplicationLocaleTags("pt_br"))
        assertEquals("zh-Hans", AppLanguageSettings.toApplicationLocaleTags("zh-CN"))
        assertEquals("id", AppLanguageSettings.toApplicationLocaleTags("in-ID"))
        assertEquals(APP_LANGUAGE_SYSTEM_TAG, AppLanguageSettings.toApplicationLocaleTags("ja-JP"))
        assertEquals(APP_LANGUAGE_SYSTEM_TAG, AppLanguageSettings.toApplicationLocaleTags(null))
    }

    @Test
    fun entryArraysAndDisplayLabelsKeepSystemDefaultFirstAndFormatUnmatchedOverrides() {
        val entries = AppLanguageSettings.buildEntries("System default")
        val entryValues = AppLanguageSettings.buildEntryValues()

        assertEquals(entries.size, entryValues.size)
        assertEquals(1 + AppLanguageSettings.SUPPORTED_LANGUAGES.size, entries.size)
        assertArrayEquals(
            arrayOf<CharSequence>(
                "System default",
                "English",
                "Español",
                "Français",
                "Bahasa Indonesia",
                "Italiano",
                "Português (Brasil)",
                "Русский",
                "简体中文",
            ),
            entries,
        )
        assertArrayEquals(
            arrayOf<CharSequence>(
                "",
                "en",
                "es",
                "fr",
                "id",
                "it",
                "pt-BR",
                "ru",
                "zh-Hans",
            ),
            entryValues,
        )
        assertEquals("System default", AppLanguageSettings.resolveDisplayLabel("", "System default"))
        assertEquals("System default", AppLanguageSettings.resolveDisplayLabel(null, "System default"))
        assertEquals("Français", AppLanguageSettings.resolveDisplayLabel("fr-FR", "System default"))
        assertEquals("简体中文", AppLanguageSettings.resolveDisplayLabel("zh-CN", "System default"))

        val ptPtLabel = AppLanguageSettings.resolveDisplayLabel("pt-PT", "System default")
        assertNotEquals("System default", ptPtLabel)
        assertTrue(ptPtLabel.isNotBlank())
        assertTrue(ptPtLabel.contains("Portugal", ignoreCase = true) || ptPtLabel.contains("pt", ignoreCase = true))

        val deDeLabel = AppLanguageSettings.resolveDisplayLabel("de-DE", "System default")
        assertNotEquals("System default", deDeLabel)
        assertTrue(deDeLabel.isNotBlank())
        assertTrue(deDeLabel.contains("Deutsch", ignoreCase = true) || deDeLabel.contains("de", ignoreCase = true))
    }

    @Test
    fun localesConfigXmlMatchesSupportedLanguagesInExactOrder() {
        val candidates = listOf(
            File("src/main/res/xml/locales_config.xml"),
            File("app/src/main/res/xml/locales_config.xml"),
        )
        val xmlFile = candidates.firstOrNull { it.isFile }
            ?: error("Could not locate locales_config.xml in ${candidates.map { it.absolutePath }}")

        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        val localeNodes = document.getElementsByTagName("locale")
        val xmlTags = buildList {
            for (index in 0 until localeNodes.length) {
                val item = localeNodes.item(index)
                val name = item.attributes?.getNamedItem("android:name")?.nodeValue
                    ?: error("Missing android:name attribute on <locale> at index $index")
                add(name)
            }
        }

        assertEquals(
            AppLanguageSettings.SUPPORTED_LANGUAGES.map { it.languageTag },
            xmlTags,
        )
    }
}
