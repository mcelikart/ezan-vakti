package com.example.data

data class Hadith(
    val id: String,
    val category: String,
    val arabic: String,
    val turkish: String,
    val source: String,
    val narrator: String = "",
    val isArbain: Boolean = false,
    val arbainNumber: Int = 0,
    val explanation: String = "Bu hadis-i şerif, müminin kalbî istikametini, ahlâkî olgunluğunu ve kulluk bilincini inşa eden temel nebevî ilkelerden birini beyan etmektedir.",
    val authenticity: String = "Sahih"
) {
    val text: String get() = turkish
    val arabicText: String get() = arabic
}

object HadithData {
    val categories = listOf(
        "Hepsi",
        "40 Hadis",
        "İman",
        "İbadet",
        "Ahlak",
        "Muamelat",
        "İlim & Hikmet",
        "Aile & Merhamet",
        "Sabır & Tevekkül",
        "Dua & Zikir"
    )

    val allHadiths = listOf(
        // 40 HADİS (İMAM NEVEVİ - ERBAİN SEÇKİLERİ)
        Hadith(
            id = "1",
            category = "İman",
            arabic = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى",
            turkish = "Ameller niyetlere göredir. Herkese ancak niyet ettiği şey vardır. Kimin hicreti Allah ve Resûlü için ise, onun hicreti Allah ve Resûlü'nedir.",
            source = "Buhârî, Bed'ü'l-Vahy, 1; Müslim, İmâre, 155",
            narrator = "Hz. Ömer (r.a.)",
            isArbain = true,
            arbainNumber = 1,
            explanation = "İslam fıkhının ve ahlâkının temel direklerinden biridir. Yapılan bir ibadetin veya iyiliğin Allah katındaki değerini belirleyen ana unsur ihlas ve halis niyettir.",
            authenticity = "Muttefekun Aleyh"
        ),
        Hadith(
            id = "2",
            category = "İman",
            arabic = "بُنِيَ الإِسْلامُ عَلَى خَمْسٍ: شَهَادَةِ أَنْ لا إِلَهَ إِلا اللَّهُ وَأَنَّ مُحَمَّدًا رَسُولُ اللَّهِ، وَإِقَامِ الصَّلاةِ، وَإِيتَاءِ الزَّكَاةِ، وَالْحَجِّ، وَصَوْمِ رَمَضَانَ",
            turkish = "İslam beş esas üzerine bina edilmiştir: Allah'tan başka ilah olmadığına ve Muhammed'in Allah'ın Resûlü olduğuna şahitlik etmek, namazı dosdoğru kılmak, zekâtı vermek, Kâbe'yi haccetmek ve Ramazan orucunu tutmak.",
            source = "Buhârî, Îmân, 1; Müslim, Îmân, 21",
            narrator = "Hz. Abdullah ibn Ömer (r.a.)",
            isArbain = true,
            arbainNumber = 3,
            explanation = "İslam binasını ayakta tutan beş temel rüknü özetler. Kelime-i şehadet kalbi ve imanı, namaz bedeni ve ruhu, zekât malı, oruç nefsi, hac ise tüm varlığı arındırır.",
            authenticity = "Muttefekun Aleyh"
        ),
        Hadith(
            id = "3",
            category = "Ahlak",
            arabic = "الدِّينُ النَّصِيحَةُ قُلْنَا: لِمَنْ؟ قَالَ: لِلَّهِ وَلِكِتَابِهِ وَلِرَسُولِهِ وَلأَئِمَّةِ الْمُسْلِمِينَ وَعَامَّتِهِمْ",
            turkish = "Din samimiyettir (nasihattir). 'Kime karşı?' diye sorduk. Buyurdu ki: 'Allah'a, Kitabına, Resûlüne, Müslümanların yöneticilerine ve bütün Müslümanlara karşı.'",
            source = "Müslim, Îmân, 95",
            narrator = "Hz. Temîm ed-Dârî (r.a.)",
            isArbain = true,
            arbainNumber = 7,
            explanation = "Buradaki 'nasihat' kelimesi içtenlik, sadakat, hayırhahlık ve gösterişten uzak samimiyet anlamındadır. Mümin her ilişkisinde dürüst ve hayır isteyen kişidir.",
            authenticity = "Sahih"
        ),
        Hadith(
            id = "4",
            category = "Ahlak",
            arabic = "مِنْ حُسْنِ إِسْلامِ الْمَرْءِ تَرْكُهُ مَا لا يَعْنِيهِ",
            turkish = "Kişinin kendisini ilgilendirmeyen şeyleri (boş söz ve işleri) terk etmesi, Müslümanlığının güzelliğindendir.",
            source = "Tirmizî, Zühd, 11; İbn Mâce, Fiten, 12",
            narrator = "Hz. Ebû Hüreyre (r.a.)",
            isArbain = true,
            arbainNumber = 12,
            explanation = "Zamanı, dili ve kalbi malayaniden (dünya ve ahirete faydası olmayan boş meşguliyetlerden) korumak, yüksek edep ve olgun imanın en parlak alametidir.",
            authenticity = "Hasen"
        ),
        Hadith(
            id = "5",
            category = "İman",
            arabic = "لا يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ",
            turkish = "Sizden biriniz, kendisi için istediğini Müslüman kardeşi için de istemedikçe (kâmil manada) iman etmiş olmaz.",
            source = "Buhârî, Îmân, 7; Müslim, Îmân, 71",
            narrator = "Hz. Enes bin Mâlik (r.a.)",
            isArbain = true,
            arbainNumber = 13,
            explanation = "İslam kardeşliğinin altın ölçüsüdür. Kalpteki haset, bencillik ve kıskançlığı temizleyerek toplumsal huzur ve diğergamlığı tesis eder.",
            authenticity = "Muttefekun Aleyh"
        ),
        Hadith(
            id = "6",
            category = "Ahlak",
            arabic = "مَنْ كَانَ يُؤْمِنُ بِاللَّهِ وَالْيَوْمِ الآخِرِ فَلْيَقُلْ خَيْرًا أَوْ لِيَصْمُتْ",
            turkish = "Allah'a ve ahiret gününe iman eden kimse ya hayır söylesin ya da sussun! Allah'a ve ahiret gününe iman eden komşusuna ikramda bulunsun.",
            source = "Buhârî, Edeb, 31; Müslim, Îmân, 74",
            narrator = "Hz. Ebû Hüreyre (r.a.)",
            isArbain = true,
            arbainNumber = 15,
            explanation = "Söz gümüşse sükût altındır düsturunun nebevî kaynağıdır. Dilin afetlerinden korunmak ve komşuluk hukukunu gözetmek doğrudan iman olgunluğuna bağlanmıştır.",
            authenticity = "Muttefekun Aleyh"
        ),
        Hadith(
            id = "7",
            category = "Ahlak",
            arabic = "لا تَغْضَبْ فَرَدَّدَ مِرَارًا قَالَ: لا تَغْضَبْ",
            turkish = "Bir sahabî Peygamberimize 'Bana öğüt ver' dedi. Resûlullah (s.a.v.): 'Öfkelenme!' buyurdu. Adam isteğini birkaç defa tekrarladı, her defasında 'Öfkelenme!' buyurdu.",
            source = "Buhârî, Edeb, 76",
            narrator = "Hz. Ebû Hüreyre (r.a.)",
            isArbain = true,
            arbainNumber = 16,
            explanation = "Öfke anında nefsine hâkim olmak gerçek pehlivanlıktır. Kırıcı sözlerin, aile içi huzursuzlukların ve pişmanlıkların önüne geçen en veciz nebevî reçetedir.",
            authenticity = "Sahih"
        ),
        Hadith(
            id = "8",
            category = "Ahlak",
            arabic = "اتَّقِ اللَّهَ حَيْثُمَا كُنْتَ، وَأَتْبِعِ السَّيِّئَةَ الْحَسَنَةَ تَمْحُهَا، وَخَالِقِ النَّاسَ بِخُلُقٍ حَسَنٍ",
            turkish = "Nerede olursan ol Allah'a karşı gelmekten sakın (takvalı ol); yaptığın bir kötülüğün hemen arkasından bir iyilik yap ki onu silip süpürsün ve insanlara güzel ahlak ile muamele et.",
            source = "Tirmizî, Birr, 55",
            narrator = "Hz. Ebû Zer el-Gıfârî (r.a.)",
            isArbain = true,
            arbainNumber = 18,
            explanation = "Hem Allah ile kul arasındaki hukuku (takva ve tövbe) hem de kullarla olan münasebeti (güzel ahlak) üç cümlede birleştiren muazzam bir hayat rehberidir.",
            authenticity = "Hasen-Sahih"
        ),
        Hadith(
            id = "9",
            category = "İbadet",
            arabic = "الطُّهُورُ شَطْرُ الإِيمَانِ، وَالْحَمْدُ لِلَّهِ تَمْلأُ الْمِيزَانَ",
            turkish = "Temizlik imanın yarısıdır. 'Elhamdülillah' mizanı doldurur. 'Sübhanallah ve Elhamdülillah' göklerle yer arasını doldurur. Namaz nurdur, sadaka burhandır, sabır ziyadır.",
            source = "Müslim, Tahâret, 1",
            narrator = "Hz. Ebû Mâlik el-Eş'arî (r.a.)",
            isArbain = true,
            arbainNumber = 23,
            explanation = "Maddi ve manevi arınmanın, zikrin, namazın, infakın ve sabrın müminin ahiret terazisindeki nurani karşılığını tasvir eder.",
            authenticity = "Sahih"
        ),
        Hadith(
            id = "10",
            category = "Muamelat",
            arabic = "لا ضَرَرَ وَلا ضِرَارَ",
            turkish = "İslam'da zarar vermek de yoktur, zarara zararla karşılık vermek de yoktur.",
            source = "İbn Mâce, Ahkâm, 17; Muvatta', Akdıye, 31",
            narrator = "Hz. Ebû Saîd el-Hudrî (r.a.)",
            isArbain = true,
            arbainNumber = 32,
            explanation = "Mecelle'nin de temel külli kaidelerinden biridir. Ticaretten komşuluğa, çevreden sosyal hayata kadar hiçbir alanda başkasına haksız zarar verilmesine izin vermez.",
            authenticity = "Hasen"
        ),
        Hadith(
            id = "11",
            category = "Muamelat",
            arabic = "مَنْ نَفَّسَ عَنْ مُؤْمِنٍ كُرْبَةً مِنْ كُرَبِ الدُّنْيَا نَفَّسَ اللَّهُ عَنْهُ كُرْبَةً مِنْ كُرَبِ يَوْمِ الْقِيَامَةِ",
            turkish = "Kim bir müminin dünya sıkıntılarından birini giderirse, Allah da onun kıyamet günü sıkıntılarından birini giderir. Kim darda kalana kolaylık gösterirse, Allah da ona dünya ve ahirette kolaylık gösterir.",
            source = "Müslim, Zikir, 38",
            narrator = "Hz. Ebû Hüreyre (r.a.)",
            isArbain = true,
            arbainNumber = 36,
            explanation = "Kul kardeşinin yardımında olduğu sürece Allah da kulunun yardımındadır. Başkasının derdine derman olmak, ilahi rahmeti celbeden en faziletli amellerdendir.",
            authenticity = "Sahih"
        ),
        Hadith(
            id = "12",
            category = "İman",
            arabic = "إِنَّ اللَّهَ لا يَنْظُرُ إِلَى صُوَرِكُمْ وَأَمْوَالِكُمْ، وَلَكِنْ يَنْظُرُ إِلَى قُلُوبِكُمْ وَأَعْمَالِكُمْ",
            turkish = "Şüphesiz Allah sizin suretlerinize ve mallarınıza bakmaz; fakat kalplerinize ve amellerinize bakar.",
            source = "Müslim, Birr, 34",
            narrator = "Hz. Ebû Hüreyre (r.a.)",
            isArbain = false,
            arbainNumber = 0,
            explanation = "Dış görünüş, servet ve makamlarla övünmeyi ortadan kaldırır; insanın gerçek kıymetinin temiz bir kalp ve salih amellerle ölçüldüğünü hatırlatır.",
            authenticity = "Sahih"
        ),
        Hadith(
            id = "13",
            category = "İbadet",
            arabic = "خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ",
            turkish = "Sizin en hayırlınız, Kur'an'ı öğrenen ve öğreteninizdir.",
            source = "Buhârî, Fedâilü'l-Kur'ân, 21",
            narrator = "Hz. Osman (r.a.)",
            isArbain = false,
            arbainNumber = 0,
            explanation = "Kur'an-ı Kerim'i okumak, anlamak, yaşamak ve nesillere aktarmak ümmetin en şerefli meşguliyeti olarak müjdelenmiştir.",
            authenticity = "Sahih"
        ),
        Hadith(
            id = "14",
            category = "İbadet",
            arabic = "الصَّلَاةُ عِمَادُ الدِّينِ",
            turkish = "Her işin başı İslam, onun direği namaz, zirvesi ise Allah yolunda gayret göstermektir.",
            source = "Tirmizî, Îmân, 8",
            narrator = "Hz. Muâz bin Cebel (r.a.)",
            isArbain = true,
            arbainNumber = 29,
            explanation = "Çadırın ayakta durması nasıl orta direğine bağlıysa, müminin manevi hayatının diri kalması da günde beş vakit namazla Rabbine yönelmesine bağlıdır.",
            authenticity = "Hasen-Sahih"
        ),
        Hadith(
            id = "15",
            category = "Ahlak",
            arabic = "تَبَسُّمُكَ فِي وَجْهِ أَخِيكَ لَكَ صَدَقَةٌ",
            turkish = "Mümin kardeşinin yüzüne tebessüm etmen senin için bir sadakadır.",
            source = "Tirmizî, Birr, 36",
            narrator = "Hz. Ebû Zer el-Gıfârî (r.a.)",
            isArbain = false,
            arbainNumber = 0,
            explanation = "İyilik ve sadaka yalnızca maddi imkanla sınırlı değildir; güler yüz, tatlı söz ve kalp ısıtan bir selam da sevap kazandıran birer sadakadır.",
            authenticity = "Hasen"
        ),
        Hadith(
            id = "16",
            category = "Muamelat",
            arabic = "التَّاجِرُ الصَّدُوقُ الأَمِينُ مَعَ النَّبِيِّينَ وَالصِّدِّيقِينَ وَالشُّهَدَاءِ",
            turkish = "Doğru sözlü ve güvenilir tüccar, (ahirette) peygamberler, sıddıklar ve şehitlerle beraberdir.",
            source = "Tirmizî, Büyû', 4",
            narrator = "Hz. Ebû Saîd el-Hudrî (r.a.)",
            isArbain = false,
            arbainNumber = 0,
            explanation = "Helal rızık peşinde koşarken ölçü ve tartıda dürüst kalan, kimseyi aldatmayan iş insanının kazanacağı yüce manevi mertebeyi müjdeler.",
            authenticity = "Hasen"
        ),
        Hadith(
            id = "17",
            category = "Muamelat",
            arabic = "أَعْطُوا الأَجِيرَ أَجْرَهُ قَبْلَ أَنْ يَجِفَّ عَرَقُهُ",
            turkish = "İşçiye ücretini, alnının teri kurumadan önce veriniz.",
            source = "İbn Mâce, Ruhûn, 4",
            narrator = "Hz. Abdullah ibn Ömer (r.a.)",
            isArbain = false,
            arbainNumber = 0,
            explanation = "Emek ve alın terinin kutsallığını vurgular; çalışanların hakkının geciktirilmeden ve eksiksiz ödenmesini emreder.",
            authenticity = "Sahih"
        ),
        Hadith(
            id = "18",
            category = "Ahlak",
            arabic = "إِنَّمَا بُعِثْتُ لأُتَمِّمَ مَكَارِمَ الأَخْلاقِ",
            turkish = "Ben ancak güzel ahlakı tamamlamak için gönderildim.",
            source = "Muvatta', Hüsnü'l-Huluk, 8; Ahmed bin Hanbel, II, 381",
            narrator = "Hz. Ebû Hüreyre (r.a.)",
            isArbain = false,
            arbainNumber = 0,
            explanation = "Peygamber Efendimizin (s.a.v.) risalet gayesinin özeti güzel ahlaktır. İbadetlerin gayesi de insanı üstün ahlâkî erdemlere ulaştırmaktır.",
            authenticity = "Sahih"
        ),
        Hadith(
            id = "19",
            category = "İlim & Hikmet",
            arabic = "مَنْ سَلَكَ طَرِيقًا يَلْتَمِسُ فِيهِ عِلْمًا سَهَّلَ اللَّهُ لَهُ بِهِ طَرِيقًا إِلَى الْجَنَّةِ",
            turkish = "Kim ilim öğrenmek için bir yola girerse, Allah ona cennete giden yolu kolaylaştırır. Melekler, yaptığından hoşnut oldukları için ilim talibine kanatlarını gererler.",
            source = "Müslim, Zikir, 38; Ebû Dâvûd, İlim, 1",
            narrator = "Hz. Ebû Hüreyre (r.a.)",
            isArbain = true,
            arbainNumber = 36,
            explanation = "Faydalı ilim öğrenmenin, okumanın ve hakikati araştırmanın Allah katında ne denli faziletli bir ibadet olduğunu beyan eder.",
            authenticity = "Sahih"
        ),
        Hadith(
            id = "20",
            category = "İlim & Hikmet",
            arabic = "الْكَلِمَةُ الْحِكْمَةُ ضَالَّةُ الْمُؤْمِنِ فَحَيْثُ وَجَدَهَا فَهُوَ أَحَقُّ بِهَا",
            turkish = "Hikmetli söz müminin yitiğidir; onu nerede bulursa almaya en layık olan odur.",
            source = "Tirmizî, İlim, 19; İbn Mâce, Zühd, 15",
            narrator = "Hz. Ebû Hüreyre (r.a.)",
            isArbain = false,
            arbainNumber = 0,
            explanation = "Mümini sürekli öğrenmeye, doğru bilgiye ve hikmete açık bir tefekkür ufkuna davet eder.",
            authenticity = "Hasen"
        ),
        Hadith(
            id = "21",
            category = "İlim & Hikmet",
            arabic = "إِذَا مَاتَ الإِنْسَانُ انْقَطَعَ عَنْهُ عَمَلُهُ إِلا مِنْ ثَلاثَةٍ: إِلا مِنْ صَدَقَةٍ جَارِيَةٍ، أَوْ عِلْمٍ يُنْتَفَعُ بِهِ، أَوْ وَلَدٍ صَالِحٍ يَدْعُو لَهُ",
            turkish = "İnsan öldüğü zaman amel defteri kapanır; ancak üç şey müstesna: Sadaka-i câriye (faydası kesintisiz süren hayır), kendisinden faydalanılan ilim ve kendisine dua eden hayırlı evlat.",
            source = "Müslim, Vasıyyet, 14",
            narrator = "Hz. Ebû Hüreyre (r.a.)",
            isArbain = false,
            arbainNumber = 0,
            explanation = "Ömür sermayesini vefattan sonra da sevap kazandıracak kalıcı eserlere, faydalı bilgiye ve ahlaklı nesillere dönüştürmeyi öğütler.",
            authenticity = "Sahih"
        ),
        Hadith(
            id = "22",
            category = "Aile & Merhamet",
            arabic = "خَيْرُكُمْ خَيْرُكُمْ لأَهْلِهِ وَأَنَا خَيْرُكُمْ لأَهْلِي",
            turkish = "Sizin en hayırlınız, ailesine karşı en hayırlı (en şefkatli ve güzel davranan) olanınızdır. Ben de ailesine karşı en hayırlı olanınızım.",
            source = "Tirmizî, Menâkıb, 63; İbn Mâce, Nikâh, 50",
            narrator = "Hz. Âişe (r.anhâ)",
            isArbain = false,
            arbainNumber = 0,
            explanation = "Gerçek güzel ahlakın ilk sınav yeri ev içi ve aile hayatıdır. Eş ve çocuklara nezaket, merhamet ve adaletle yaklaşmak sünnetin özüdür.",
            authenticity = "Sahih"
        ),
        Hadith(
            id = "23",
            category = "Aile & Merhamet",
            arabic = "الرَّاحِمُونَ يَرْحَمُهُمُ الرَّحْمَنُ، ارْحَمُوا مَنْ فِي الأَرْضِ يَرْحَمْكُمْ مَنْ فِي السَّمَاءِ",
            turkish = "Merhamet edenlere Rahmân olan Allah da merhamet eder. Siz yeryüzündekilere merhamet edin ki, semâ ehli (Allah ve melekleri) de size merhamet etsin.",
            source = "Ebû Dâvûd, Edeb, 58; Tirmizî, Birr, 16",
            narrator = "Hz. Abdullah bin Amr (r.a.)",
            isArbain = false,
            arbainNumber = 0,
            explanation = "İnsanlara, çocuklara, yaşlılara ve tüm canlılara şefkatle muamele etmenin ilahi rahmete kavuşmanın anahtarı olduğunu bildirir.",
            authenticity = "Sahih"
        ),
        Hadith(
            id = "24",
            category = "Aile & Merhamet",
            arabic = "رِضَا الرَّبِّ فِي رِضَا الْوَالِدِ، وَسَخَطُ الرَّبِّ فِي سَخَطِ الْوَالِدِ",
            turkish = "Allah'ın rızası anne ve babanın rızasındadır; Allah'ın hoşnutsuzluğu da anne ve babanın hoşnutsuzluğundadır.",
            source = "Tirmizî, Birr, 3",
            narrator = "Hz. Abdullah bin Amr (r.a.)",
            isArbain = false,
            arbainNumber = 0,
            explanation = "Anne ve babaya hürmet, vefa ve hayır dualarını almak, dünya ve ahiret saadetinin en büyük vesilelerindendir.",
            authenticity = "Sahih"
        ),
        Hadith(
            id = "25",
            category = "Sabır & Tevekkül",
            arabic = "عَجَبًا لأَمْرِ الْمُؤْمِنِ إِنَّ أَمْرَهُ كُلَّهُ خَيْرٌ... إِنْ أَصَابَتْهُ سَرَّاءُ شَكَرَ فَكَانَ خَيْرًا لَهُ، وَإِنْ أَصَابَتْهُ ضَرَّاءُ صَبَرَ فَكَانَ خَيْرًا لَهُ",
            turkish = "Müminin durumu ne kadar hayranlık vericidir! Onun her işi kendisi için hayırdır. Bu durum yalnız mümine mahsustur: Ona sevindirici bir şey gelirse şükreder, bu onun için hayır olur; başına bir sıkıntı gelirse sabreder, bu da onun için hayır olur.",
            source = "Müslim, Zühd, 64",
            narrator = "Hz. Suheyb er-Rûmî (r.a.)",
            isArbain = false,
            arbainNumber = 0,
            explanation = "Sabır ve şükür kanatlarıyla uçan müminin hayatında mutlak kayıp yoktur; her imtihan onu Rabbine yaklaştıran bir basamağa dönüşür.",
            authenticity = "Sahih"
        ),
        Hadith(
            id = "26",
            category = "Sabır & Tevekkül",
            arabic = "احْفَظِ اللَّهَ يَحْفَظْكَ، احْفَظِ اللَّهَ تَجِدْهُ تُجَاهَكَ... وَاعْلَمْ أَنَّ النَّصْرَ مَعَ الصَّبْرِ، وَأَنَّ الْفَرَجَ مَعَ الْكَرْبِ، وَأَنَّ مَعَ الْعُسْرِ يُسْرًا",
            turkish = "Allah'ın sınırlarını koru ki Allah da seni korusun. Allah'ı gözet ki O'nu daima yanında bulasın. Bil ki zafer sabırla beraberdir, kurtuluş sıkıntıyla birliktedir ve her zorlukla beraber muhakkak bir kolaylık vardır.",
            source = "Tirmizî, Kıyâmet, 59; Ahmed bin Hanbel, I, 293",
            narrator = "Hz. Abdullah ibn Abbâs (r.a.)",
            isArbain = true,
            arbainNumber = 19,
            explanation = "Tevhid ve tevekkül bilincini aşılayan eşsiz bir nebevî vasiyettir. Zorluklar karşısında ümitsizliğe kapılmadan sabırla direnmeyi öğretir.",
            authenticity = "Sahih"
        ),
        Hadith(
            id = "27",
            category = "Sabır & Tevekkül",
            arabic = "لَوْ أَنَّكُمْ تَتَوَكَّلُونَ عَلَى اللَّهِ حَقَّ تَوَكُّلِهِ لَرَزَقَكُمْ كَمَا يَرْزَقُ الطَّيْرَ، تَغْدُو خِمَاصًا وَتَرُوحُ بِطَانًا",
            turkish = "Eğer siz Allah'a hakkıyla tevekkül etseydiniz, sabahleyin kursakları boş çıkıp akşamleyin tok dönen kuşları rızıklandırdığı gibi sizi de rızıklandırırdı.",
            source = "Tirmizî, Zühd, 33; İbn Mâce, Zühd, 14",
            narrator = "Hz. Ömer (r.a.)",
            isArbain = false,
            arbainNumber = 0,
            explanation = "Gerçek tevekkül tembellik değil; kuşların sabah erkenden kanat çırpıp rızık aramaya çıkması gibi sebeplere sarıldıktan sonra neticeyi Allah'tan beklemektir.",
            authenticity = "Hasen-Sahih"
        ),
        Hadith(
            id = "28",
            category = "Dua & Zikir",
            arabic = "الدُّعَاءُ هُوَ الْعِبَادَةُ",
            turkish = "Dua, ibadetin ta kendisidir (özüdür).",
            source = "Tirmizî, Daavât, 1; Ebû Dâvûd, Vitir, 23",
            narrator = "Hz. Nu'mân bin Beşîr (r.a.)",
            isArbain = false,
            arbainNumber = 0,
            explanation = "Kulun acziyetini bilip sonsuz kudret sahibi Rabbine el açması, kulluğun en saf ve en samimi tezahürüdür.",
            authenticity = "Hasen-Sahih"
        ),
        Hadith(
            id = "29",
            category = "Dua & Zikir",
            arabic = "كَلِمَتَانِ خَفِيفَتَانِ عَلَى اللِّسَانِ، ثَقِيلَتَانِ فِي الْمِيزَانِ، حَبِيبَتَانِ إِلَى الرَّحْمَنِ: سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
            turkish = "Dile hafif, mizanda ağır ve Rahmân'a çok sevimli gelen iki cümle vardır: 'Sübhânallâhi ve bi-hamdihî, Sübhânallâhi'l-Azîm' (Allah'ı hamd ile tesbih ederim, Yüce Allah her eksiklikten münezzehtir).",
            source = "Buhârî, Daavât, 65; Müslim, Zikir, 31",
            narrator = "Hz. Ebû Hüreyre (r.a.)",
            isArbain = false,
            arbainNumber = 0,
            explanation = "Buhârî'nin Sahih'ini kendisiyle mühürlediği bu muazzam tesbih, gün içinde her an kolayca okunabilecek büyük bir manevi hazinedir.",
            authenticity = "Muttefekun Aleyh"
        ),
        Hadith(
            id = "30",
            category = "Dua & Zikir",
            arabic = "مَثَلُ الَّذِي يَذْكُرُ رَبَّهُ وَالَّذِي لا يَذْكُرُ رَبَّهُ مَثَلُ الْحَيِّ وَالْمَيِّتِ",
            turkish = "Rabbini zikreden kimse ile Rabbini zikretmeyen kimsenin misali, diri ile ölü gibidir.",
            source = "Buhârî, Daavât, 66",
            narrator = "Hz. Ebû Mûsâ el-Eş'arî (r.a.)",
            isArbain = false,
            arbainNumber = 0,
            explanation = "Kalbin gerçek hayatı Allah'ı anmakla (zikirle) mümkündür. Zikirden gafil kalan kalp, manevi diriliğini ve huzurunu kaybeder.",
            authenticity = "Sahih"
        ),
        Hadith(
            id = "31",
            category = "İman",
            arabic = "إِنَّ اللَّهَ تَجَاوَزَ لِي عَنْ أُمَّتِي الْخَطَأَ وَالنِّسْيَانَ وَمَا اسْتُكْرِهُوا عَلَيْهِ",
            turkish = "Şüphesiz Allah, ümmetimin yanılma (hata), unutma ve zorlama altında yaptıkları şeylerin günahını bağışlamıştır.",
            source = "İbn Mâce, Talâk, 16; Beyhakî, es-Sünenü'l-Kübrâ, VII, 356",
            narrator = "Hz. Abdullah ibn Abbâs (r.a.)",
            isArbain = true,
            arbainNumber = 39,
            explanation = "İslam dininin kolaylık ve rahmet dini olduğunun en açık delillerindendir; kasıtsız hata ve unutmalar ilahi af kapsamındadır.",
            authenticity = "Hasen"
        ),
        Hadith(
            id = "32",
            category = "Ahlak",
            arabic = "كُنْ فِي الدُّنْيَا كَأَنَّكَ غَرِيبٌ أَوْ عَابِرُ سَبِيلٍ",
            turkish = "Dünyada bir garip (gurbetteki kimse) veya bir yolcu gibi ol!",
            source = "Buhârî, Rikâk, 3",
            narrator = "Hz. Abdullah ibn Ömer (r.a.)",
            isArbain = true,
            arbainNumber = 40,
            explanation = "Dünya hayatının geçiciliğini unutmadan, kalbi dünyevî hırslara esir etmeden ebedî yurt olan ahiret için hazırlık yapmayı öğütler.",
            authenticity = "Sahih"
        ),
        Hadith(
            id = "33",
            category = "Muamelat",
            arabic = "إِنَّ الْحَلالَ بَيِّنٌ وَإِنَّ الْحَرَامَ بَيِّنٌ وَبَيْنَهُمَا مُشْتَبِهَاتٌ... فَمَنِ اتَّقَى الشُّبُهَاتِ اسْتَبْرَأَ لِدِينِهِ وَعِرْضِهِ",
            turkish = "Şüphesiz helal belli, haram da bellidir. İkisinin arasında ise şüpheli şeyler vardır. Kim şüpheli şeylerden sakınırsa dinini ve onurunu korumuş olur. Dikkat edin! Vücutta öyle bir et parçası vardır ki o iyi olursa bütün beden iyi olur; o bozulursa bütün beden bozulur. Dikkat edin, o kalptir!",
            source = "Buhârî, Îmân, 39; Müslim, Müsâkât, 107",
            narrator = "Hz. Nu'mân bin Beşîr (r.a.)",
            isArbain = true,
            arbainNumber = 6,
            explanation = "Helal lokma hassasiyetini, şüpheli kazançlardan uzak durmayı (takva ve verâ) ve kalp temizliğini bir arada sunan temel hadislerden biridir.",
            authenticity = "Muttefekun Aleyh"
        ),
        Hadith(
            id = "34",
            category = "İbadet",
            arabic = "أَحَبُّ الأَعْمَالِ إِلَى اللَّهِ أَدْوَمُهَا وَإِنْ قَلَّ",
            turkish = "Allah'a en sevimli gelen amel, az da olsa devamlı olanıdır.",
            source = "Buhârî, Rikâk, 18; Müslim, Müsâfirîn, 218",
            narrator = "Hz. Âişe (r.anhâ)",
            isArbain = false,
            arbainNumber = 0,
            explanation = "İbadette ve hayırda geçici coşkulardan ziyade istikrarın, disiplinin ve sürekliliğin esas olduğunu öğretir.",
            authenticity = "Muttefekun Aleyh"
        )
    )
}

object HadithRepository {
    val categories = HadithData.categories
    val hadiths = HadithData.allHadiths
}
