package com.example.data

enum class DailyPrayer(val title: String, val rakats: Int) {
    FAJR("Sabah", 2), DHUHR("Öğle", 4), ASR("İkindi", 4), MAGHRIB("Akşam", 3), ISHA("Yatsı", 4)
}

enum class PrayerPosture { INTENTION, TAKBIR, STANDING, BOWING, UPRIGHT, PROSTRATION, SITTING, GREETING }
data class PrayerLessonStep(
    val id: String, val rakat: Int, val title: String, val instruction: String,
    val posture: PrayerPosture, val recitations: List<String> = emptyList()
)
data class PrayerRecitation(val id: String, val title: String, val arabic: String, val reading: String)

object PrayerLessons {
    const val SOURCE = "https://namaz.diyanet.gov.tr/namaz/html/kutuphane/HTML/NamazIlmihali/assets/basic-html/page-49.html"
    const val ABLUTION_SOURCE = "https://kurul.diyanet.gov.tr/Cevap-Ara/36/abdest-nedir-ve-nasil-alinir"
    const val SCOPE = "Hanefî usulü • Tek başına • Beş vaktin farzı"
    const val NOTE = "Bu ders, namaz öncesi öğrenme ve tekrar içindir. Sünnet, vitir, cemaat ve seferîlik hükümleri bu akışın dışında tutulmuştur. Diğer mezheplerde bazı uygulamalar farklıdır."

    val preparation = listOf(
        "Temizlik" to "Abdestli olun; gusül gerekiyorsa önce gusül alın. Beden, kıyafet ve namaz yeri temiz olmalıdır.",
        "Örtünme" to "Namazda örtülmesi gereken yerleri örten temiz ve uygun kıyafet giyin.",
        "Vakit ve kıble" to "Kılacağınız namazın vakti girmiş olmalı. Kâbe yönüne dönün.",
        "Niyet" to "Hangi namazı kılacağınıza kalben karar verin. Niyetin dille söylenmesi şart değildir."
    )
    val ablution = listOf(
        "Niyet edip besmele çekin; elleri, parmak aralarını da temizleyerek üç kez yıkayın.",
        "Ağza ve burna ayrı ayrı üçer kez su alıp temizleyin.",
        "Yüzü üç kez yıkayın.",
        "Önce sağ, ardından sol kolu dirsekler dâhil üçer kez yıkayın.",
        "Islak elle başı bir kez mesh edin. Hanefîlerde en az dörtte biri mesh edilir.",
        "Ellerinizi yeniden ıslatıp kulakların içini ve dışını, ardından ellerin arkasıyla boynu mesh edin.",
        "Sağdan başlayarak ayakları topuk kemikleri ve parmak araları dâhil yıkayın."
    )

    /** Individual obligatory prayers only: no extra surah in rakats three/four. */
    fun steps(prayer: DailyPrayer): List<PrayerLessonStep> = buildList {
        add(PrayerLessonStep("intention", 0, "Niyet",
            "${prayer.title} namazının ${prayer.rakats} rekât farzını Allah rızası için kılmaya niyet edin.",
            PrayerPosture.INTENTION))
        add(PrayerLessonStep("takbir", 1, "İftitah tekbiri",
            "Ayakta, kıbleye dönük olarak ellerinizi kaldırın; tekbirden sonra ellerinizi bağlayın.",
            PrayerPosture.TAKBIR, listOf("takbir")))
        for (rakat in 1..prayer.rakats) {
            add(PrayerLessonStep("${rakat}-standing", rakat, "Kıyam ve kıraat",
                when (rakat) {
                    1 -> "Sübhaneke, eûzü-besmele ve Fâtiha okuyun. Âmin dedikten sonra bir sûre ekleyin; burada Kevser örneği var."
                    2 -> "Tekbirle ayağa kalkın. Besmele, Fâtiha ve âminin ardından bir sûre okuyun; burada İhlâs örneği var."
                    else -> "Tekbirle ayağa kalkın. Besmele ile Fâtiha okuyup âmin deyin. Farzın bu rekâtında ek sûre okunmaz."
                }, PrayerPosture.STANDING,
                (if (rakat == 1) listOf("subhaneke", "euzu") else emptyList()) +
                    listOf("fatiha") + when (rakat) { 1 -> listOf("kevser"); 2 -> listOf("ihlas"); else -> emptyList() }))
            add(PrayerLessonStep("${rakat}-ruku", rakat, "Rükû",
                "Tekbirle eğilin, ellerinizi dizlerinize koyun. Rükûda sakinleşin; tesbihi üç kez söyleyin.",
                PrayerPosture.BOWING, listOf("ruku")))
            add(PrayerLessonStep("${rakat}-upright", rakat, "Kavme",
                "Doğrulurken ilk sözü, tam dik durduğunuzda ikinci sözü söyleyin. Acele etmeyin.",
                PrayerPosture.UPRIGHT, listOf("kavme")))
            add(PrayerLessonStep("${rakat}-sujud-1", rakat, "Birinci secde",
                "Tekbirle secdeye inin. Alın, burun, eller, dizler ve ayak parmakları yere temas eder. Tesbihi üç kez söyleyin.",
                PrayerPosture.PROSTRATION, listOf("secde")))
            add(PrayerLessonStep("${rakat}-sitting", rakat, "İki secde arası",
                "Tekbirle başınızı kaldırıp oturun. Dik ve sakin bir oturuştan sonra ikinci secdeye geçin.",
                PrayerPosture.SITTING))
            add(PrayerLessonStep("${rakat}-sujud-2", rakat, "İkinci secde",
                "Tekbirle yeniden secde edin. Aynı tesbihi üç kez söyleyin.",
                PrayerPosture.PROSTRATION, listOf("secde")))
            if (rakat == 2 || rakat == prayer.rakats) {
                val final = rakat == prayer.rakats
                add(PrayerLessonStep("${rakat}-tashahhud", rakat, if (final) "Son oturuş" else "İlk oturuş",
                    if (final) "Tekbirle oturun. Tahiyyat, Salli-Bârik ve Rabbenâ dualarını okuyun."
                    else "Tekbirle oturun ve yalnız Tahiyyat okuyun. Ardından üçüncü rekâta kalkılır.",
                    PrayerPosture.SITTING, if (final) listOf("tahiyyat", "salawat", "rabbena") else listOf("tahiyyat")))
            }
        }
        add(PrayerLessonStep("salam", prayer.rakats, "Selâm",
            "Başınızı önce sağa, sonra sola çevirerek her iki yöne selâm verin.",
            PrayerPosture.GREETING, listOf("salam")))
    }

    val recitations = listOf(
        PrayerRecitation("takbir", "Tekbir", "اللَّهُ أَكْبَرُ", "Allâhü ekber."),
        PrayerRecitation("subhaneke", "Sübhaneke",
            "سُبْحَانَكَ اللَّهُمَّ وَبِحَمْدِكَ وَتَبَارَكَ اسْمُكَ وَتَعَالَى جَدُّكَ وَلَا إِلَهَ غَيْرُكَ",
            "Sübhânekellâhümme ve bi hamdik. Ve tebârakesmük. Ve teâlâ ceddük. Ve lâ ilâhe gayrük."),
        PrayerRecitation("euzu", "Eûzü", "أَعُوذُ بِاللَّهِ مِنَ الشَّيْطَانِ الرَّجِيمِ", "Eûzü billâhi mineşşeytânirracîm."),
        PrayerRecitation("fatiha", "Besmele ve Fâtiha",
            "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ\nالْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ ۞ الرَّحْمَنِ الرَّحِيمِ ۞ مَالِكِ يَوْمِ الدِّينِ ۞ إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ ۞ اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ ۞ صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ",
            "Bismillâhirrahmânirrahîm. Elhamdü lillâhi rabbil'âlemîn. Errahmânirrahîm. Mâliki yevmiddîn. İyyâke na'büdü ve iyyâke neste'în. İhdinessırâtel müstakîm. Sırâtellezîne en'amte aleyhim gayrilmağdûbi aleyhim veleddâllîn. (Âmin.)"),
        PrayerRecitation("kevser", "Kevser",
            "إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ ۞ فَصَلِّ لِرَبِّكَ وَانْحَرْ ۞ إِنَّ شَانِئَكَ هُوَ الْأَبْتَرُ",
            "İnnâ a'taynâkel kevser. Fesalli lirabbike venhar. İnne şânieke hüvel ebter."),
        PrayerRecitation("ihlas", "İhlâs",
            "قُلْ هُوَ اللَّهُ أَحَدٌ ۞ اللَّهُ الصَّمَدُ ۞ لَمْ يَلِدْ وَلَمْ يُولَدْ ۞ وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ",
            "Kul hüvellâhü ehad. Allâhüssamed. Lem yelid ve lem yûled. Ve lem yekün lehû küfüven ehad."),
        PrayerRecitation("ruku", "Rükû tesbihi", "سُبْحَانَ رَبِّيَ الْعَظِيمِ", "Sübhâne rabbiyel azîm. (Üç kez.)"),
        PrayerRecitation("kavme", "Doğrulurken", "سَمِعَ اللَّهُ لِمَنْ حَمِدَهُ\nرَبَّنَا لَكَ الْحَمْدُ",
            "Semiallâhü limen hamideh.\nRabbenâ lekel hamd."),
        PrayerRecitation("secde", "Secde tesbihi", "سُبْحَانَ رَبِّيَ الْأَعْلَى", "Sübhâne rabbiyel a'lâ. (Üç kez.)"),
        PrayerRecitation("tahiyyat", "Tahiyyat",
            "التَّحِيَّاتُ لِلَّهِ وَالصَّلَوَاتُ وَالطَّيِّبَاتُ ۞ السَّلَامُ عَلَيْكَ أَيُّهَا النَّبِيُّ وَرَحْمَةُ اللَّهِ وَبَرَكَاتُهُ ۞ السَّلَامُ عَلَيْنَا وَعَلَى عِبَادِ اللَّهِ الصَّالِحِينَ ۞ أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا اللَّهُ وَأَشْهَدُ أَنَّ مُحَمَّدًا عَبْدُهُ وَرَسُولُهُ",
            "Ettehiyyâtü lillâhi vessalavâtü vettayyibât. Esselâmü aleyke eyyühennebiyyü ve rahmetullâhi ve berekâtüh. Esselâmü aleynâ ve alâ ibâdillâhissâlihîn. Eşhedü en lâ ilâhe illallâh ve eşhedü enne Muhammeden abdühû ve resûlüh."),
        PrayerRecitation("salawat", "Salli ve Bârik",
            "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ كَمَا صَلَّيْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ إِنَّكَ حَمِيدٌ مَجِيدٌ ۞ اللَّهُمَّ بَارِكْ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ كَمَا بَارَكْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ إِنَّكَ حَمِيدٌ مَجِيدٌ",
            "Allâhümme salli alâ Muhammedin ve alâ âli Muhammed. Kemâ salleyte alâ İbrâhîme ve alâ âli İbrâhîm. İnneke hamîdün mecîd.\nAllâhümme bârik alâ Muhammedin ve alâ âli Muhammed. Kemâ bârekte alâ İbrâhîme ve alâ âli İbrâhîm. İnneke hamîdün mecîd."),
        PrayerRecitation("rabbena", "Rabbenâ",
            "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ ۞ رَبَّنَا اغْفِرْ لِي وَلِوَالِدَيَّ وَلِلْمُؤْمِنِينَ يَوْمَ يَقُومُ الْحِسَابُ",
            "Rabbenâ âtinâ fiddünyâ haseneten ve fil'âhireti haseneten ve kınâ azâbennâr.\nRabbenağfirlî ve li-vâlideyye ve lil-mü'minîne yevme yekûmül hisâb."),
        PrayerRecitation("salam", "Selâm", "السَّلَامُ عَلَيْكُمْ وَرَحْمَةُ اللَّهِ", "Esselâmü aleyküm ve rahmetullâh.")
    ).associateBy { it.id }
}
