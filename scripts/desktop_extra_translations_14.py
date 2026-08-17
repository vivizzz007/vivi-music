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
    "pause_listen_history_desc": {
        "ar": "يخفي شاشة السجل من الشريط الجانبي.", "as": "চাইডবাৰৰ পৰা ইতিহাস স্ক্ৰীণ লুকুৱায়।", "az": "Tarix ekranını yan paneldən gizlədir.", "be": "Хавае экран гісторыі з бакавой панэлі.", "bg": "Скрива екрана с история от страничната лента.", "bn": "সাইডবার থেকে হিস্টরি স্ক্রিন লুকিয়ে রাখে।", "bs": "Sakriva ekran historije sa bočne trake.", "ca": "Amaga la pantalla d'historial de la barra lateral.",
        "cs": "Skryje obrazovku historie z postranního panelu.", "de": "Blendet den Verlauf aus der Seitenleiste aus.", "el": "Αποκρύπτει την οθόνη ιστορικού από τη γραμμή πλοήγησης.", "es": "Oculta la pantalla de historial de la barra lateral.", "et": "Peidab ajaloo vaate külgribast.", "eu": "Ezkutatzen du historia pantaila alboko barraren.", "fi": "Piilottaa historian näkymän sivupalkista.", "fil": "Itinatago ang history screen sa sidebar.",
        "fr": "Masque l'écran d'historique de la barre latérale.", "hi": "साइडबार से इतिहास स्क्रीन छिपाता है।", "hr": "Sakriva ekran povijesti s bočne trake.", "hu": "Elrejti az előzmények képernyőt az oldalsávról.", "id": "Menyembunyikan layar riwayat dari bilah samping.", "it": "Nasconde la schermata della cronologia dalla barra laterale.", "ja": "サイドバーから履歴画面を非表示にします。", "km": "លាក់អេក្រង់ប្រវត្តិពីរបារចំហៀង។", "ko": "사이드바에서 기록 화면을 숨깁니다.", "lt": "Paslepia istorijos ekraną iš šoninės juostos.",
        "ml": "സൈഡ്ബാറിൽ നിന്ന് ചരിത്ര സ്ക്രീൻ മറയ്ക്കുന്നു.", "ms": "Menyembunyikan skrin sejarah daripada bar sisi.", "nb": "Skjuler historikkskjermen fra sidefeltet.", "nl": "Verbergt het geschiedenisscherm uit de zijbalk.", "pa": "ਸਾਈਡਬਾਰ ਤੋਂ ਇਤਿਹਾਸ ਸਕ੍ਰੀਨ ਲੁਕਾਉਂਦਾ ਹੈ।", "pl": "Ukrywa ekran historii z paska bocznego.", "pt": "Oculta o ecrã de histórico da barra lateral.", "ro": "Ascunde ecranul de istoric din bara laterală.", "ru": "Скрывает экран истории с боковой панели.", "sk": "Skryje obrazovku histórie z bočného panela.", "sl": "Skrije zaslon zgodovine iz bočne vrstice.", "sr": "Sakriva ekran istorije sa bočne trake.", "sv": "Döljer historikskärmen från sidofältet.", "ta": "பக்கப்பட்டியில் இருந்து வரலாறு திரையை மறைக்கிறது.", "te": "సైడ్‌బార్ నుండి చరిత్ర స్క్రీన్‌ను దాచుతుంది.", "th": "ซ่อนหน้าจอประวัติจากแถบด้านข้าง", "tr": "Geçmiş ekranını kenar çubuğundan gizler.", "uk": "Ховає екран історії з бічної панелі.", "vi": "Ẩn màn hình lịch sử khỏi thanh bên.", "zh-rCN": "从侧边栏隐藏历史记录屏幕。", "zh-rTW": "從側邊欄隱藏歷史記錄畫面。",
    },
    "pause_search_history_desc": {
        "ar": "يمنع إضافة عمليات البحث الجديدة إلى قائمة الأخيرة.", "as": "নতুন সন্ধানসমূহ সাম্প্ৰতিক সন্ধান তালিকাত ৰখা বন্ধ কৰে।", "az": "Yeni axtarışların son axtarışlar siyahısına daxil olmasının qarşısını alır.", "be": "Не дае новым пошукам трапіць у спіс нядаўніх.", "bg": "Не позволява новите търсения да влизат в списъка с последни.", "bn": "নতুন অনুসন্ধান সাম্প্রতিক তালিকায় ঢোকা আটকায়।", "bs": "Sprečava nove pretrage da uđu u listu nedavnih.", "ca": "Evita que les noves cerques entrin a la llista de recents.",
        "cs": "Zabraňuje novým vyhledáváním dostat se do seznamu nedávných.", "de": "Verhindert, dass neue Suchanfragen in die letzten Suchanfragen gelangen.", "el": "Εμποδίζει τις νέες αναζητήσεις να μπουν στη λίστα πρόσφατων.", "es": "Impide que las nuevas búsquedas entren en la lista de recientes.", "et": "Takistab uute otsingute sattumist hiljutiste loendisse.", "eu": "Gelditzen du bilaketa berriak azkeneko zerrendan sartzea.", "fi": "Estää uusien hakujen pääsyn viimeisimpien listaan.", "fil": "Pinipigilan ang mga bagong paghahanap na pumasok sa listahan ng mga kamakailan.",
        "fr": "Empêche les nouvelles recherches d'entrer dans la liste des récentes.", "hi": "नई खोजों को हाल की सूची में आने से रोकता है।", "hr": "Sprečava nove pretrage da uđu u popis nedavnih.", "hu": "Megakadályozza, hogy az új keresések bekerüljenek a legutóbbiak közé.", "id": "Mencegah pencarian baru masuk ke daftar terbaru.", "it": "Impedisce alle nuove ricerche di entrare nell'elenco delle recenti.", "ja": "新しい検索が最近のリストに入らないようにします。", "km": "រារាំងការស្វែងរកថ្មីពីការចូលក្នុងបញ្ជីថ្មីៗ។", "ko": "새 검색이 최근 목록에 들어가지 못하게 합니다.", "lt": "Neleidžia naujoms paieškoms patekti į neseniai atliktų sąrašą.",
        "ml": "പുതിയ തിരയലുകൾ സമീപകാല പട്ടികയിൽ കടക്കുന്നത് തടയുന്നു.", "ms": "Menghalang carian baharu daripada memasuki senarai terbaru.", "nb": "Hindrer nye søk i å komme inn i nylig-listen.", "nl": "Voorkomt dat nieuwe zoekopdrachten in de recente lijst komen.", "pa": "ਨਵੀਆਂ ਖੋਜਾਂ ਨੂੰ ਹਾਲੀਆ ਸੂਚੀ ਵਿੱਚ ਆਉਣ ਤੋਂ ਰੋਕਦਾ ਹੈ।", "pl": "Zapobiega trafianiu nowych wyszukiwań na listę ostatnich.", "pt": "Impede que novas pesquisas entrem na lista de recentes.", "ro": "Împiedică noile căutări să intre în lista recentelor.", "ru": "Не позволяет новым поискам попадать в список недавних.", "sk": "Zabraňuje novým vyhľadávaniam dostať sa do zoznamu nedávnych.", "sl": "Preprečuje novim iskanjem vstop v seznam nedavnih.", "sr": "Sprečava nove pretrage da uđu u listu nedavnih.", "sv": "Förhindrar nya sökningar från att komma in i senaste-listan.", "ta": "புதிய தேடல்கள் சமீபத்திய பட்டியலில் வருவதைத் தடுக்கிறது.", "te": "కొత్త శోధనలు ఇటీవలి జాబితాలోకి రాకుండా నిరోధిస్తుంది.", "th": "ป้องกันการค้นหาใหม่ไม่ให้เข้าสู่รายการล่าสุด", "tr": "Yeni aramaların son aramalar listesine girmesini engeller.", "uk": "Запобігає потраплянню нових пошуків до списку нещодавніх.", "vi": "Ngăn các tìm kiếm mới vào danh sách gần đây.",        "zh-rCN": "防止新搜索进入最近列表。", "zh-rTW": "防止新搜尋進入最近列表。",
    },
    "quick_settings": {
        "ar": "إعدادات سريعة", "as": "কুইক ছেটিংছ", "az": "Tez parametrlər", "be": "Хуткія налады", "bg": "Бързи настройки", "bn": "কুইক সেটিংস", "bs": "Brze postavke", "ca": "Configuració ràpida",
        "cs": "Rychlá nastavení", "de": "Schnelleinstellungen", "el": "Γρήγορες ρυθμίσεις", "es": "Ajustes rápidos", "et": "Kiirseaded", "eu": "Ezarpen azkarrak", "fi": "Pika-asetukset", "fil": "Mabilis na mga setting",
        "fr": "Paramètres rapides", "hi": "त्वरित सेटिंग्स", "hr": "Brze postavke", "hu": "Gyorsbeállítások", "id": "Pengaturan cepat", "it": "Impostazioni rapide", "ja": "クイック設定", "km": "ការកំណត់រហ័ស", "ko": "빠른 설정",
        "lt": "Greiti nustatymai", "ml": "ദ്രുത ക്രമീകരണങ്ങൾ", "ms": "Tetapan pantas", "nb": "Hurtiginnstillinger", "nl": "Snelle instellingen", "pa": "ਤੇਜ਼ ਸੈਟਿੰਗਾਂ", "pl": "Szybkie ustawienia", "pt": "Definições rápidas", "ro": "Setări rapide", "ru": "Быстрые настройки", "sk": "Rýchle nastavenia", "sl": "Hitre nastavitve", "sr": "Brze postavke", "sv": "Snabbinställningar",
        "ta": "விரைவு அமைப்புகள்", "te": "శీఘ్ర సెట్టింగులు", "th": "การตั้งค่าด่วน", "tr": "Hızlı ayarlar", "uk": "Швидкі налаштування", "vi": "Cài đặt nhanh", "zh-rCN": "快速设置", "zh-rTW": "快速設定",
    },
}
