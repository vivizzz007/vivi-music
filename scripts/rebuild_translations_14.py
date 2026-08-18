# -*- coding: utf-8 -*-
"""Rebuilds desktop_extra_translations_14.py deterministically."""
import io

LANGS = ["ar", "as", "az", "be", "bg", "bn", "bs", "ca", "cs", "de", "el", "es", "et", "eu", "fi", "fil", "fr",
         "hi", "hr", "hu", "id", "it", "ja", "km", "ko", "lt", "ml", "ms", "nb", "nl", "pa", "pl", "pt", "ro",
         "ru", "sk", "sl", "sr", "sv", "ta", "te", "th", "tr", "uk", "vi", "zh-rCN", "zh-rTW"]

DATA = {}

DATA["sort_az"] = {l: "A\u2013Z" for l in LANGS}
DATA["sort_za"] = {l: "Z\u2013A" for l in LANGS}
DATA["sort_artist"] = {
    "ar": "\u062d\u0633\u0628 \u0627\u0644\u0641\u0646\u0627\u0646", "as": "\u09b6\u09bf\u09b2\u09cd\u09aa\u09c0 \u0985\u09a8\u09c1\u09af\u09be\u09af\u09bc\u09c0", "az": "S\u0259n\u0259t\u00e7iy\u0259 g\u00f6r\u0259",
    "be": "\u041f\u0430 \u043a\u0430\u0441\u0442\u0443", "bg": "\u041f\u043e \u0438\u0437\u043f\u044a\u043b\u043d\u0438\u0442\u0435\u043b", "bn": "\u09b6\u09bf\u09b2\u09cd\u09aa\u09c0 \u0985\u09a8\u09c1\u09b8\u09be\u09b0\u09c7",
    "bs": "Po izvo\u0111a\u010du", "ca": "Per artista", "cs": "Podle um\u011blce", "de": "Nach K\u00fcnstler", "el": "\u039a\u03b1\u03c4\u03ac \u03ba\u03b1\u03bb\u03bb\u03b9\u03c4\u03ad\u03c7\u03bd\u03b7",
    "es": "Por artista", "et": "Esitaja j\u00e4rgi", "eu": "Artistaren arabera", "fi": "Esitt\u00e4j\u00e4n mukaan", "fil": "Ayon sa artist",
    "fr": "Par artiste", "hi": "\u0915\u0932\u093e\u0915\u093e\u0940 \u0926\u094d\u0935\u093e\u0930\u093e", "hr": "Po izvo\u0111a\u010du", "hu": "El\u0151ad\u00f3 szerint", "id": "Menurut artis",
    "it": "Per artista", "ja": "\u30a2\u30fc\u30c6\u30a3\u30b9\u30c8\u9806", "km": "\u178f\u17b6\u1798\u179f\u17b7\u179b\u1794\u178f\u1780\u179a",
    "ko": "\uc544\ud2f0\uc2a4\ud2b8\ubcc4", "lt": "Pagal atlik\u0117j\u0105", "ml": "\u0d15\u0d32\u0d3e\u0d15\u0d3e\u0d30\u0d7b \u0d05\u0d28\u0d41\u0d38\u0d30\u0d3f\u0d1a\u0d4d\u0d1a\u0d4d",
    "ms": "Mengikut artis", "nb": "Etter artist", "nl": "Op artiest", "pa": "\u0a15\u0a32\u0a3e\u0a15\u0a3e\u0a30 \u0a05\u0a28\u0a41\u0a38\u0a3e\u0a30",
    "pl": "Wg artysty", "pt": "Por artista", "ro": "Dup\u0103 artist", "ru": "\u041f\u043e \u0438\u0441\u043f\u043e\u043b\u043d\u0438\u0442\u0435\u043b\u044e",
    "sk": "Pod\u013ea umelca", "sl": "Po izvajalcu", "sr": "\u041f\u043e \u0438\u0437\u0432\u043e\u0452\u0430\u0447\u0443", "sv": "Efter artist",
    "ta": "\u0b95\u0bb2\u0bc8\u0b9e\u0bb0\u0bbe\u0bb2\u0bcd", "te": "\u0c16\u0c33\u0c3e\u0c15\u0c3e\u0c30\u0c41\u0c28\u0c3f \u0c26\u0c4d\u0c35\u0c3e\u0c30\u0c3e",
    "th": "\u0e15\u0e32\u0e21\u0e28\u0e34\u0e25\u0e1b\u0e34\u0e19", "tr": "Sanat\u00e7\u0131ya g\u00f6re", "uk": "\u0417\u0430 \u0432\u0438\u043a\u043e\u043d\u0430\u0432\u0446\u0435\u043c",
    "vi": "Theo ngh\u1ec7 s\u0129", "zh-rCN": "\u6309\u827a\u672f\u5bb6", "zh-rTW": "\u6309\u85dd\u8853\u5bb6",
}
DATA["last_listen"] = {
    "en": "Last listen", "ar": "\u0622\u062e\u0630 \u0627\u0633\u062a\u0645\u0627\u0639", "de": "Zuletzt geh\u00f6rt", "el": "\u03a4\u03b5\u03bb\u03b5\u03c5\u03c4\u03b1\u03af\u03b1 \u03b1\u03ba\u03c1\u03cc\u03b1\u03c3\u03b7",
    "es": "\u00daltima escucha", "fr": "Derni\u00e8re \u00e9coute", "it": "Ultimo ascolto", "pl": "Ostatnio s\u0142uchane", "pt": "\u00daltima audi\u00e7\u00e3o",
    "ru": "\u041d\u0435\u0434\u0430\u0432\u043d\u043e \u0441\u043b\u0443\u0448\u0430\u043b\u0438", "tr": "Son dinlenen", "uk": "\u041e\u0441\u0442\u0430\u043d\u043d\u0454 \u043f\u0440\u043e\u0441\u043b\u0443\u0445\u043e\u0432\u0443\u0432\u0430\u043d\u043d\u044f",
    "zh-rCN": "\u6700\u8fd1\u6536\u542c", "zh-rTW": "\u6700\u8fd1\u6536\u807d",
}
DATA["randomize_home_order"] = {
    "en": "Randomize home order", "ar": "\u062e\u0644\u0637 \u062a\u0631\u062a\u064a\u0628 \u0627\u0644\u0635\u0641\u062d\u0629 \u0627\u0644\u0631\u0626\u064a\u0633\u064a\u0629",
    "de": "Reihenfolge der Startseite mischen", "el": "\u03a4\u03c5\u03c7\u03b1\u03af\u03b1 \u03c3\u03b5\u03b9\u03c1\u03ac \u03c3\u03c4\u03b7\u03bd \u03b1\u03c1\u03c7\u03b9\u03ba\u03ae",
    "es": "Aleatorizar el orden de inicio", "fr": "M\u00e9langer l'ordre de l'accueil", "it": "Mescola l'ordine della home",
    "pl": "Losuj kolejno\u015b\u0107 strony g\u0142\u00f3wnej", "pt": "Embaralhar ordem da p\u00e1gina inicial",
    "ru": "\u041f\u0435\u0440\u0435\u043c\u0435\u0448\u0430\u0442\u044c \u043f\u043e\u0440\u044f\u0434\u043a\u043e\u043a \u0433\u043b\u0430\u0432\u043d\u043e\u0439",
    "tr": "Ana sayfa s\u0131ras\u0131n\u0131 kar\u0131\u015ft\u0131r", "uk": "\u041f\u0435\u0440\u0435\u043c\u0456\u0448\u0430\u0442\u0438 \u043f\u043e\u0440\u044f\u0434\u043e\u043a \u0433\u043e\u043b\u043e\u0432\u043d\u043e\u0457",
    "zh-rCN": "\u968f\u673a\u4e3b\u9875\u987a\u5e8f", "zh-rTW": "\u968f\u6a5f\u4e3b\u9875\u987a\u5e8f",
}
DATA["randomize"] = {"en": "Randomize"}
DATA["wrapped_title"] = {"en": "VIVI Wrapped \u00b7 This session"}
DATA["wrapped_tracks"] = {"en": "tracks"}
DATA["wrapped_desc"] = {
    "en": "Your listening stats for the current session — restart to reset.",
    "ar": "إحصائيات الاستماع الخاصة بك للجلسة الحالية — أعد التشغيل لإعادة التعيين.",
    "de": "Deine Hörstatistiken für die aktuelle Sitzung — Neustart setzt zurück.",
    "el": "Τα στατιστικά ακρόασης για την τρέχουσα συνεδρία — κάντε επανεκκίνηση για επαναφορά.",
    "es": "Tus estadísticas de escucha de la sesión actual; reinicia para restablecerlas.",
    "fr": "Vos statistiques d'écoute pour la session en cours — redémarrez pour réinitialiser.",
    "it": "Le tue statistiche di ascolto per la sessione corrente — riavvia per azzerarle.",
    "pl": "Twoje statystyki słuchania z bieżącej sesji — zrestartuj, aby wyzerować.",
    "pt": "Suas estatísticas de audição da sessão atual — reinicie para redefinir.",
    "ru": "Ваша статистика прослушивания за текущую сессию — перезапустите, чтобы сбросить.",
    "tr": "Bu oturumun dinleme istatistikleri — sıfırlamak için yeniden başlatın.",
    "uk": "Ваша статистика прослуховування за поточну сесію — перезапустіть, щоб скинути.",
    "zh-rCN": "当前会话的收听统计 — 重启可重置。",
    "zh-rTW": "目前工作階段的收聽統計——重新啟動可重設。",
}
DATA["wrapped_listening_time"] = {"en": "listening"}
DATA["wrapped_top_song"] = {"en": "top song"}
DATA["wrapped_show_on_home"] = {
    "en": "Show on Home", "ar": "\u0639\u0631\u0636 \u0641\u064a \u0627\u0644\u0631\u0626\u064a\u0633\u064a\u0629", "de": "Auf der Startseite anzeigen",
    "el": "\u0395\u03bc\u03c6\u03ac\u03bd\u03b9\u03c3\u03b7 \u03c3\u03c4\u03b7\u03bd \u03b1\u03c1\u03c7\u03b9\u03ba\u03ae", "es": "Mostrar en inicio", "fr": "Afficher sur l'accueil",
    "it": "Mostra nella home", "pl": "Poka\u017c na stronie g\u0142\u00f3wnej", "pt": "Mostrar no in\u00edcio",
    "ru": "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c \u043d\u0430 \u0433\u043b\u0430\u0432\u043d\u043e\u0439", "tr": "Ana sayfada g\u00f6ster",
    "uk": "\u041f\u043e\u043a\u0430\u0437\u0443\u0432\u0430\u0442\u0438 \u043d\u0430 \u0433\u043e\u043b\u043e\u0432\u043d\u0456\u0439", "zh-rCN": "\u5728\u4e3b\u9875\u663e\u793a", "zh-rTW": "\u5728\u4e3b\u9801\u986f\u793a",
}
DATA["wrapped_show_on_home_desc"] = {
    "en": "Display the VIVI Wrapped card at the top of the Home screen.",
    "ar": "\u0639\u0631\u0636 \u0628\u0637\u0627\u0642\u0629 VIVI Wrapped \u0641\u064a \u0623\u0639\u0644\u0649 \u0627\u0644\u0634\u0627\u0634\u0629 \u0627\u0644\u0631\u0626\u064a\u0633\u064a\u0629.",
    "de": "Zeigt die VIVI-Wrapped-Karte oben auf der Startseite.",
    "es": "Muestra la tarjeta VIVI Wrapped en la parte superior de la pantalla de inicio.",
    "fr": "Affiche la carte VIVI Wrapped en haut de l'\u00e9cran d'accueil.",
    "it": "Mostra la scheda VIVI Wrapped in cima alla home.",
    "pt": "Mostra o cart\u00e3o VIVI Wrapped no topo da tela inicial.",
    "ru": "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0435\u0442 \u043a\u0430\u0440\u0442\u043e\u0447\u043a\u0443 VIVI Wrapped \u0441\u0432\u0435\u0440\u0445\u0443 \u0433\u043b\u0430\u0432\u043d\u043e\u0433\u043e \u044d\u043a\u0440\u0430\u043d\u0430.",
    "tr": "VIVI Wrapped kart\u0131n\u0131 ana ekran\u0131n \u00fcst k\u0131sm\u0131nda g\u00f6sterir.",
    "uk": "\u041f\u043e\u043a\u0430\u0437\u0443\u0454 \u043a\u0430\u0440\u0442\u043a\u0443 VIVI Wrapped \u0432\u0433\u043e\u0440\u0456 \u0433\u043e\u043b\u043e\u0432\u043d\u043e\u0433\u043e \u0435\u043a\u0440\u0430\u043d\u0430.",
    "zh-rCN": "\u5728\u4e3b\u9875\u9876\u90e8\u663e\u793a VIVI Wrapped \u5361\u7247\u3002",
    "zh-rTW": "\u5728\u4e3b\u9801\u9802\u90e8\u986f\u793a VIVI Wrapped \u5361\u7247\u3002",
}
DATA["pause_listen_history_desc"] = {"en": "Hides the History screen from the sidebar."}
DATA["pause_search_history_desc"] = {"en": "Keeps new searches out of the recent-searches list."}
DATA["quick_settings"] = {
    "en": "Quick settings", "ar": "\u0625\u0639\u062f\u0627\u062f\u0627\u062a \u0633\u0631\u064a\u0639\u0629", "de": "Schnelleinstellungen",
    "el": "\u03a3\u03b3\u03b1\u03c1\u03ae \u03c1\u03c5\u03b8\u03bc\u03af\u03c3\u03b5\u03b9\u03c2", "es": "Ajustes r\u00e1pidos",
    "fr": "Param\u00e8tres rapides", "it": "Impostazioni rapide", "pl": "Szybkie ustawienia", "pt": "Defini\u00e7\u00f5es r\u00e1pidas",
    "ru": "\u0411\u044b\u0441\u0442\u0440\u044b\u0435 \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438", "tr": "H\u0131zl\u0131 ayarlar",
    "uk": "\u0428\u0432\u0438\u0434\u043a\u0456 \u043d\u0430\u043b\u0430\u0448\u0442\u0443\u0432\u0430\u043d\u043d\u044f", "zh-rCN": "\u5feb\u901f\u8bbe\u7f6e", "zh-rTW": "\u5feb\u901f\u8a2d\u5b9a",
}
DATA["lyrics_line_spacing"] = {
    "en": "Line spacing", "ar": "\u062a\u0628\u0627\u0639\u062f \u0627\u0644\u0623\u0633\u0637\u0631", "de": "Zeilenabstand", "el": "\u0394\u03b9\u03ac\u03c3\u03c4\u03b9\u03c7\u03bf",
    "es": "Espacio entre l\u00edneas", "fr": "Espacement des lignes", "it": "Spaziatura righe", "nl": "Regelafstand",
    "pl": "Odst\u0119p mi\u0119dzy wierszami", "pt": "Espa\u00e7amento de linhas", "ru": "\u041c\u0435\u0436\u0441\u0442\u0440\u043e\u0447\u043d\u044b\u0439 \u0438\u043d\u0442\u0435\u0440\u0432\u0430\u043b",
    "tr": "K\u0131s\u0131m aral\u0131\u011f\u0131", "uk": "\u041c\u0456\u0436\u0440\u044f\u0434\u043a\u043e\u0432\u0438\u0439 \u0456\u043d\u0442\u0435\u0440\u0432\u0430\u043b", "zh-rCN": "\u884c\u8ddd", "zh-rTW": "\u884c\u8ddd",
}
DATA["lyrics_line_spacing_desc"] = {"en": "Adjust the vertical spacing of the lyric lines."}

DATA["integrations"] = {"en": "Integrations"}
DATA["integrations_active"] = {"en": "Active"}
DATA["integrations_inactive"] = {"en": "Off"}
DATA["discord_presence"] = {"en": "Discord Rich Presence"}
DATA["discord_presence_enable"] = {"en": "Enable Rich Presence"}
DATA["discord_presence_desc"] = {
    "en": "Shows the current track on your Discord profile.", "ar": "\u0649\u0639\u0631\u0636 \u0627\u0644\u0623\u063a\u0644\u0646\u064a\u0629 \u0627\u0644\u062d\u0627\u0644\u064a\u0629 \u0639\u0644\u0649 \u0645\u0644\u0641\u0643 \u0627\u0644\u0634\u062e\u0635\u064a \u0641\u064a \u062f\u064a\u0633\u0643\u0648\u0631\u062f.",
    "de": "Zeigt den aktuellen Titel auf deinem Discord-Profil.",
    "es": "Muestra la canci\u00f3n actual en tu perfil de Discord.",
    "fr": "Affiche la chanson en cours sur votre profil Discord.",
    "it": "Mostra la canzone in riproduzione sul tuo profilo Discord.",
    "nl": "Toont het huidige nummer op je Discord-profiel.",
    "pt": "Mostra a m\u00fasica atual no seu perfil do Discord.",
    "ru": "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0435\u0442 \u0442\u0435\u043a\u0443\u0449\u0438\u0439 \u0442\u0440\u0435\u043a \u0432 \u0432\u0430\u0448\u0435\u043c \u043f\u0440\u043e\u0444\u0438\u043b\u0435 Discord.",
    "tr": "Discord profilinizde \u00e7alan \u015fark\u0131y\u0131 g\u00f6r\u00fcnt\u00fcler.",
    "uk": "\u041f\u043e\u043a\u0430\u0437\u0443\u0454 \u0442\u043e\u0447\u043d\u0438\u0439 \u0442\u0440\u0435\u043a \u0443 \u0432\u0430\u0448\u043e\u043c\u0443 \u043f\u0440\u043e\u0444\u0456\u043b\u0456 Discord.",
    "zh-rCN": "\u5728\u60a8\u7684 Discord \u4e2a\u4eba\u8d44\u6599\u4e2d\u663e\u793a\u5f53\u524d\u6682\u66f2\u3002",
    "zh-rTW": "\u5728\u60a8\u7684 Discord \u500b\u4eba\u8cc7\u6599\u4e2d\u986f\u793a\u76ee\u524d\u66f2\u76ee\u3002",
    "pl": "Pokazuje obecny utw\u00f3r na twoim profilu Discord.",
}
DATA["discord_client_id"] = {"en": "Discord application ID"}
DATA["discord_client_id_hint"] = {"en": "Create an application at discord.com/developers and paste its ID."}
DATA["lastfm"] = {"en": "Last.fm"}
DATA["lastfm_enable"] = {"en": "Enable scrobbling"}
DATA["lastfm_enable_desc"] = {"en": "Scrobbles the tracks you listen to on Last.fm."}
DATA["lastfm_session"] = {"en": "Session key"}
DATA["lastfm_session_hint"] = {"en": "Paste your Last.fm session key (from the mobile app or last.fm/api)."}
DATA["lastfm_now_playing"] = {"en": "Update now playing"}
DATA["lastfm_now_playing_desc"] = {"en": "Also report the track currently playing."}

DATA["stream_cache_minutes"] = {
    "en": "Stream cache minutes", "ar": "\u062f\u0642\u0627\u0626\u0642 \u0645\u064a\u0643\u0627\u0627\u0646\u064a\u0632\u0645 \u0627\u0644\u0628\u062b",
    "de": "Stream-Cache (Minuten)", "el": "\u039a\u03b1\u03c4\u03ac\u03bb\u03bf\u03b3\u03bf\u03c2 \u03c1\u03bf\u03ae\u03c2 (\u03bb\u03b5\u03c0\u03c4\u03ac)",
    "es": "Cach\u00e9 del stream (minutos)", "fr": "Cache du stream (minutes)", "it": "Cache dello stream (minuti)",
    "pt": "Cache do stream (minutos)", "ru": "\u0412\u0440\u0435\u043c\u044f \u043a\u044d\u0448\u0430 \u043f\u043e\u0442\u043e\u043a\u0430 (\u043c\u0438\u043d\u0443\u0442\u044b)",
    "tr": "Ak\u0131\u015f \u00f6nbelle\u011fi (dakika)", "zh-rCN": "\u6d41\u7f13\u5b58\uff08\u5206\u949f\uff09", "zh-rTW": "\u4e32\u6d41\u5feb\u5b58\uff08\u5206\u9418\uff09",
}
DATA["stream_cache_minutes_desc"] = {"en": "How long a resolved stream URL is reused before it is resolved again."}
DATA["stream_cache_forever"] = {
    "en": "Forever", "ar": "\u0625\u0644\u0649 \u0627\u0644\u0623\u0628\u062f", "de": "F\u00fcr immer",
    "el": "\u0393\u03b9\u03b1 \u03c0\u03ac\u03bd\u03c4\u03b1", "es": "Para siempre", "fr": "Pour toujours", "it": "Per sempre",
    "pt": "Para sempre", "ru": "\u041d\u0430\u0432\u0441\u0435\u0433\u0434\u0430", "tr": "Sonsuza dek",
    "uk": "\u041d\u0430\u0437\u0430\u0432\u0436\u0434\u0438", "zh-rCN": "\u6c38\u4e45", "zh-rTW": "\u6c38\u4e45",
}

out = ["# -*- coding: utf-8 -*-",
       "\"\"\"Desktop-only translations: home toggles, sort chips, privacy, quick settings, integrations.\"\"\"",
       "",
       "_LANGS = " + repr(LANGS), ""]
out.append("EXTRA_TRANSLATIONS = {")
for key, d in sorted(DATA.items()):
    merged = {l: d.get(l, d.get("en", next(iter(d.values())))) for l in LANGS}
    pairs = ", ".join('"%s": "%s"' % (l, merged[l].replace("\\", "\\\\").replace('"', '\\"')) for l in LANGS)
    out.append('    "%s": {%s},' % (key, pairs))
out.append("}")
io.open("scripts/desktop_extra_translations_14.py", "w", encoding="utf-8", newline="\n").write("\n".join(out) + "\n")
print("REWRITTEN OK", len(DATA), "keys")