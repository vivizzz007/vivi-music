# -*- coding: utf-8 -*-
"""Desktop-only translations for the library sort chips (A-Z / Z-A / by artist)."""

_LANGS = ["ar", "as", "az", "be", "bg", "bn", "bs", "ca", "cs", "de", "el", "es", "et", "eu", "fi", "fil", "fr",
          "hi", "hr", "hu", "id", "it", "ja", "km", "ko", "lt", "ml", "ms", "nb", "nl", "pa", "pl", "pt", "ro",
          "ru", "sk", "sl", "sr", "sv", "ta", "te", "th", "tr", "uk", "vi", "zh-rCN", "zh-rTW"]

def _all(value):
    return {k: value for k in _LANGS}

EXTRA_TRANSLATIONS = {
    "sort_az": _all("A–Z"),
    "sort_za": _all("Z–A"),
    "sort_artist": {
        "ar": "حسب الفنان", "as": "শিল্পী অনুযায়ী", "az": "Sənətçiyə görə", "be": "Па касту", "bg": "По изпълнител", "bn": "শিল্পী অনুসারে", "bs": "Po izvođaču", "ca": "Per artista",
        "cs": "Podle umělce", "de": "Nach Künstler", "el": "Κατά καλλιτέχνη", "es": "Por artista", "et": "Esitaja järgi", "eu": "Artistaren arabera", "fi": "Esittäjän mukaan", "fil": "Ayon sa artist",
        "fr": "Par artiste", "hi": "कलाकार द्वारा", "hr": "Po izvođaču", "hu": "Előadó szerint", "id": "Menurut artis", "it": "Per artista", "ja": "アーティスト順", "km": "តាមសិល្បករ", "ko": "아티스트별",
        "lt": "Pagal atlikėją", "ml": "കലാകാരൻ അനുസരിച്ച്", "ms": "Mengikut artis", "nb": "Etter artist", "nl": "Op artiest", "pa": "ਕਲਾਕਾਰ ਅਨੁਸਾਰ", "pl": "Wg artysta", "pt": "Por artista",
        "ro": "După artist", "ru": "По исполнителю", "sk": "Podľa umelca", "sl": "Po izvajalcu", "sr": "По izvođaču", "sv": "Efter artist", "ta": "கலைஞரால்", "te": "కళాకారుని ద్వారా", "th": "ตามศิลปิน",
        "tr": "Sanatçıya göre", "uk": "За виконавцем", "vi": "Theo nghệ sĩ", "zh-rCN": "按艺术家", "zh-rTW": "按藝術家",
    },
}