package com.example.data

object QuranMiraclesDeepGeologyOcean {
    val list: List<MiracleItem> = listOf(
        MiracleItem(id = "geology_tectonic_plate_drift",
        title = "Dağların Bulutlar Gibi Yürümesi (Levha Tektoniği)",
        subtitle = "Hareketsiz sanılan dağların ve kıtaların bulutların akışı gibi hareket ettiğinin haber verilmesi",
        category = MiracleCategory.GEOLOGY,
        surahName = "Neml Sûresi",
        verseNumber = "88. Ayet",
        surahIndex = 27,
        verseIndex = 88,
        arabicVerse = "وَتَرَى الْجِبَالَ تَحْسَبُهَا جَامِدَةً وَهِيَ تَمُرُّ مَرَّ السَّحَابِ صُنْعَ اللَّهِ الَّذِي أَتْقَنَ كُلَّ شَيْءٍ",
        verseTranslation = "Sen dağları görürsün de onları hareketsiz (donmuş) sanırsın; oysa onlar bulutların geçişi gibi geçip gitmektedirler (yürümektedirler). Her şeyi kusursuz yaratan Allah'ın sanatıdır bu!",
        scientificDiscoveryTitle = "Alfred Wegener & Kıtaların Kayması ve Levha Tektoniği (1915/1960)",
        discoveryCentury = "20. Yüzyıl (1915-1968)",
        scientificExplanation = """
        Antik çağdan 20. yüzyılın başlarına kadar dağların ve yerkabuğunun tamamen sabit ve kıpırdamaz olduğu kabul ediliyordu. 1915'te Alfred Wegener 'Kıtaların Kayması' teorisini öne sürdü; 1960'larda ise modern 'Levha Tektoniği' (Plate Tectonics) kanıtlandı.
        
        Litosferik levhalar ve üzerlerindeki dağlar, erimiş astenosfer tabakası üzerinde yılda 2-10 cm hızla sürüklenirler. Ayetteki 'bulutların geçişi gibi' ifadesi, bu hareketin gözle doğrudan fark edilemeyecek kadar pürüzsüz, yumuşak ve sürekli bir akış olduğunu kusursuz bir zarafetle anlatır.
        """.trimIndent(),
        keyScientificFacts = listOf(
        "Kıtalar ve üzerlerindeki dağlar magma tabakası üzerinde sürekli kaymaktadır.",
        "Neml 88 ayeti dağların donuk/sabit sanıldığını ancak gerçekte yürüdüğünü belirtir.",
        "Bu jeolojik hareket Dünya'nın jeotermal enerjisini boşaltmasını ve volkanik dengeyi sağlar."
        ),
        quotesAndReferences = listOf(
        "Alfred Wegener, 'Die Entstehung der Kontinente und Ozeane' (1915)",
        "Press, F., & Siever, R. 'Earth' (W. H. Freeman & Co.)"
        ),
        tags = listOf("levha tektoniği", "kıtaların kayması", "neml suresi", "dağlar", "wegener", "jeoloji")
        ),
        MiracleItem(id = "geology_iron_origin_meteorites",
        title = "Demirin Gökten İndirilmesi (Enzelne'l-Hadîd)",
        subtitle = "Demir elementinin Dünya'da üretilemeyip süpernova patlamalarıyla uzaydan indirilmesi",
        category = MiracleCategory.GEOLOGY,
        surahName = "Hadîd Sûresi",
        verseNumber = "25. Ayet",
        surahIndex = 57,
        verseIndex = 25,
        arabicVerse = "وَأَنْزَلْنَا الْحَدِيدَ فِيهِ بَأْسٌ شَدِيدٌ وَمَنَافِعُ لِلنَّاسِ",
        verseTranslation = "...Ve kendisinde çetin bir güç ve insanlar için birçok faydalar bulunan demiri indirdik (gökten indirdik).",
        scientificDiscoveryTitle = "Yıldız Nükleosentezi ve Ağır Elementlerin Süpernovalardan Gelmesi",
        discoveryCentury = "20. Yüzyıl (Fred Hoyle)",
        scientificExplanation = """
        Arapça 'Ve enzelnâ' (وَأَنْزَلْنَا) fiili doğrudan 'gökten aşağı indirdik' demektir. Kur'an'da yaratılan diğer maddeler için 'halakna' (yarattık) denilirken demir için özellikle 'indirdik' ifadesi kullanılmıştır.
        
        Nükleer astrofiziğe göre Güneş'in ve erken Dünya'nın sıcaklığı demir atomunu (26 proton) sentezlemek için yetersizdir. Demirin oluşabilmesi için yüz milyonlarca derecelik Süpernova patlamaları gerekir. Evrenin geçmişinde patlayan dev yıldızlardan uzaya saçılan demir göktaşları, gezegenimizin oluşumu sırasında Dünya'ya 'gökten indirilmiştir'.
        """.trimIndent(),
        keyScientificFacts = listOf(
        "Demir çekirdeği sadece ömrünü tamamlayan dev yıldızların süpernova patlamalarında üretilir.",
        "Dünya'daki demir rezervleri uzaydan yağan meteoritler sayesinde yerkabuğuna ve çekirdeğe inmiştir.",
        "Hadîd Suresi Kur'an'ın 57. suresidir; demirin kararlı izotoplarından birinin kütle numarası 57'dir."
        ),
        quotesAndReferences = listOf(
        "Burbidge, E. M., Hoyle, F., et al. 'Synthesis of the Elements in Stars' (Reviews of Modern Physics, 1957)",
        "NASA Stardust Mission Reports"
        ),
        tags = listOf("demir", "hadid suresi", "süpernova", "nükleosentez", "gökten indirme", "element")
        ),
        MiracleItem(id = "geology_fault_lines_cracking_earth",
        title = "Yarıklarla Dolu Yerküre ve Fay Hatları Kuşağı",
        subtitle = "Yeryüzünün çatlak ve yarıklara sahip bir gezegen (Zâtü's-Sad') olarak nitelendirilmesi",
        category = MiracleCategory.GEOLOGY,
        surahName = "Târık Sûresi",
        verseNumber = "12. Ayet",
        surahIndex = 86,
        verseIndex = 12,
        arabicVerse = "وَالْأَرْضِ ذَاتِ الصَّدْعِ",
        verseTranslation = "Yarıklarla / çatlaklarla dolu yeryüzüne andolsun!",
        scientificDiscoveryTitle = "Küresel Fay Hatları Ağı ve Litosferik Kırılma Zonları",
        discoveryCentury = "20. Yüzyıl (Jeofizik)",
        scientificExplanation = """
        Arapça 'Sad' (الصَّدْعِ) kelimesi bir bütünün boydan boya yarılması, derin çatlaklar ve ayrıklar oluşturması demektir.
        
        Jeofizik haritaları, Dünya yerkabuğunun tek parça masif bir kaya olmadığını; San Andreas Fayı, Doğu Anadolu Fayı, Mariana Çukuru ve Okyanus Sırtları gibi on binlerce kilometrelik devasa kırık ve fay hatlarıyla (Fault Lines) yarılmış bir küre olduğunu ortaya koymuştur. Bu yarıklar gezegenin iç ısısını tahliye etmesi ve kıtasal levhaların hareket etmesi için hayati öneme sahiptir.
        """.trimIndent(),
        keyScientificFacts = listOf(
        "Dünya yerkabuğu birbirine kenetli levhalar ve onları ayıran dev fay hatlarıyla örülüdür.",
        "Sad' kelimesi yerkabuğunun boydan boya kırık hatlarını jeolojik hassasiyetle belirtir.",
        "Bu çatlaklar volkanik mineral zenginleşmesini ve karbon döngüsünü mümkün kılar."
        ),
        quotesAndReferences = listOf(
        "Kearey, P., Klepeis, K. A., & Vine, F. J. 'Global Tectonics' (Wiley-Blackwell)",
        "USGS (United States Geological Survey) - 'Fault Lines and Plate Boundaries'"
        ),
        tags = listOf("fay hatları", "sad", "tarık suresi", "deprem", "çatlaklar", "jeoloji")
        ),
        MiracleItem(id = "geology_water_cycle_underground_reservoirs",
        title = "Hidrolojik Döngü ve Yeraltı Akifer Rezervuarları",
        subtitle = "Gökten inen suyun toprağın derinliklerine süzülüp yeraltı su kaynaklarına depolanması",
        category = MiracleCategory.GEOLOGY,
        surahName = "Zümer Sûresi",
        verseNumber = "21. Ayet",
        surahIndex = 39,
        verseIndex = 21,
        arabicVerse = "أَلَمْ تَرَ أَنَّ اللَّهَ أَنْزَلَ مِنَ السَّمَاءِ مَاءً فَسَلَكَهُ يَنَابِيعَ فِي الْأَرْضِ",
        verseTranslation = "Görmedin mi, Allah gökten bir su indirdi de onu yerdeki su kaynaklarına (akiferlere, yeraltı damarlarına) akıtıp depoladı...",
        scientificDiscoveryTitle = "Bernard Palissy & Modern Su Döngüsü ve Akifer Teorisi (1580)",
        discoveryCentury = "16. ve 17. Yüzyıl (Bernard Palissy)",
        scientificExplanation = """
        Eski Yunan ve Roma felsefesinde (Aristo ve Platon) kaynak sularının gökten yağan yağmurla değil, okyanus suyunun yerin altındaki gizli tünellerden emilip buharlaşarak dağlardan fışkırmasıyla oluştuğu sanılıyordu.
        
        1580'de Bernard Palissy yağmurların toprağa sızarak yeraltı su kaynaklarını (akiferleri) beslediğini ilk kez bilimsel olarak kanıtladı. Kur'an ise 1000 yıl önce 'fe-selekehû yenâbîa fi'l-ard' (فَسَلَكَهُ يَنَابِيعَ فِي الْأَرْضِ - onu yeraltı kaynaklarına süzüp akıttı) diyerek modern hidrolojik döngüyü eksiksiz beyan etmiştir.
        """.trimIndent(),
        keyScientificFacts = listOf(
        "Yağmur suları geçirgen kayaçlardan süzülerek yeraltı akiferlerini ve pınarları doldurur.",
        "Eski çağın 'okyanustan sızan yeraltı suları' miti Kur'an ayetiyle reddedilmiştir.",
        "Hidrolojik döngü buharlaşma, yoğuşma, yağış ve yeraltı depolanması aşamalarıyla işler."
        ),
        quotesAndReferences = listOf(
        "Bernard Palissy, 'Discours admirables de la nature des eaux et fontaines' (1580)",
        "Todd, D. K., & Mays, L. W. 'Groundwater Hydrology' (Wiley)"
        ),
        tags = listOf("su döngüsü", "akifer", "yeraltı suyu", "zümer suresi", "hidroloji", "yağmur")
        ),
        MiracleItem(id = "geology_atmospheric_air_weight_heavy_clouds",
        title = "Yağmur Bulutlarının Devasa Tonajı ve Ağırlığı",
        subtitle = "Pamuk gibi hafif görünen bulutların aslında milyonlarca tonluk ağır kütleler olması",
        category = MiracleCategory.GEOLOGY,
        surahName = "A'râf Sûresi",
        verseNumber = "57. Ayet",
        surahIndex = 7,
        verseIndex = 57,
        arabicVerse = "حَتَّىٰ إِذَا أَقَلَّتْ سَحَابًا ثِقَالًا سُقْنَاهُ لِبَلَدٍ مَيِّتٍ",
        verseTranslation = "...Nihayet rüzgarlar o ağır bulutları (tonlarca su taşıyan bulut kütlelerini) yüklendiğinde, onu ölü bir beldeye sevk ederiz...",
        scientificDiscoveryTitle = "Meteorolojik Bulut Dinamiği ve Kümülonimbus Ağırlık Ölçümleri",
        discoveryCentury = "20. Yüzyıl (Meteoroloji Fiziği)",
        scientificExplanation = """
        Gözle bakıldığında gökyüzünde süzülen bulutlar hafif ve tüy gibi görünür. Oysa meteorolojik ölçümlere göre orta büyüklükte bir Kümülüs bulutu yaklaşık 500.000 kg (500 ton), dev bir fırtına bulutu (Kümülonimbus) ise 300.000 ton ila birkaç milyon ton su kütlesi barındırır.
        
        Ayet-i kerimede 'Sehâben Sikâlen' (سَحَابًا ثِقَالًا - ağır bulutlar) tamlamasının kullanılması, bulutların optik hafifliğine aldanmayan bilimsel ve fiziki gerçeği tam isabetle ortaya koyar.
        """.trimIndent(),
        keyScientificFacts = listOf(
        "1 km³'lük standart bir kümülüs bulutu yaklaşık 500 ton su damlacığı içerir.",
        "Büyük bir fırtına cephesi 1 milyar kilogramdan fazla su ağırlığı taşıyabilir.",
        "'Sikâl' (ağır) sıfatı bulutların tonajlı su depoları olduğunu vurgular."
        ),
        quotesAndReferences = listOf(
        "National Center for Atmospheric Research (NCAR) - 'How Much Does a Cloud Weigh?'",
        "Ahrens, C. D. 'Meteorology Today: An Introduction to Weather, Climate, and the Environment'"
        ),
        tags = listOf("ağır bulutlar", "araf suresi", "meteoroloji", "kümülonimbus", "yağmur tonajı")
        ),
        MiracleItem(id = "geology_rain_measured_quantity",
        title = "Yağmurun Yeryüzüne Belirli Bir Ölçüyle (Bi-Kaderin) İnmesi",
        subtitle = "Her yıl Dünya'dan buharlaşan ve yeryüzüne yağan su miktarının sabit kalması",
        category = MiracleCategory.GEOLOGY,
        surahName = "Zuhruf Sûresi",
        verseNumber = "11. Ayet",
        surahIndex = 43,
        verseIndex = 11,
        arabicVerse = "وَالَّذِي نَزَّلَ مِنَ السَّمَاءِ مَاءً بِقَدَرٍ فَأَنْشَرْنَا بِهِ بَلْدَةً مَيْتًا",
        verseTranslation = "O ki, gökten bir ölçüye göre (belirlenmiş hassas bir miktarla) su indirdi. Biz onunla ölü bir memlekete can verdik...",
        scientificDiscoveryTitle = "Küresel Hidrolojik Denge Sabiti (Yıllık 513 Trilyon m³)",
        discoveryCentury = "20. Yüzyıl (Klimatoloji & Hidroloji)",
        scientificExplanation = """
        Modern hidrolojik ölçümler, Dünya genelinde 1 saniyede buharlaşan su miktarının yaklaşık 16 milyon ton olduğunu tespit etmiştir. Bir yılda buharlaşan toplam su miktarı yaklaşık 513 trilyon metreküptür.
        
        Mucizevi olan şudur ki, her yıl yeryüzüne yağan yağmurun toplam hacmi de tamı tamına buharlaşan miktara eşittir (513 trilyon m³)! Su molekülleri döngüde kaybolmaz veya artmaz; ayette ifade edildiği gibi tam bir 'kader' (hassas ölçü ve matematiksel oran) ile inmektedir.
        """.trimIndent(),
        keyScientificFacts = listOf(
        "Yıllık küresel buharlaşma ve yağış miktarı 513 trilyon m³ ile daima dengededir.",
        "Yağmur damlalarının düşme hızı hava sürtünmesi sayesinde terminal hıza (yaklaşık 9 m/s) sabitlenir.",
        "'Bi-kaderin' kelimesi hem hacimsel dengeyi hem de damlaların aerodinamik hız ölçüsünü ifade eder."
        ),
        quotesAndReferences = listOf(
        "Chahine, M. T. 'The hydrological cycle' (Nature, 1992)",
        "USGS Water Science School - 'The Water Cycle Summary'"
        ),
        tags = listOf("ölçülü yağmur", "zuhruf suresi", "hidroloji", "buharlaşma", "klimatoloji")
        ),
        MiracleItem(id = "geology_earth_round_egg_shape",
        title = "Dünya'nın Geoit / Oval Şekli (Dehâhâ)",
        subtitle = "Yerin düz değil, deve kuşu yumurtası gibi kutuplardan basık küremsi kılınması",
        category = MiracleCategory.GEOLOGY,
        surahName = "Nâzi'ât Sûresi",
        verseNumber = "30. Ayet",
        surahIndex = 79,
        verseIndex = 30,
        arabicVerse = "وَالْأَرْضَ بَعْدَ ذَٰلِكَ دَحَاهَا",
        verseTranslation = "Bundan sonra da yeryüzünü yuvarlatıp yaydı (deve kuşu yumurtası gibi geoit biçiminde şekillendirdi).",
        scientificDiscoveryTitle = "Geoid Geometri ve Eratosthenes'ten Uydu Ölçümlerine Küresellik",
        discoveryCentury = "Antik Dönem & 20. Yüzyıl (Jeodezi)",
        scientificExplanation = """
        Arapça 'Dehahâ' (دَحَاهَا) fiili köken olarak 'dahv' kelimesinden gelir. Klasik Arapça sözlüklerde (Lisanü'l-Arab vb.) 'Udhiyye / Medhâ' deve kuşunun yumurtasını bıraktığı yuvarlak zemin ve bizzat 'deve kuşu yumurtası' anlamındadır.
        
        Dünya tam bir küre değildir; ekvatordan şişkin, kutuplardan basık özel bir 'Geoit / Oblate Spheroid' şeklindedir ve bu form deve kuşu yumurtasının elipsoid basıklığı ile kusursuz bir benzerlik gösterir.
        """.trimIndent(),
        keyScientificFacts = listOf(
        "Dünya kutuplar arası 12.714 km, ekvator çapı 12.756 km olan basık bir geoittir.",
        "'Dehaha' fiili hem yayma/döşeme hem de elipsoid yumurta formu verme anlamlarını içerir.",
        "Kur'an düz dünya efsanesini 14 asır önce Arapçanın en zengin morfolojisiyle reddetmiştir."
        ),
        quotesAndReferences = listOf(
        "Ibn Manzur, 'Lisânü'l-Arab' (دحا maddesi)",
        "Torge, W., & Müller, J. 'Geodesy' (De Gruyter)"
        ),
        tags = listOf("dünya şekli", "dehaha", "naziat suresi", "geoit", "küre", "jeodezi")
        ),
        MiracleItem(id = "geology_lightning_flash_velocity",
        title = "Şimşeğin Işık Patlaması ve Kör Edici Plazma Sıcaklığı",
        subtitle = "Şimşek parıltısının gözleri kamaştırması ve saniyeler içindeki plazma deşarjı",
        category = MiracleCategory.GEOLOGY,
        surahName = "Nûr Sûresi",
        verseNumber = "43. Ayet",
        surahIndex = 24,
        verseIndex = 43,
        arabicVerse = "يَكَادُ سَنَا بَرْقِهِ يَذْهَبُ بِالْأَبْصَارِ",
        verseTranslation = "...Onun şimşeğinin parıltısı neredeyse gözleri alıp götürecek (kör edecek) gibi olur!",
        scientificDiscoveryTitle = "Yıldırım Fiziği, 30.000 Kelvin Plazma ve Fotoreseptör Körlüğü",
        discoveryCentury = "20. Yüzyıl (Elektrostatik & Plazma Fiziği)",
        scientificExplanation = """
        Şimşek ve yıldırım boşalması sırasında hava kanalı 30.000 Kelvin (Güneş yüzeyinin 5 katı) sıcaklığa ulaşır ve yüksek enerjili bir plazma sütununa dönüşür. Bu anda yayılan lüminesans ve yoğun foton akışı, insan retinasındaki rodopsin pigmentlerini anında ağartarak geçici veya kalıcı 'flaş körlüğüne' (Flash Blindness) yol açar.
        
        Nûr 43 ayetinde dolu bulutlarının anlatıldığı bağlamda şimşek parıltısının gözü alması (Yezhebu bi'l-ebsâr) elektrostatik deşarj fiziği ile biyolojik görme mekanizmasının tam kesişim noktasıdır.
        """.trimIndent(),
        keyScientificFacts = listOf(
        "Yıldırım kanalı Güneş yüzeyinden 5 kat daha sıcaktır (yaklaşık 30.000 °C).",
        "Göz bebekleri bu ani foton patlamasına karşı kasılamaz ve geçici körlük oluşur.",
        "Bulut içi buz kristalleri (dolu) arasındaki sürtünme dev şimşek potansiyelini tetikler."
        ),
        quotesAndReferences = listOf(
        "Rakov, V. A., & Uman, M. A. 'Lightning: Physics and Effects' (Cambridge University Press)",
        "NASA Lightning & Atmospheric Electricity Research"
        ),
        tags = listOf("şimşek", "yıldırım", "plazma", "nur suresi", "flaş körlüğü", "fizik")
        ),
        MiracleItem(id = "geology_earth_shrinkage_edges_erosion",
        title = "Yeryüzünün Uçlarından/Kenarlarından Eksiltilmesi",
        subtitle = "Kıtaların kıyılardan aşınması, kutuplardaki buzul kütlelerinin erimesi ve tektonik dalma-batma",
        category = MiracleCategory.GEOLOGY,
        surahName = "Ra'd Sûresi",
        verseNumber = "41. Ayet",
        surahIndex = 13,
        verseIndex = 41,
        arabicVerse = "أَوَلَمْ يَرَوْا أَنَّا نَأْتِي الْأَرْضَ نَنْقُصُهَا مِنْ أَطْرَافِهَا",
        verseTranslation = "Görmüyorlar mı ki, biz yeryüzüne gelip onu uçlarından/kenarlarından eksiltmekteyiz...",
        scientificDiscoveryTitle = "Kıyı Erozyonu, Kutup Buzul Kaybı ve Subdüksiyon (Dalma-Batma) Zonları",
        discoveryCentury = "20. ve 21. Yüzyıl (Jeomorfoloji)",
        scientificExplanation = """
        Arapça 'Nenküsühâ min etrâfihâ' (نَنْقُصُهَا مِنْ أَطْرَافِهَا) ifadesi 'onu uçlarından, kenarlarından eksiltiyoruz' demektir.
        
        Jeolojik ve coğrafi açıdan bu ayet üç temel küresel gerçeğe işaret eder:
        1. Dalma-Batma Zonları (Subduction): Kıtaların kenarlarındaki okyanus kabukları mantoya batarak erir ve karasal alanların uçları eksilir.
        2. Kutup Buzulları: Dünya'nın uç noktaları olan Kuzey ve Güney kutup buzul kütleleri eriyerek deniz seviyesini yükseltir ve kıyı şeritlerini daraltır.
        3. Kıyı Erozyonu: Dalgalar ve rüzgarlar kıta sınırlarını sürekli aşındırır.
        """.trimIndent(),
        keyScientificFacts = listOf(
        "Okyanus hendeklerinde litosfer levhaları mantoya dalarak yerkabuğundan kütle eksiltir.",
        "Dünya'nın uçları (kutuplar) küresel iklim döngüleriyle sürekli hacim kaybeder.",
        "Erozyon ve tektonik dalma Dünya'nın dinamik kütle dengesini sağlar."
        ),
        quotesAndReferences = listOf(
        "Le Pichon, X. 'Sea-floor spreading and continental drift' (Journal of Geophysical Research)",
        "IPCC Climate Change Reports - 'Polar Ice Caps and Sea Level Rise'"
        ),
        tags = listOf("uçlardan eksilme", "rad suresi", "dalma batma", "kutuplar", "erozyon", "jeoloji")
        ),
        MiracleItem(id = "geology_petroleum_formation_pastures",
        title = "Petrol ve Fosil Yakıtların Kararmış Bitki Atıklarından Oluşumu",
        subtitle = "Yeşil otlakların çürütülüp kararmış bir tortu (Ahvâ) haline getirilmesi",
        category = MiracleCategory.GEOLOGY,
        surahName = "A'lâ Sûresi",
        verseNumber = "4-5. Ayetler",
        surahIndex = 87,
        verseIndex = 4,
        arabicVerse = "وَالَّذِي أَخْرَجَ الْمَرْعَىٰ * فَجَعَلَهُ غُثَاءً أَحْوَىٰ",
        verseTranslation = "O ki yeşil otlakları (bitkileri) çıkardı; sonra da onu kararmış bir sel süprüntüsüne / tortu atığına (katran/petrol hammaddesine) çevirdi.",
        scientificDiscoveryTitle = "Hidrokarbon ve Petrolün Biyojenik Fosil Oluşumu",
        discoveryCentury = "19. ve 20. Yüzyıl (Organik Jeokimya)",
        scientificExplanation = """
        Arapça 'Gusâen Ahvâ' (غُثَاءً أَحْوَىٰ) ifadesinde; 'Gusâ' çürümüş organik süprüntü, bitki tortusu; 'Ahvâ' ise siyaha çalan koyu esmer, kahverengi-siyah katran rengi demektir.
        
        Jeokimyaya göre petrol ve doğal gaz, milyonlarca yıl önce yaşamış yeşil bitki, alg ve plankton örtüsünün yer altında yüksek basınç ve ısıyla çürüyerek koyu siyah renkli viskoz hidrokarbon sıvılarına (ham petrole) dönüşmesiyle meydana gelmiştir. Ayet bitki örtüsünün bu siyahlaşma döngüsünü 1400 yıl önceden özetlemiştir.
        """.trimIndent(),
        keyScientificFacts = listOf(
        "Petrol, organik bitki ve fitoplankton artıklarının anaerobik bozulmasıyla oluşur.",
        "'Gusâen Ahvâ' tamlaması petrolün hidrokarbon tortusu ve siyah kıvamını birebir betimler.",
        "Fosil yakıtlar antik biyosferin yeraltındaki dönüşüm ürünüdür."
        ),
        quotesAndReferences = listOf(
        "Tissot, B. P., & Welte, D. H. 'Petroleum Formation and Occurrence' (Springer)",
        "Hunt, J. M. 'Petroleum Geochemistry and Geology'"
        ),
        tags = listOf("petrol", "ahva", "ala suresi", "fosil yakıt", "organik kimya", "jeoloji")
        ),
        MiracleItem(id = "geology_clay_ceramic_earth_composition",
        title = "Toprağın Kil ve Seramik / Pişmiş Çömlek Yapısı",
        subtitle = "İnsanın hammaddesi olan toprağın mineral/kil (Fahhâr) yapısı ve biyomoleküler köken",
        category = MiracleCategory.GEOLOGY,
        surahName = "Rahmân Sûresi",
        verseNumber = "14. Ayet",
        surahIndex = 55,
        verseIndex = 14,
        arabicVerse = "خَلَقَ الْإِنْسَانَ مِنْ صَلْصَالٍ كَالْفَخَّارِ",
        verseTranslation = "O, insanı pişmiş çömlek gibi çınlayan kuru bir balçıktan (kilden) yarattı.",
        scientificDiscoveryTitle = "Kil Minerallerinin Biyo-Katalitik Rolü ve Prebiyotik Yaşam",
        discoveryCentury = "20. ve 21. Yüzyıl (Biyokimya & Mineraloji)",
        scientificExplanation = """
        Arapça 'Salsâl' (صَلْصَالٍ) şekil verilebilir mineral kili; 'Fahhâr' (كَالْفَخَّارِ) ise fırınlanmış seramik / pişmiş toprak demektir.
        
        Modern moleküler biyoloji ve inorganik kimya (örneğin Dr. A. Graham Cairns-Smith'in Kil Hipotezi), kil kristallerinin tabakalı silikat yapısının RNA ve protein zincirlerinin bir araya gelmesinde doğal bir şablon ve katalizör vazifesi gördüğünü ortaya koymuştur. Ayrıca insan bedenindeki tüm elementler (karbon, demir, kalsiyum, silisyum, potasyum) doğrudan toprak mineralleriyle birebir örtüşür.
        """.trimIndent(),
        keyScientificFacts = listOf(
        "Kil mineralleri organik molekülleri yoğunlaştırıp aminoasit polimerizasyonunu hızlandırır.",
        "İnsan bedeninin temel element bileşimi ile verimli balçık toprağı elementleri denktir.",
        "Salsâl ve Fahhâr ifadeleri toprağın mineral kristalizasyonunu ve dayanıklılığını vurgular."
        ),
        quotesAndReferences = listOf(
        "Cairns-Smith, A. G. 'Genetic Takeover and the Mineral Origins of Life' (Cambridge University Press)",
        "Bernal, J. D. 'The Physical Basis of Life'"
        ),
        tags = listOf("balçık", "kil", "fahhar", "rahman suresi", "toprak mineralleri", "biyokimya")
        )
    )
}
