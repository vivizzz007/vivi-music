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
    "lyrics_line_spacing": {
        "ar": "تباعد الأسطر", "as": "শাৰীৰ ব্যৱধান", "az": "Sətir aralığı", "be": "Міжрадковы інтэрвал", "bg": "Разстояние между редовете", "bn": "লাইনের ব্যবধান", "bs": "Razmak između redova", "ca": "Espai entre línies",
        "cs": "Řádkování", "de": "Zeilenabstand", "el": "Διάστιχο", "es": "Espaciado de línea", "et": "Reavahe", "eu": "Lerro tartea", "fi": "Riviväli", "fil": "Espasyo ng linya",
        "fr": "Espacement des lignes", "hi": "पंक्ति रिक्ति", "hr": "Razmak redaka", "hu": "Sorköz", "id": "Jarak baris", "it": "Spaziatura righe", "ja": "行間隔", "km": "គម្លាតបន្ទាត់", "ko": "줄 간격",
        "lt": "Eilučių tarpas", "ml": "വരി അകലം", "ms": "Jarak baris", "nb": "Linjeavstand", "nl": "Regelafstand", "pa": "ਲਾਈਨ ਸਪੇਸਿੰਗ", "pl": "Odstęp między wierszami", "pt": "Espaçamento de linhas", "ro": "Spațiere între rânduri", "ru": "Межстрочный интервал", "sk": "Riadkovanie", "sl": "Razmik med vrsticami", "sr": "Размак између редова", "sv": "Radavstånd",
        "ta": "வரி இடைவெளி", "te": "లైన్ స్పేసింగ్", "th": "ระยะห่างบรรทัด", "tr": "Satır aralığı", "uk": "Міжрядковий інтервал", "vi": "Khoảng cách dòng", "zh-rCN": "行距", "zh-rTW": "行距",
    },
    "integrations": _all("Integrations"),
    "integrations_active": _all("Active"),
    "integrations_inactive": _all("Off"),
    "discord_presence": _all("Discord Rich Presence"),
    "discord_presence_enable": _all("Enable Rich Presence"),

    "discord_presence_desc": {
        "ar": "يعرض الأغنية الحالية على ملفك الشخصي في ديسكورد.", "as": "আপোনাৰ Discord প্ৰফাইলত বৰ্তমান ট্ৰেকটো দেখুৱায়।", "az": "Cari mahnınızı Discord profilində göstərir.", "be": "Паказвае бягучы трэк у вашым профілі Discord.", "bg": "Показва текущата песен във вашия профил на Discord.", "bn": "আপনার Discord প্রোফাইলে বর্তমান ট্র্যাকটি দেখায়।", "bs": "Prikazuje trenutnu pjesmu na vašem Discord profilu.", "ca": "Mostra la cançó actual al vostre perfil de Discord.",
        "cs": "Zobrazuje právě přehrávanou skladbu ve vašem profilu Discord.", "de": "Zeigt den aktuellen Titel auf deinem Discord-Profil.", "el": "Εμφανίζει το τρέχον τραγούδι στο προφίλ σας στο Discord.", "es": "Muestra la canción actual en tu perfil de Discord.", "et": "Näitab praegust lugu teie Discord-profiilis.", "eu": "Uneko abestia zure Discord profilean erakusten du.", "fi": "Näyttää parhaillaan soivan kappaleen Discordprofiilissasi.", "fil": "Ipinapakita ang kasalukuyang kanta sa iyong Discord profile.",
        "fr": "Affiche la chanson en cours sur votre profil Discord.", "hi": "आपके Discord प्रोफ़ाइल पर चल रहे ट्रैक को दिखाता है।", "hr": "Prikazuje trenutnu pjesmu na vašem Discord profilu.", "hu": "Megmutatja az éppen szóló számot a Discord profilodon.", "id": "Menampilkan lagu yang sedang diputar di profil Discord Anda.", "it": "Mostra la canzone in riproduzione sul tuo profilo Discord.", "ja": "Discordのプロフィールに再生中の曲を表示します。", "km": "បង្ហាញបទកំពុងចាក់នៅលើប្រូហ្វាល Discord របស់អ្នក។", "ko": "Discord 프로필에 현재 재생 중인 곡을 표시합니다.",
        "lt": "Rodo šiuo metu grojamą dainą jūsų Discord profilyje.", "ml": "നിങ്ങളുടെ Discord പ്രൊഫൈലിൽ നിലവിലെ പാട്ട് കാണിക്കുന്നു.", "ms": "Menunjukkan lagu semasa pada profil Discord anda.", "nb": "Viser nåværende låt i Discord-profilen din.", "nl": "Toont het huidige nummer op je Discord-profiel.", "pa": "ਤੁਹਾਡੇ Discord ਪ੍ਰੋਫਾਈਲ 'ਤੇ ਮੌਜੂਦਾ ਟਰੈਕ ਦਿਖਾਉਂਦਾ ਹੈ।", "pl": "Pokazuje obecny utwór na twoim profilu Discord.", "pt": "Mostra a música atual no seu perfil do Discord.", "ro": "Arată melodia curentă pe profilul tău Discord.", "ru": "Показывает текущий трек в вашем профиле Discord.",
        "sk": "Zobrazuje práve prehrávanú pieseň vo vašom profile Discord.", "sl": "Prikazuje trenutno skladbo v vašem profilu Discord.", "sr": "Приказује тренутну песму у свом Discord профилу.", "sv": "Visar den nuvarande låten i din Discord-profil.", "ta": "உங்கள் Discord சுயவிவரத்தில் தற்போதைய இசையை காட்டுகிறது.", "te": "మీ Discord ప్రొఫైల్‌లో ప్రస్తుతం ప్లే అవుతున్న ట్రాక్‌ని చూపుతుంది.", "th": "แสดงเพลงที่กำลังเล่นบนโปรไฟล์ Discord ของคุณ", "tr": "Discord profilinizde çalan şarkıyı görüntüler.", "uk": "Показує поточний трек у вашому профілі Discord.", "vi": "Hiển thị bài hát đang phát trên hồ sơ Discord của bạn.", "zh-rCN": "在您的 Discord 个人资料中显示当前曲目。", "zh-rTW": "在您的 Discord 個人資料中顯示目前曲目。",
    },
    "discord_client_id": {
        "ar": "معرف تطبيق ديسكورد", "de": "Discord-Anwendungs-ID", "el": "Αναγνωριστικό εφαρμογής Discord", "es": "ID de aplicación de Discord", "fr": "ID d'application Discord", "it": "ID applicazione Discord", "pl": "ID aplikacji Discord", "pt": "ID da aplicação Discord", "ru": "ID приложения Discord", "tr": "Discord uygulama kimliği", "uk": "ID застосунку Discord", "zh-rCN": "Discord 应用 ID", "zh-rTW": "Discord 應用程式 ID",
    },
    "discord_client_id_hint": {
        "ar": "أنشئ تطبيقًا على discord.com/developers والصق معرفه.", "de": "Erstelle eine App auf discord.com/developers und füge ihre ID ein.", "es": "Crea una aplicación en discord.com/developers y pega su ID.", "fr": "Créez une application sur discord.com/developers et collez son ID.", "it": "Crea un'app su discord.com/developers e incolla il suo ID.", "pl": "Utwórz aplikację na discord.com/developers i wklej jej ID.", "pt": "Crie uma aplicação em discord.com/developers e cole o ID.", "ru": "Создайте приложение на discord.com/developers и вставьте его ID.", "tr": "discord.com/developers üzerinden bir uygulama oluşturup kimliğini yapıştırın.", "uk": "Створіть застосунок на discord.com/developers і вставте його ID.", "zh-rCN": "在 discord.com/developers 创建应用并粘贴其 ID。", "zh-rTW": "在 discord.com/developers 建立應用程式並貼上其 ID。",
    },

    "lastfm": {
        "ar": "لست ف.إم", "de": "Last.fm", "el": "Last.fm", "es": "Last.fm", "fr": "Last.fm", "it": "Last.fm", "ja": "Last.fm", "ko": "Last.fm", "pl": "Last.fm", "pt": "Last.fm", "ru": "Last.fm", "tr": "Last.fm", "uk": "Last.fm", "zh-rCN": "Last.fm", "zh-rTW": "Last.fm",
    },
    "lastfm_enable": {
        "ar": "تفعيل التسجيل التلقائي", "de": "Scrobbling aktivieren", "el": "Ενεργοποίηση καταγραφής", "es": "Activar scrobbling", "fr": "Activer le scrobbling", "it": "Attiva il scrobbling", "pl": "Włącz scrobblowanie", "pt": "Ativar scrobbling", "ru": "Включить скробблинг", "tr": "Scrobbling'i etkinleştir", "uk": "Увімкнути скроблінг", "zh-rCN": "启用记录", "zh-rTW": "啟用記錄",
    },
    "lastfm_enable_desc": {
        "ar": "يسجل الأغاني التي تستمع إليها على Last.fm.", "de": "Überträgt die gehörten Titel an Last.fm.", "el": "Καταγράφει τα τραγούδια που ακούτε στο Last.fm.", "es": "Registra las canciones que escuchas en Last.fm.", "fr": "Enregistre les chansons que vous écoutez sur Last.fm.", "it": "Registra le canzoni che ascolti su Last.fm.", "pl": "Rejestruje słuchane utwory na Last.fm.", "pt": "Regista as músicas que ouve no Last.fm.", "ru": "Отправляет прослушанные треки в Last.fm.", "tr": "Dinlediğiniz şarkıları Last.fm'e gönderir.", "uk": "Відправляє прослухані треки у Last.fm.", "zh-rCN": "将您收听的歌曲记录到 Last.fm。", "zh-rTW": "將您聆聽的歌曲記錄到 Last.fm。",
    },
    "lastfm_session": {
        "ar": "مفتاح الجلسة", "de": "Sitzungsschlüssel", "el": "Κλειδί συνόδου", "es": "Clave de sesión", "fr": "Clé de session", "it": "Chiave di sessione", "pl": "Klucz sesji", "pt": "Chave de sessão", "ru": "Ключ сессии", "tr": "Oturum anahtarı", "uk": "Ключ сесії", "zh-rCN": "会话密钥", "zh-rTW": "工作階段金鑰",
    },
    "lastfm_session_hint": {
        "ar": "الصق مفتاح جلسة Last.fm الخاص بك.", "de": "Einfügen deines Last.fm-Sitzungsschlüssels.", "es": "Pega tu clave de sesión de Last.fm.", "fr": "Collez votre clé de session Last.fm.", "it": "Incolla la tua chiave di sessione Last.fm.", "pl": "Wklej swój klucz sesji Last.fm.", "pt": "Cole a sua chave de sessão do Last.fm.", "ru": "Вставьте ключ сессии Last.fm.", "tr": "Last.fm oturum anahtarınızı yapıştırın.", "uk": "Вставте ключ сесії Last.fm.", "zh-rCN": "粘贴您的 Last.fm 会话密钥。", "zh-rTW": "貼上您的 Last.fm 工作階段金鑰。",
    },
    "lastfm_now_playing": {
        "ar": "تحديث ما يتم تشغيله الآن", "de": "Aktuellen Titel mitteilen", "el": "Ενημέρωση τρέχοντος τραγουδιού", "es": "Actualizar ahora en reproducción", "fr": "Mettre à jour le titre en cours", "it": "Aggiorna brano in riproduzione", "pl": "Aktualizuj teraz odtwarzane", "pt": "Atualizar música atual", "ru": "Обновлять текущий трек", "tr": "Şu an dinleneni güncelle", "uk": "Оновлювати поточний трек", "zh-rCN": "更新当前播放", "zh-rTW": "更新目前播放",
    },
    "lastfm_now_playing_desc": {
        "ar": "يعرض أيضًا الأغنية قيد التشغيل حاليًا.", "de": "Meldet zusätzlich den aktuell gespielten Titel.", "es": "También informa del tema que se está reproduciendo.", "fr": "Signale aussi le titre en cours de lecture.", "it": "Segnala anche il brano in riproduzione.", "pl": "Zgłasza również aktualnie odtwarzany utwór.", "pt": "Também informa a música em reprodução.", "ru": "Также сообщает о текущем треке.", "tr": "Çalmakta olan şarkıyı da bildirir.", "uk": "Також повідомляє про поточний трек.", "zh-rCN": "同时上报正在播放的曲目。", "zh-rTW": "同時回報正在播放的曲目。",
    },
}

# For the long-sentence keys above, translations for the long-tail languages
# intentionally fall back to the English text (the generator does the same) so
# no language ever shows a broken/empty string.
_EN_FALLBACK = {
    "discord_client_id": "Discord application ID",
    "discord_client_id_hint": "Create an application at discord.com/developers and paste its ID.",
    "lastfm": "Last.fm",
    "lastfm_enable": "Enable scrobbling",
    "lastfm_enable_desc": "Scrobbles the tracks you listen to on Last.fm.",
    "lastfm_session": "Session key",
    "lastfm_session_hint": "Paste your Last.fm session key (from the mobile app or last.fm/api).",
    "lastfm_now_playing": "Update now playing",
    "lastfm_now_playing_desc": "Also report the track currently playing.",
}
for _key, _en in _EN_FALLBACK.items():
    _d = EXTRA_TRANSLATIONS.setdefault(_key, {})
    for _lang in _LANGS:
        _d.setdefault(_lang, _en)
