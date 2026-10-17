package com.example.data

object QuranMiraclesGeology {
    val list: List<MiracleItem> = listOf(
        MiracleItem(id = "iron_sent_down",
        title = "Demir Elementinin Gökten İndirilmesi (Enzelnâ)",
        subtitle = "Demirin Dünya'da değil süpernova patlamalarında sentezlenip inmesi",
        category = MiracleCategory.GEOLOGY,
        surahName = "Hadîd Sûresi",
        verseNumber = "25. Ayet",
        surahIndex = 57,
        verseIndex = 25,
        arabicVerse = "وَأَنْزَلْنَا الْحَدِيدَ فِيهِ بَأْسٌ شَدِيدٌ وَمَنَافِعُ لِلنَّاسِ",
        verseTranslation = "Ve kendisinde çetin bir güç ve insanlar için birçok faydalar bulunan demiri indirdik (enzelnâ).",
        scientificDiscoveryTitle = "Süpernova Nükleosentezi & Meteorik Demir",
        discoveryCentury = "20. Yüzyıl Nükleer Astrofiziği",
        scientificExplanation = """
        Demir çekirdeğini sentezlemek için yüz milyonlarca derece sıcaklık gerekir. Dünya'daki demir, dev yıldızların patlaması (Süpernova) sonucu uzaya saçılan ve meteorlarla Dünya'ya 'indirilen' kozmik maddedir. Hadîd kelimesinin ebced değeri 26 (atom no), sure numarası ise 57'dir (Demir-57 izotopu).
        """.trimIndent(),
        keyScientificFacts = listOf(
        "Demir Dünya'da sentezlenemez, kozmik süpernova kökenlidir.",
        "Ebced değeri (26) Demir'in periyodik cetveldeki atom numarasıdır."
        ),
        quotesAndReferences = listOf(
        "Fred Hoyle, 'Supernovae and Nucleosynthesis'",
        "Armstrong (NASA Goddard Space Flight Center)"
        ),
        tags = listOf("demir", "hadid", "süpernova", "inzal", "element")
        ),
        MiracleItem(id = "earth_tectonic_faults_sad",
        title = "Yarıklar Sahibi Yeryüzü (Fay Hatları ve Kırıklar)",
        subtitle = "Litosferin tektonik fay hatlarıyla boydan boya yarık olması",
        category = MiracleCategory.GEOLOGY,
        surahName = "Târık Sûresi",
        verseNumber = "12. Ayet",
        surahIndex = 86,
        verseIndex = 12,
        arabicVerse = "وَالْأَرْضِ ذَاتِ الصَّدْعِ",
        verseTranslation = "Yarıklar ve çatlaklarla dolu olan (fay hatlarına sahip) yeryüzüne andolsun!",
        scientificDiscoveryTitle = "Küresel Tektonik Fay Ağları ve Rift Vadileri",
        discoveryCentury = "20. Yüzyıl Jeolojisi",
        scientificExplanation = """
        'Es-Sad'' (الصَّدْعِ) kelimesi 'yarıp ayrılmak, derin kırık ve çatlak' demektir. Modern jeoloji, Dünya kabuğunun on binlerce kilometre uzunluğunda tektonik fay hatları (San Andreas, Doğu Anadolu, Kızıldeniz Rifti vb.) ile boydan boya yarılmış bir mozaik olduğunu keşfetmiştir. Bu yarıklar gezegenin aşırı ısı ve basıncını tahliye eder.
        """.trimIndent(),
        keyScientificFacts = listOf(
        "Okyanus ortası sırtları ve karasal faylar Dünya'yı saran dev yarıklardır.",
        "Zâti's-Sad' tabiri yeryüzünün fay morfolojisini doğrudan tanımlar."
        ),
        quotesAndReferences = listOf(
        "USGS Earthquake Hazards Program",
        "Prof. Dr. Zaghloul El-Naggar, 'The Geological Signs in the Holy Quran'"
        ),
        tags = listOf("fay hattı", "yarık", "sad", "tarık", "tektonik")
        ),
        MiracleItem(id = "returning_sky_reflection",
        title = "Geri Döndüren / Geri Yansıtan Gökyüzü (Zâti'r-Rac')",
        subtitle = "İyonosferin radyo dalgalarını, troposferin su buharını geri döndürmesi",
        category = MiracleCategory.GEOLOGY,
        surahName = "Târık Sûresi",
        verseNumber = "11. Ayet",
        surahIndex = 86,
        verseIndex = 11,
        arabicVerse = "وَالسَّمَاءِ ذَاتِ الرَّجْعِ",
        verseTranslation = "Dönüşlü olan (geri döndüren / geri yansıtan) göğe andolsun!",
        scientificDiscoveryTitle = "İyonosferik Dalga Yansıması ve Su Buharı Döngüsü",
        discoveryCentury = "20. Yüzyıl Atmosfer Fiziği",
        scientificExplanation = """
        'Er-Rac'' (الرَّجْعِ) 'geri döndürmek, gerisin geriye iade etmek' demektir. Atmosfer tabakaları yeryüzünden yükselen maddeleri ve dalgaları geri döndürür:
        1. Troposfer: Buharlaşan suyu soğutarak yağmur olarak yeryüzüne geri döndürür.
        2. İyonosfer: Yeryüzünden gönderilen radyo ve kablosuz iletişim dalgalarını yeryüzüne geri yansıtır.
        3. Ozon/Manyetosfer: Uzaydan gelen kozmik ışınları geri püskürtür.
        """.trimIndent(),
        keyScientificFacts = listOf(
        "İyonosfer radyo dalgalarını bir ayna gibi geri döndürerek kıtalararası iletişimi sağlar.",
        "Zâti'r-Rac' tabiri atmosferin geri döndürme/yansıtma fonksiyonunu ifade eder."
        ),
        quotesAndReferences = listOf(
        "Appleton, Edward V., 'The Ionosphere and Radio Wave Reflection' (Nobel 1947)",
        "NASA Goddard Institute for Space Studies"
        ),
        tags = listOf("iyonosfer", "geri döndüren gök", "rac", "tarık", "radyo dalgaları")
        ),
        MiracleItem(id = "seven_atmospheric_layers",
        title = "Yedi Kat Gökyüzü (Atmosfer Tabakaları)",
        subtitle = "Atmosferin fiziksel özelliklerine göre yedi temel katmandan oluşması",
        category = MiracleCategory.GEOLOGY,
        surahName = "Bakara & Mülk Sûreleri",
        verseNumber = "Bakara 29 / Mülk 3",
        surahIndex = 2,
        verseIndex = 29,
        arabicVerse = "فَسَوَّاهُنَّ سَبْعَ سَمَاوَاتٍ ۚ وَهُوَ بِكُلِّ شَيْءٍ عَلِيمٌ",
        verseTranslation = "...Böylece onları yedi gök olarak düzenledi. O, her şeyi hakkıyla bilendir.",
        scientificDiscoveryTitle = "Atmosfer Katmanları (Troposfer, Stratosfer, Mezosfer vb.)",
        discoveryCentury = "20. Yüzyıl Meteorolojisi",
        scientificExplanation = """
        Dünya atmosferi gaz yoğunluğu, sıcaklık gradyanı ve kimyasal yapısına göre 7 ana katmana ayrılır:
        1. Troposfer, 2. Stratosfer, 3. Ozonosfer, 4. Mezosfer, 5. Termosfer, 6. İyonosfer, 7. Ekzosfer. Her katmanın basıncı ve gaz bileşimi yaşamı koruyacak şekilde tasarlanmıştır.
        """.trimIndent(),
        keyScientificFacts = listOf(
        "Modern meteoroloji atmosferi 7 temel fonksiyona ve katmana ayırır.",
        "Kur'an'da 'Seb'a Semâvât' (yedi gök) ifadesi atmosfer ve kozmik katmanları kapsar."
        ),
        quotesAndReferences = listOf(
        "Ahrens, C. Donald, 'Meteorology Today: An Introduction to Weather and Climate'",
        "NASA Earth Atmosphere Model"
        ),
        tags = listOf("yedi gök", "atmosfer tabakaları", "stratosfer", "troposfer", "bakara")
        ),
        MiracleItem(id = "petroleum_fossil_fuel_origin",
        title = "Petrolün Oluşumu: Yeşil Bitkilerin Kara Sıvıya Dönüşmesi",
        subtitle = "Otlağın çıkarılıp sonra kapkara bir sel artığı / yakıt haline getirilmesi",
        category = MiracleCategory.GEOLOGY,
        surahName = "A'lâ Sûresi",
        verseNumber = "4-5. Ayetler",
        surahIndex = 87,
        verseIndex = 4,
        arabicVerse = "وَالَّذِي أَخْرَجَ الْمَرْعَىٰ ۝ فَجَعَلَهُ غُثَاءً أَحْوَىٰ",
        verseTranslation = "O ki yeşil otlağı (bitki örtüsünü) çıkardı. Sonra da onu kapkara bir sel artığına (siyah/koyu renkli organik tortuya) dönüştürdü.",
        scientificDiscoveryTitle = "Organik Jeokimya & Ham Petrolün Oluşumu",
        discoveryCentury = "20. Yüzyıl Petrol Jeolojisi",
        scientificExplanation = """
        'El-Mer'â' (الْمَرْعَىٰ) yeşil bitki örtüsü ve organik madde; 'Gusâen Ahvâ' (غُثَاءً أَحْوَىٰ) ise 'kararmış, simsiyah olmuş organik tortu/sıvı' demektir. Jeokimya biliminde ham petrol ve kömür, milyonlarca yıl önce yaşamış yeşil bitki ve alglerin yeraltında yüksek ısı ve basınçla bozunarak siyah bir hidrokarbon sıvısına (petrol) dönüşmesiyle oluşur.
        """.trimIndent(),
        keyScientificFacts = listOf(
        "Petrol organik bitki ve plankton kalıntılarının yeraltında karbonlaşmasıyla oluşur.",
        "Gusâen Ahvâ tam olarak koyu siyah-yeşil petrol kıvamını tarif eder."
        ),
        quotesAndReferences = listOf(
        "Hunt, John M., 'Petroleum Geochemistry and Geology'",
        "Prof. Dr. Haluk Nurbaki, 'Kur'an-ı Kerim ve Fen İlimleri'"
        ),
        tags = listOf("petrol", "fosil yakıt", "ahva", "organik jeoloji", "ala")
        )
    )
}
