package net.mbonnin.arcanetracker

import java.util.*

class Language(var friendlyName: String, var jsonName: String, var key: String) {
    companion object {

        var allLanguages = mutableListOf(
                Language("English", "enUS", "en"),
                Language("Français", "frFR", "fr"),
                Language("Español", "esES", "es"),
                Language("Italiano", "itIT", "it"),
                Language("Português", "ptBR", "pt"),
                Language("Pусский", "ruRU", "ru"),
                Language("한국의", "koKR", "ko"),
                Language("中國", "zhTW", "zh"))

    val currentLanguage: Language
        get() {
            var l: String? = Settings.get(Settings.LANGUAGE, "")

            if (l == "") {
                val locale = Locale.getDefault().language.toLowerCase()

                if (locale.contains("fr")) {
                    l = "fr"
                } else if (locale.contains("ru")) {
                    l = "ru"
                } else if (locale.contains("pt")) {
                    l = "pt"
                } else if (locale.contains("ko")) {
                    l = "ko"
                } else if (locale.contains("zh")) {
                    l = "zh"
                } else if (locale.contains("es")) {
                    l = "es"
                } else if (locale.contains("it")) {
                    l = "it"
                } else {
                    l = "en"
                }
            }

            var language: Language? = null
            for (language2 in allLanguages) {
                if (language2.key == l) {
                    language = language2
                    break
                }
            }

            if (language == null) {
                language = allLanguages[0]
            }

            return language
        }
}

}
