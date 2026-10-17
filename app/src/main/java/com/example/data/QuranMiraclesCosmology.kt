package com.example.data

object QuranMiraclesCosmology {
    val list: List<MiracleItem> = listOf(
        MiracleItem(id = "expanding_universe",
        title = "Evrenin Sürekli Genişlemesi",
        subtitle = "Gök kubbenin genişletilmesinin 14 asır önceden bildirilmesi",
        category = MiracleCategory.COSMOLOGY,
        surahName = "Zâriyât Sûresi",
        verseNumber = "47. Ayet",
        surahIndex = 51,
        verseIndex = 47,
        arabicVerse = "وَالسَّمَاءَ بَنَيْنَاهَا بِأَيْدٍ وَإِنَّا لَمُوسِعُونَ",
        verseTranslation = "Biz göğü kudretimizle bina ettik ve şüphesiz biz onu sürekli genişletmekteyiz.",
        scientificDiscoveryTitle = "Edwin Hubble & Genişleyen Kozmos (1929)",
        discoveryCentury = "20. Yüzyıl (1929)",
        scientificExplanation = """
        20. yüzyılın başlarına kadar 'Durağan Evren Modeli' kabul ediliyordu. 1929'da Edwin Hubble, galaksilerin ışık tayfındaki kırmızıya kaymayı (Doppler etkisi) gözlemleyerek evrenin sürekli genişlediğini kanıtladı.
        
        Arapça 've innâ lemûsi'ûn' (وَإِنَّا لَمُوسِعُونَ) ifadesindeki 'Evse'a' fiilinin şimdiki/geniş zamanlı fail formu 'durmaksızın genişletmekteyiz' demektir.
        """.trimIndent(),
        keyScientificFacts = listOf(
        "Hubble Yasası (1929) galaksilerin birbirinden uzaklaştığını ispatladı.",
        "Einstein'ın Genel Görelilik formülleri de evrenin dinamik genişlemesini destekler."
        ),
        quotesAndReferences = listOf(
        "Edwin Hubble, 'A Relation Between Distance and Radial Velocity among Extra-Galactic Nebulae' (1929)",
        "Stephen Hawking, 'Zamanın Kısa Tarihi'"
        ),
        tags = listOf("evren", "uzay", "genişleme", "hubble", "kozmoloji", "big bang")
        ),
        MiracleItem(id = "big_bang_origin",
        title = "Büyük Patlama (Big Bang) ve Kozmik Bitişiklik",
        subtitle = "Göklerin ve yerin başlangıçta tek bir nokta (ratk) halinde olması",
        category = MiracleCategory.COSMOLOGY,
        surahName = "Enbiyâ Sûresi",
        verseNumber = "30. Ayet",
        surahIndex = 21,
        verseIndex = 30,
        arabicVerse = "أَوَلَمْ يَرَ الَّذِينَ كَفَرُوا أَنَّ السَّمَاوَاتِ وَالْأَرْضَ كَانَتَا رَتْقًا فَفَتَقْنَاهُمَا",
        verseTranslation = "İnkâr edenler görmediler mi ki göklerle yer bitişik (tek bir nokta) idi de biz onları yarıp ayırdık?",
        scientificDiscoveryTitle = "Kozmik Tekillik & Big Bang Modeli (1965)",
        discoveryCentury = "20. Yüzyıl (1965)",
        scientificExplanation = """
        Madde ve uzay-zaman yaklaşık 13.8 milyar yıl önce sıfır hacimli tekil bir noktada (Singularity) bir aradaydı. 'Büyük Patlama' ile bu tekillik yarılarak genişledi.
        
        'Ratk' (رَتْقًا) birbiri içine geçmiş homojen tek parça kütle; 'Fetak' (فَتَقْنَاهُمَا) ise bu bütünlüğü yarıp dışarı çıkarmak anlamına gelir.
        """.trimIndent(),
        keyScientificFacts = listOf(
        "1965'te Kozmik Mikrodalga Arka Plan Işıması bulunarak Big Bang ispatlandı.",
        "Ratk ve Fetak kelimeleri tekillik ve patlamanın linguistik karşılığıdır."
        ),
        quotesAndReferences = listOf(
        "Steven Weinberg, 'İlk Üç Dakika'",
        "George Gamow, 'The Creation of the Universe'"
        ),
        tags = listOf("big bang", "tekillik", "ratk", "fetak", "kozmoloji")
        ),
        MiracleItem(id = "day_night_coiling_round",
        title = "Gece ve Gündüzün Birbirine Sarılması (Tekvîr)",
        subtitle = "Yerkürenin dönüşü sebebiyle gece ve gündüzün sarık gibi sarılması",
        category = MiracleCategory.COSMOLOGY,
        surahName = "Zümer Sûresi",
        verseNumber = "5. Ayet",
        surahIndex = 39,
        verseIndex = 5,
        arabicVerse = "يُكَوِّرُ اللَّيْلَ عَلَى النَّهَارِ وَيُكَوِّرُ النَّهَارَ عَلَى اللَّيْلِ",
        verseTranslation = "Geceyi gündüzün üzerine sarıyor, gündüzü de gecenin üzerine sarıyor (yükevviru).",
        scientificDiscoveryTitle = "Dünya'nın Dönüşü ve Gece-Gündüz Işık Eğrisi",
        discoveryCentury = "Astronomik Jeodezi",
        scientificExplanation = """
        Arapça 'Kevvere' (كورة) fiili 'yuvarlak bir nesnenin (başın) üzerine sarığı dolamak/sarmak' anlamına gelir. Dünya küre şeklinde olduğu ve kendi ekseninde döndüğü için gece ve gündüz aydınlanma çizgisi (Terminator Line) kürenin üzerine sarık gibi sarılır.
        """.trimIndent(),
        keyScientificFacts = listOf(
        "'Yükevviru' fiili ancak küresel bir cisim üzerinde gerçekleşebilecek sarmal hareketi ifade eder.",
        "Düz bir yüzeyde gece-gündüz sarılamaz, sadece açılıp kapanabilir."
        ),
        quotesAndReferences = listOf(
        "Râğıb el-İsfahânî, El-Müfredât (ك-و-ر maddesi)",
        "Prof. Dr. Haluk Nurbaki, 'İslam ve Kozmoloji'"
        ),
        tags = listOf("gece gündüz", "tekvir", "küresellik", "zumer", "dünya dönüşü")
        ),
        MiracleItem(id = "cosmic_balance_mizan",
        title = "Kozmik Denge ve Hassas Kütleçekim Ayarı (Mîzân)",
        subtitle = "Gök cisimlerinin kütleçekim ve merkezkaç kuvveti dengesiyle ayakta durması",
        category = MiracleCategory.COSMOLOGY,
        surahName = "Rahmân Sûresi",
        verseNumber = "7. Ayet",
        surahIndex = 55,
        verseIndex = 7,
        arabicVerse = "وَالسَّمَاءَ رَفَعَهَا وَوَضَعَ الْمِيزَانَ",
        verseTranslation = "Göğü yükseltti ve mîzânı (hassas ölçü ve dengeyi) koydu.",
        scientificDiscoveryTitle = "Evrenin Hassas Ayarı (Fine-Tuning) & Dört Temel Kuvvet",
        discoveryCentury = "20-21. Yüzyıl Teorik Fiziği",
        scientificExplanation = """
        Modern astrofizikte 'Fine-Tuning' (Hassas Ayar) olarak bilinen gerçek; evrendeki dört temel kuvvetin (Kütleçekim, Elektromanyetik, Güçlü ve Zayıf Nükleer Kuvvet) 10^120'de bir hassasiyetle ayarlanmış olmasıdır. Kütleçekim kuvveti milyarda bir dahi farklı olsaydı yıldızlar ve gezegenler oluşamazdı.
        """.trimIndent(),
        keyScientificFacts = listOf(
        "Kozmolojik sabit ve yerçekimi sabiti en ufak bir sapmada evreni çökertecek hassasiyettedir.",
        "Mîzân (ölçü ve denge) evrenin temel mimarisidir."
        ),
        quotesAndReferences = listOf(
        "Paul Davies, 'The Cosmic Blueprint' & 'The Accidental Universe'",
        "Sir Martin Rees, 'Just Six Numbers'"
        ),
        tags = listOf("mizan", "hassas ayar", "kütleçekim", "rahman", "kozmoloji")
        ),
        MiracleItem(id = "constellations_zodiac_buruj",
        title = "Gökteki Burçlar ve Galaktik Kümeler (Burûc)",
        subtitle = "Gök kubbede devasa burçların ve yıldız kümelerinin inşa edilmesi",
        category = MiracleCategory.COSMOLOGY,
        surahName = "Hicr & Furkân Sûreleri",
        verseNumber = "Hicr 16 / Furkân 61",
        surahIndex = 15,
        verseIndex = 16,
        arabicVerse = "وَلَقَدْ جَعَلْنَا فِي السَّمَاءِ بُرُوجًا وَزَيَّنَّاهَا لِلنَّاظِرِينَ",
        verseTranslation = "Andolsun biz gökte burçlar (yıldız kümeleri ve galaktik kaleler) yaptık ve onları bakanlar için süsledik.",
        scientificDiscoveryTitle = "Takımyıldızlar, Galaksi Kümeleri ve Süper Kümeler",
        discoveryCentury = "Astronomik Haritalandırma",
        scientificExplanation = """
        'Burûc' (بُرُوجًا) kelimesi 'yüksek kuleler, müstahkem kaleler ve devasa yapılar' anlamına gelir. Astronomide yıldızların oluşturduğu takımyıldızlar, galaksiler ve Laniakea gibi devasa süper kümeler uzayda birer kozmik kale gibi organize olmuştur.
        """.trimIndent(),
        keyScientificFacts = listOf(
        "Galaksi kümeleri milyonlarca ışık yılı genişliğinde kozmik yapılar oluşturur.",
        "Burûc kelimesi hem takımyıldızları hem de dev galaksi yapılarını kapsar."
        ),
        quotesAndReferences = listOf(
        "R. Brent Tully, 'The Laniakea Supercluster of Galaxies' (Nature, 2014)",
        "Fahreddin Râzî Tefsiri"
        ),
        tags = listOf("buruc", "takımyıldız", "galaksi kümesi", "hicr", "astronomi")
        )
    )
}
