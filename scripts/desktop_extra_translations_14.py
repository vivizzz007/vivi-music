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
    "last_listen": {
        "ar": "آخر استماع", "as": "শেষ শুনা", "az": "Son dinləmə", "be": "Апошняе праслухоўванне", "bg": "Последно слушане", "bn": "শেষ শোনা", "bs": "Zadnje slušanje", "ca": "Darrera escolta",
        "cs": "Poslední poslech", "de": "Zuletzt gehört", "el": "Τελευταία ακρόαση", "es": "Última escucha", "et": "Viimati kuulatud", "eu": "Azken entzunaldia", "fi": "Viimeksi kuunneltu", "fil": "Huling pinakinggan",
        "fr": "Dernière écoute", "hi": "आखिरी बार सुना", "hr": "Zadnje slušanje", "hu": "Utolsó hallgatás", "id": "Terakhir didengar", "it": "Ultimo ascolto", "ja": "最近聴いた曲", "km": "ស្តាប់ចុងក្រោយ", "ko": "마지막으로 들음", "lt": "Paskutinį kartą klausyta",
        "ml": "അവസാനം കേട്ടത്", "ms": "Terakhir didengar", "nb": "Sist spilt", "nl": "Laatst beluisterd", "pa": "ਆਖਰੀ ਸੁਣੀ", "pl": "Ostatnio słuchane", "pt": "Última audição", "ro": "Ultima ascultare", "ru": "Недавно слушали", "sk": "Naposledy počúvané", "sl": "Nazadnje poslušano", "sr": "Posljednje slušano", "sv": "Senast lyssnat", "ta": "கடைசியாகக் கேட்டது", "te": "చివరగా విన్నవి",
        "th": "ฟังล่าสุด", "tr": "Son dinlenen", "uk": "Останнє прослуховування", "vi": "Đã nghe gần đây", "zh-rCN": "最近收听", "zh-rTW": "最近收聽",
    },
    "randomize_home_order": {
        "ar": "خلط ترتيب الصفحة الرئيسية", "as": "হ'ম ক্ৰম ৰেণ্ডমাইজ কৰক", "az": "Ana səhifə sırasını qarışdır", "be": "Перамяшаць парадак на галоўнай", "bg": "Разбъркай реда на началната страница", "bn": "হোমের ক্রম এলোমেলো করুন", "bs": "Nasumično miješaj redoslijed početne", "ca": "Aleatoritza l'ordre de la pàgina principal",
        "cs": "Náhodně seřadit domovské sekce", "de": "Reihenfolge der Startseite mischen", "el": "Τυχαία σειρά στην αρχική", "es": "Aleatorizar el orden de inicio", "et": "Segista avalehe järjekorda", "eu": "Ausaz ordenatu hasierako atalek", "fi": "Sekoita kodin järjestys", "fil": "I-random ang order ng home",
        "fr": "Mélanger l'ordre de l'accueil", "hi": "होम का क्रम यादृच्छिक करें", "hr": "Nasumično promijeni redoslijed početne", "hu": "Kezdőlap sorrendjének véletlenszerűsítése", "id": "Acak urutan beranda", "it": "Mescola l'ordine della home", "ja": "ホームの順序をランダム化", "km": "ចៃដន្យលំដាប់ទំព័រដើម", "ko": "홈 순서 무작위로",
        "lt": "Atsitiktinai išmaišyti pradinio puslapio eilę", "ml": "ഹോം ക്രമം റാൻഡം ചെയ്യുക", "ms": "Rawak urutan laman utama", "nb": "Tilfeldig rekkefølge på hjemmesiden", "nl": "Volgorde van startpagina door elkaar halen", "pa": "ਹੋਮ ਦੇ ਕ੍ਰਮ ਨੂੰ ਬੇਤਰਤੀਬ ਕਰੋ", "pl": "Losuj kolejność strony głównej", "pt": "Embaralhar ordem da página inicial", "ro": "Amestecă ordinea de pe prima pagină", "ru": "Случайный порядок главной страницы", "sk": "Náhodne usporiadať domovskú stránku", "sl": "Naključno razporedi domov",
        "sr": "Nasumično rasporedi početnu stranicu", "sv": "Slumpa ordningen på startsidan", "ta": "முகப்பு வரிசையை கலக்கவும்", "te": "హోమ్ క్రమాన్ని యాదృచ్ఛికం చేయండి", "th": "สุ่มลำดับหน้าแรก", "tr": "Ana sayfa sırasını karıştır", "uk": "Перемішати порядок головної сторінки", "vi": "Trộn thứ tự trang chủ", "zh-rCN": "随机主页顺序", "zh-rTW": "隨機主頁順序",
    },
    "randomize": _all("Randomize"),
    "wrapped_title": _all("VIVI Wrapped · This session"),
    "wrapped_tracks": _all("tracks"),
    "wrapped_listening_time": _all("listening"),
    "wrapped_top_song": _all("top song"),
    "sort_artist": {
        "ar": "حسب الفنان", "as": "শিল্পী অনুযায়ী", "az": "Sənətçiyə görə", "be": "Па касту", "bg": "По изпълнител", "bn": "শিল্পী অনুসারে", "bs": "Po izvođaču", "ca": "Per artista",
        "cs": "Podle umělce", "de": "Nach Künstler", "el": "Κατά καλλιτέχνη", "es": "Por artista", "et": "Esitaja järgi", "eu": "Artistaren arabera", "fi": "Esittäjän mukaan", "fil": "Ayon sa artist",
        "fr": "Par artiste", "hi": "कलाकार द्वारा", "hr": "Po izvođaču", "hu": "Előadó szerint", "id": "Menurut artis", "it": "Per artista", "ja": "アーティスト順", "km": "តាមសិល្បករ", "ko": "아티스트별",
        "lt": "Pagal atlikėją", "ml": "കലാകാരൻ അനുസരിച്ച്", "ms": "Mengikut artis", "nb": "Etter artist", "nl": "Op artiest", "pa": "ਕਲਾਕਾਰ ਅਨੁਸਾਰ", "pl": "Wg artysta", "pt": "Por artista",
        "ro": "După artist", "ru": "По исполнителю", "sk": "Podľa umelca", "sl": "Po izvajalcu", "sr": "По izvođaču", "sv": "Efter artist", "ta": "கலைஞரால்", "te": "కళాకారుని ద్వారా", "th": "ตามศิลปิน",
        "tr": "Sanatçıya göre", "uk": "За виконавцем", "vi": "Theo nghệ sĩ", "zh-rCN": "按艺术家", "zh-rTW": "按藝術家",
    },
}