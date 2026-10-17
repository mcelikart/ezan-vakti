package com.example.data

data class Hadith(
    val id: String,
    val text: String,
    val arabicText: String? = null,
    val category: String,
    val source: String,
    val explanation: String? = null
)

object HadithData {
    val allHadiths = listOf(
        Hadith(
            id = "1",
            text = "Ameller niyetlere göredir. Herkese niyet ettiği şey vardır.",
            arabicText = "إنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى",
            category = "İman",
            source = "Buhârî, Bed'ü'l-Vahy, 1; Müslim, İmâret, 155",
            explanation = "Yapılan her hayırlı amelin değeri niyetimize bağlıdır. Allah rızası için yapılan az ibadet, gösteriş için yapılan çok ibadetten daha üstündür."
        ),
        Hadith(
            id = "2",
            text = "Kolaylaştırınız, zorlaştırmayınız; müjdeleyiniz, nefret ettirmeyiniz.",
            arabicText = "يَسِّرُوا وَلَا تُعَسِّرُوا، وَبَشِّرُوا وَلَا تُنَفِّرُوا",
            category = "Ahlak",
            source = "Buhârî, İlim, 11; Müslim, Cihâd, 6",
            explanation = "İslam dini kolaylık dinidir. Din hizmetinde bulunan veya birine İslam'ı anlatan kimseler yapıcı, müjdeleyici ve sabırlı bir dil kullanmalıdır."
        ),
        Hadith(
            id = "3",
            text = "Sizin en hayırlınız, ahlakı en güzel olanınızdır.",
            arabicText = "إِنَّ مِنْ خِيَارِكُمْ أَحْسَنَكُمْ أَخْلَاقًا",
            category = "Ahlak",
            source = "Buhârî, Edeb, 39; Müslim, Fezâil, 68",
            explanation = "Müslümanın olgunluğu güzel ahlakıyla ölçülür. Başkalarına iyi davranmak, güler yüzlü olmak ve kimseyi incitmemek en yüce erdemdir."
        ),
        Hadith(
            id = "4",
            text = "Komşusu açken tok yatan bizden değildir.",
            arabicText = "مَا آمَنَ بِي مَنْ بَاتَ شَبْعَانًا وَجَارُهُ جَائِعٌ إِلَى جَنْبِهِ وَهُوَ يَعْلَمُ بِهِ",
            category = "Kardeşlik",
            source = "Hâkim, Müstedrek, II, 15; Taberânî",
            explanation = "İslam sosyal adalete ve yardımlaşmaya büyük önem verir. Yakınımızdaki insanların dertleriyle dertlenmek imanımızın bir gereğidir."
        ),
        Hadith(
            id = "5",
            text = "Müslüman, dilinden ve elinden Müslümanların emin olduğu kimsedir.",
            arabicText = "الْمُسْلِمُ مَنْ سَلِمَ الْمُسْلِمُونَ مِنْ لِسَانِهِ وَيَدِهِ",
            category = "Kardeşlik",
            source = "Buhârî, Îmân, 4; Müslim, Îmân, 64",
            explanation = "Güvenilirlik Müslümanın şiarıdır. Bir mümin asla eliyle (zarar vererek) veya diliyle (gıybet, iftira, yalan söyleyerek) başkalarını incitmemelidir."
        ),
        Hadith(
            id = "6",
            text = "Dua, ibadetin özüdür.",
            arabicText = "الدُّعَاءُ هُوَ الْعِبَادَةُ",
            category = "İbadet",
            source = "Tirmizî, Deavât, 1; Ebû Dâvûd, Vitr, 23",
            explanation = "Dua, kulun acziyetini kabul edip her şeyin sahibi olan Yaratıcıya sığınmasıdır. Allah'a yönelmek ibadetin ruhunu oluşturur."
        ),
        Hadith(
            id = "7",
            text = "Temizlik imanın yarısıdır.",
            arabicText = "الطُّهُورُ شَطْرُ الإِيمَانِ",
            category = "İbadet",
            source = "Müslim, Tahâret, 1; Tirmizî, Deavât, 86",
            explanation = "Mümin hem bedenini, elbisesini ve çevresini temiz tutmalı hem de kalbini kötü duygu ve düşüncelerden arındırmalıdır."
        ),
        Hadith(
            id = "8",
            text = "Hiçbir baba, çocuğuna güzel ahlaktan daha hayırlı bir miras bırakamaz.",
            arabicText = "مَا نَحَلَ وَالِدٌ وَلَدًا مِنْ نُحْلٍ أَفْضَلَ مِنْ أَدَبٍ حَسَنٍ",
            category = "Ahlak",
            source = "Tirmizî, Birr, 33",
            explanation = "Maddi miraslar tükenir ancak güzel ahlak ve dürüst karakter evladın hem dünyasını hem ahiretini güzelleştiren kalıcı bir hazinedir."
        ),
        Hadith(
            id = "9",
            text = "Kişi sevdiği ile beraberdir.",
            arabicText = "الْمَرْءُ مَعَ مَنْ أَحَبَّ",
            category = "Kardeşlik",
            source = "Buhârî, Edeb, 96; Müslim, Birr, 165",
            explanation = "İnsan kimleri sever ve kimlerin yolundan giderse, dünyada ve ahirette onlarla haşrolunur. Dostlarımızı iyi seçmeliyiz."
        ),
        Hadith(
            id = "10",
            text = "Allah sizin dış görünüşünüze ve mallarınıza bakmaz. O, sadece kalplerinize ve amellerinize bakar.",
            arabicText = "إِنَّ اللَّهَ لَا يَنْظُرُ إِلَى صُوَرِكُمْ وَأَمْوَالِكُمْ وَلَكِنْ يَنْظُرُ إِلَى قُلُوبِكُمْ وَأَعْمَالِكُمْ",
            category = "İman",
            source = "Müslim, Birr, 34; İbn Mâce, Zühd, 9",
            explanation = "İnsanlar arasında üstünlük takva, temiz bir kalp ve salih amellerledir. Maddi zenginlik veya dış görünüş Allah katında değer ölçütü değildir."
        ),
        Hadith(
            id = "11",
            text = "Her iyilik bir sadakadır.",
            arabicText = "كُلُّ مَعْرُوفٍ صَدَقَةٌ",
            category = "Sosyal Hayat",
            source = "Buhârî, Edeb, 33; Müslim, Zekât, 52",
            explanation = "Sadaka sadece para ile olmaz. Bir insana yol göstermek, güler yüzle selam vermek veya bir engeli yoldan kaldırmak da sadakadır."
        ),
        Hadith(
            id = "12",
            text = "İki nimet vardır ki insanların çoğu onların kıymetini bilmez: Sağlık ve boş vakit.",
            arabicText = "نِعْمَتَانِ مَغْبُونٌ فِيهِمَا كَثِيرٌ مِنَ النَّاسِ: الصِّحَّةُ وَالْفَرَاغُ",
            category = "Sosyal Hayat",
            source = "Buhârî, Rikâk, 1; Tirmizî, Zühd, 1",
            explanation = "Sağlıklı olmak ve zamanı verimli kullanabilmek, geri dönüşü olmayan büyük fırsatlardır. Mümin her anını hayırlı işlerle değerlendirmelidir."
        ),
        Hadith(
            id = "13",
            text = "İnsanların en hayırlısı insanlara faydalı olanıdır.",
            arabicText = "خَيْرُ النَّاسِ أَنْفَعُهُمْ لِلنَّاسِ",
            category = "Kardeşlik",
            source = "Feyzü'l-Kadîr, 3/480; Taberânî",
            explanation = "Hayatın gayesi bencil yaşamak değil, başkalarına el uzatmak, toplumun huzuruna katkıda bulunmak ve iyilik üretmektir."
        ),
        Hadith(
            id = "14",
            text = "Müminlerin iman bakımından en olgunu, ahlakı en güzel olanıdır.",
            arabicText = "أَكْمَلُ الْمُؤْمِنِينَ إِيمَانًا أَحْسَنُهُمْ خُلُقًا",
            category = "İman",
            source = "Ebû Dâvûd, Sünnet, 15; Tirmizî, Radâ, 11",
            explanation = "İman ile ahlak arasında doğrudan bir ilişki vardır. İnancı güçlü olan bir kimsenin ahlakı da nezaketli, dürüst ve hoşgörülü olur."
        ),
        Hadith(
            id = "15",
            text = "Hiçbiriniz kendisi için istediğini kardeşi için de istemedikçe hakkıyla iman etmiş olmaz.",
            arabicText = "لَا يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لِأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ",
            category = "Kardeşlik",
            source = "Buhârî, Îmân, 7; Müslim, Îmân, 71",
            explanation = "Gerçek kardeşlik empati yapmayı gerektirir. Kendimiz için arzuladığımız barış, esenlik ve güzel nimetleri din kardeşlerimiz için de dilemeliyiz."
        ),
        Hadith(
            id = "16",
            text = "İbadetin en hayırlısı az da olsa devamlı olanıdır.",
            arabicText = "أَحَبُّ الأَعْمَالِ إِلَى اللَّهِ أَدْوَمُهَا وَإِنْ قَلَّ",
            category = "İbadet",
            source = "Buhârî, Rikâk, 18; Müslim, Müsâfirîn, 218",
            explanation = "Bir gün aşırı ibadet edip sonra terk etmek yerine, her gün düzenli olarak yapılan az miktardaki ibadet ve hayırlı iş Allah katında daha sevimlidir."
        ),
        Hadith(
            id = "17",
            text = "Güzel söz sadakadır.",
            arabicText = "وَالْكَلِمَةُ الطَّيِّبَةُ صَدَقَةٌ",
            category = "Ahlak",
            source = "Buhârî, Cihâd, 128; Müslim, Zekât, 56",
            explanation = "Konuşurken nazik olmak, gönül alıcı sözler sarf etmek ve insanları kırmamak hem ahlaki bir görev hem de manevi bir kazançtır."
        ),
        Hadith(
            id = "18",
            text = "Cennet annelerin ayakları altındadır.",
            arabicText = "الْجَنَّةُ تَحْتَ أَقْدَامِ الأُمَّهَاتِ",
            category = "Ahlak",
            source = "Nesâî, Cihâd, 6; Ahmed b. Hanbel",
            explanation = "Anneye saygı göstermek, onun duasını almak ve ona hizmet etmek, cennet kapılarını aralayan en büyük vesilelerdendir."
        ),
        Hadith(
            id = "19",
            text = "Bizi aldatan bizden değildir.",
            arabicText = "مَنْ غَشَّنَا فَلَيْسَ مِنَّا",
            category = "Sosyal Hayat",
            source = "Müslim, Îmân, 164; Ebû Dâvûd, Büyû', 50",
            explanation = "Müslüman dürüst ve şeffaf olmalıdır. Ticarette, sosyal hayatta veya ikili ilişkilerde hileye, yalana ve aldatmaya başvurmak haramdır."
        ),
        Hadith(
            id = "20",
            text = "İlim öğrenmek her Müslümana farzdır.",
            arabicText = "طَلَبُ الْعِلْمِ فَرِيضَةٌ عَلَى كُلِّ مُسْلِمٍ",
            category = "İlim",
            source = "İbn Mâce, Mukaddime, 17",
            explanation = "Cehalet İslam'ın en büyük düşmanıdır. Her mümin hem dini görevlerini yerine getirecek hem de dünyaya faydalı olacak ilimleri öğrenmekle mükelleftir."
        ),
        Hadith(
            id = "21",
            text = "Kolaylaştırınız, zorlaştırmayınız; müjdeleyiniz, nefret ettirmeyiniz.",
            arabicText = "يَسِّرُوا وَلَا تُعَسِّرُوا وَبَشِّرُوا وَلَا تُنَفِّرُوا",
            category = "Ahlak",
            source = "Buhârî, İlim, 11; Müslim, Cihâd, 6",
            explanation = "İnsanlara dini ve hayırlı işleri sevdirerek anlatmak esastır. Baskı veya korkutma yerine teşvik edici ve merhametli olunmalıdır."
        ),
        Hadith(
            id = "22",
            text = "Sabır, ilk darbe anında gösterilen metanettir.",
            arabicText = "إِنَّمَا الصَّبْرُ عِنْدَ الصَّدْمَةِ الأُولَى",
            category = "Ahlak",
            source = "Buhârî, Cenâiz, 32; Müslim, Cenâiz, 14",
            explanation = "Asıl kıymetli olan sabır, musibetin ilk başa geldiği, acının en taze olduğu anda isyan etmeden rıza gösterebilmektir."
        ),
        Hadith(
            id = "23",
            text = "Doğruluktan ayrılmayınız. Doğruluk iyiliğe, iyilik de cennete götürür.",
            arabicText = "عَلَيْكُمْ بِالصِّدْقِ فَإِنَّ الصِّدْقَ يَهْدِي إِلَى الْبِرِّ وَإِنَّ الْبِرَّ يَهْدِي إِلَى الْجَنَّةِ",
            category = "Ahlak",
            source = "Müslim, Birr, 105; Ebû Dâvûd, Edeb, 80",
            explanation = "Doğruluk, sözde ve özde samimi olmaktır. Yalan ise kötülüklerin anasıdır. Mümin dürüstlüğü hayat felsefesi haline getirmelidir."
        ),
        Hadith(
            id = "24",
            text = "Hayra vesile olan, o hayrı yapan gibidir.",
            arabicText = "الدَّالُّ عَلَى الْخَيْرِ كَفَاعِلِهِ",
            category = "Kardeşlik",
            source = "Tirmizî, İlim, 14; Ahmed b. Hanbel",
            explanation = "Güzel işlerin yayılmasına yardımcı olmak, başkalarını iyiliğe teşvik etmek veya bir hayra ön ayak olmak, o hayrın sevabına ortak kılar."
        ),
        Hadith(
            id = "25",
            text = "İki göz vardır ki cehennem ateşi onlara dokunmaz: Allah korkusundan ağlayan göz ve Allah yolunda nöbet tutan göz.",
            arabicText = "عَيْنَانِ لَا تَمَسُّهُمَا النَّارُ: عَيْنٌ بَكَتْ مِنْ خَشْيَةِ اللَّهِ، وَعَيْنٌ بَاتَتْ تَحْرُسُ فِي سَبِيلِ اللَّهِ",
            category = "İman",
            source = "Tirmizî, Fedâilü'l-Cihâd, 12",
            explanation = "İçten gelen samimi pişmanlık/haşyet yaşları dökmek ve vatan, millet, mukaddesat uğruna canı pahasına bekçilik yapmak cehennemden koruyan yüce işlerdir."
        )
    )
}
