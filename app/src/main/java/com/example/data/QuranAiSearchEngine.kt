package com.example.data

import android.content.Context
import com.example.ui.QuranData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class QuranAiSearchResult(
    val surahId: Int,
    val surahName: String,
    val verseNumber: String,
    val verseNumberInt: Int,
    val arabic: String,
    val translation: String,
    val relevanceSummary: String,
    val aiTafsirInsight: String,
    val keyConcepts: List<String>,
    val relevancePercentage: Int = 95,
    val themeCategory: String = "Kur'an Rehberliği"
)

data class QuranTopicIndex(
    val id: String,
    val title: String,
    val icon: String,
    val category: String,
    val description: String,
    val verses: List<QuranIndexVerseItem>
)

data class QuranIndexVerseItem(
    val surahId: Int,
    val surahName: String,
    val verseNumber: Int,
    val arabic: String,
    val translation: String,
    val explanation: String,
    val tags: List<String>
)

object QuranAiSearchEngine {

    fun normalizeText(text: String): String {
        return text.lowercase(Locale("tr", "TR"))
            .replace("â", "a")
            .replace("î", "i")
            .replace("û", "u")
            .replace("ı", "i")
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ö", "o")
            .replace("ç", "c")
            .replace("İ", "i")
            .replace("Ğ", "g")
            .replace("Ü", "u")
            .replace("Ş", "s")
            .replace("Ö", "o")
            .replace("Ç", "c")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .trim()
    }

    fun findSurahId(nameOrNumber: String): Int {
        val clean = normalizeText(nameOrNumber)
            .replace("suresi", "")
            .replace("sure", "")
            .replace("ayeti", "")
            .replace("ayet", "")
            .trim()

        val asNum = clean.toIntOrNull()
        if (asNum != null && asNum in 1..114) {
            return asNum
        }

        if (clean.isEmpty()) return 1

        val exact = QuranData.surahMetadata.find { meta ->
            normalizeText(meta.name) == clean
        }
        if (exact != null) return exact.id

        val contains = QuranData.surahMetadata.find { meta ->
            val metaNameNorm = normalizeText(meta.name)
            metaNameNorm.contains(clean) || clean.contains(metaNameNorm)
        }
        return contains?.id ?: 1
    }

    suspend fun searchQuranWithAi(
        query: String,
        context: Context,
        filterCategory: String = "Tümü",
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): List<QuranAiSearchResult> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return@withContext emptyList()

        onProgress(0.12f, "Soru ve niyet semantik olarak çözümleniyor...")
        delay(120)

        onProgress(0.28f, "Kur'an-ı Kerim'in 114 suresi taranıyor...")
        delay(150)

        val apiKey = GeminiApiClient.getApiKey(context)
        if (GeminiApiClient.isApiKeyValid(apiKey)) {
            try {
                onProgress(0.50f, "Yapay Zeka (Gemini 3.5 Flash) ayetleri ve tefsir bağlamını inceliyor...")
                
                val filterClause = if (filterCategory != "Tümü") {
                    "Özellikle '$filterCategory' kategorisi ve bağlamıyla doğrudan ilgili ayetlere öncelik ver."
                } else {
                    "Tüm Kur'an-ı Kerim'den (inanç, ibadet, ahlak, sosyal hayat, kıssalar, psikolojik haller, dualar) en doğrudan ve etkileyici ayetleri seç."
                }

                val systemPrompt = """
                    Sen Kur'an-ı Kerim, tefsir, nüzul sebepleri ve İslami ilimler konusunda yetkin bir Yapay Zeka Kur'an Araştırma Motorusun.
                    
                    GÖREVİN:
                    Kullanıcının girdiği soru, konu, duygu durumu, hayat problemi veya arama terimini ('$trimmedQuery') Kur'an-ı Kerim'in 114 suresi ve 6236 ayeti genelinde derinlemesine analiz etmek.
                    $filterClause
                    
                    Bu konuya KESİNLİKLE en doğrudan cevap veren, kalbe şifa ve akla rehberlik sunan EN UYGUN 4 ila 7 AYETİ seç ve JSON olarak getir.
                    
                    KURALLAR:
                    1. surahId: 1 ile 114 arasında doğru sure numarası olmalıdır (örn: Fatiha için 1, Bakara için 2, İnşirah için 94).
                    2. surahName: Surenin bilinen tam Türkçe adı (örn: "Bakara Suresi", "İnşirâh Suresi").
                    3. verseNumber: Ayet numarası veya aralığı (örn: "153", "5-6", "286").
                    4. verseNumberInt: Ayet başlangıç tam sayısı (örn: 153).
                    5. arabic: Ayetin doğru, tam ve harekeli Arapça metni (Uthmani hat).
                    6. translation: Akıcı, güvenilir ve anlaşılır Türkçe meali (Diyanet / Elmalılı mealleri kalitesinde).
                    7. relevanceSummary: Bu ayetin kullanıcının sorusu veya aradığı konuyla doğrudan bağını ve taşıdığı ana mesajı açıklayan 1-2 cümlelik net özet.
                    8. aiTafsirInsight: Ayetin tefsiri, nüzul hikmeti, kelime incelikleri ve günümüz insanının hayatına/kalbine sunduğu somut rehberliği açıklayan derinlikli tefsir analizi.
                    9. keyConcepts: Ayetten çıkarılan 3-5 adet anahtar kavram veya etiket (örn: ["#Sabır", "#Namaz", "#İlahiYardım"]).
                    10. relevancePercentage: Ayetin aramayla alaka yüzdesi (örn: 98, 95, 92).
                    11. themeCategory: Konunun kategorisi (örn: "Kalp & Maneviyat", "İbadet & Dua", "Ahlak & Toplum", "Adalet & Hukuk", "İlim & Kâinat").

                    DÖNDÜRÜLECEK FORMAT:
                    SADECE geçerli bir JSON array formatında döndür. Hiçbir selamlama, markdown açıklaması veya fazladan metin ekleme.
                    Örnek:
                    [
                      {
                        "surahId": 2,
                        "surahName": "Bakara Suresi",
                        "verseNumber": "153",
                        "verseNumberInt": 153,
                        "arabic": "يَا أَيُّهَا الَّذِينَ آمَنُوا اسْتَعِينُوا بِالصَّبْرِ وَالصَّلَاةِ ۚ إِنَّ اللَّهَ مَعَ الصَّابِرِينَ",
                        "translation": "Ey iman edenler! Sabır ve namazla Allah'tan yardım dileyin. Şüphesiz Allah sabredenlerle beraberdir.",
                        "relevanceSummary": "Zorluk ve darlık anında yalnız olmadığınızı, namaz ve sabır ile ilahi desteğin geleceğini beyan eder.",
                        "aiTafsirInsight": "Ayet, müminin hayatın fırtınaları karşısında iki sarsılmaz sığınağı olduğunu bildirir: İç dünyayı eğiten 'sabır' ve dikey irtibatı kuran 'namaz'. Allah'ın maiyyeti (beraberliği) sadece seyirci kalmak değil, lütuf ve yardım ile kuşatmaktır.",
                        "keyConcepts": ["Sabır", "Namaz", "İlahi Yardım", "Metanet"],
                        "relevancePercentage": 99,
                        "themeCategory": "Kalp & Maneviyat"
                      }
                    ]
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = "Kullanıcı Arama Talebi: $trimmedQuery")))),
                    systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                )

                onProgress(0.72f, "Ayet mealleri ve tefsir ilişkilendirmeleri sentezleniyor...")
                val response = GeminiApiClient.service.generateContent(apiKey, request)

                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val cleanJson = text
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()

                if (cleanJson.startsWith("[")) {
                    val jsonArray = JSONArray(cleanJson)
                    val list = mutableListOf<QuranAiSearchResult>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val sName = obj.optString("surahName", "Sure")
                        val explicitSurahId = obj.optInt("surahId", 0)
                        val finalSurahId = if (explicitSurahId in 1..114) explicitSurahId else findSurahId(sName)
                        val vNumStr = obj.optString("verseNumber", "1")
                        val vNumInt = obj.optInt("verseNumberInt", vNumStr.filter { it.isDigit() }.toIntOrNull() ?: 1)

                        val concepts = mutableListOf<String>()
                        if (obj.has("keyConcepts")) {
                            val arr = obj.getJSONArray("keyConcepts")
                            for (j in 0 until arr.length()) {
                                concepts.add(arr.getString(j).removePrefix("#"))
                            }
                        }

                        list.add(
                            QuranAiSearchResult(
                                surahId = finalSurahId,
                                surahName = sName,
                                verseNumber = vNumStr,
                                verseNumberInt = vNumInt,
                                arabic = obj.optString("arabic", ""),
                                translation = obj.optString("translation", ""),
                                relevanceSummary = obj.optString("relevanceSummary", ""),
                                aiTafsirInsight = obj.optString("aiTafsirInsight", ""),
                                keyConcepts = concepts,
                                relevancePercentage = obj.optInt("relevancePercentage", 95),
                                themeCategory = obj.optString("themeCategory", "Kur'an Rehberliği")
                            )
                        )
                    }

                    if (list.isNotEmpty()) {
                        onProgress(1.0f, "Yapay Zeka analizi tamamlandı.")
                        return@withContext list
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Offline / Fallback Intelligent Semantic Search Engine
        onProgress(0.70f, "Çevrimdışı Kur'an Fihristi ve semantik veritabanı taranıyor...")
        delay(100)

        val results = searchOfflineCuratedIndex(trimmedQuery, filterCategory)
        onProgress(1.0f, "Sonuçlar hazırlandı.")
        return@withContext results
    }

    private fun searchOfflineCuratedIndex(query: String, filterCategory: String): List<QuranAiSearchResult> {
        val queryNorm = normalizeText(query)
        val tokens = queryNorm.split(" ").filter { it.length >= 2 }

        val scoredResults = mutableListOf<Pair<QuranAiSearchResult, Int>>()

        allCuratedTopics.forEach { topic ->
            if (filterCategory != "Tümü" && topic.category != filterCategory) {
                return@forEach
            }

            val topicTitleNorm = normalizeText(topic.title)
            val topicCatNorm = normalizeText(topic.category)
            val topicDescNorm = normalizeText(topic.description)

            topic.verses.forEach { v ->
                val trNorm = normalizeText(v.translation)
                val expNorm = normalizeText(v.explanation)
                val surahNorm = normalizeText(v.surahName)

                var score = 0
                tokens.forEach { t ->
                    if (topicTitleNorm.contains(t)) score += 25
                    if (trNorm.contains(t)) score += 15
                    if (expNorm.contains(t)) score += 12
                    if (topicCatNorm.contains(t)) score += 10
                    if (surahNorm.contains(t)) score += 20
                    if (v.tags.any { normalizeText(it).contains(t) }) score += 18
                }

                if (queryNorm.isNotEmpty() && trNorm.contains(queryNorm)) score += 40
                if (queryNorm.isNotEmpty() && topicTitleNorm.contains(queryNorm)) score += 50

                if (score > 0 || tokens.isEmpty()) {
                    val calcPercentage = (80 + (score.coerceAtMost(20))).coerceIn(82, 99)
                    scoredResults.add(
                        Pair(
                            QuranAiSearchResult(
                                surahId = v.surahId,
                                surahName = v.surahName,
                                verseNumber = "${v.verseNumber}",
                                verseNumberInt = v.verseNumber,
                                arabic = v.arabic,
                                translation = v.translation,
                                relevanceSummary = v.explanation,
                                aiTafsirInsight = "${v.surahName} ${v.verseNumber}. ayeti, '$query' aramanızın işaret ettiği '${topic.title}' hikmetiyle doğrudan bağdaşır. Bu ayet-i kerime kalbe metanet ve hidayet kapılarını aralar.",
                                keyConcepts = v.tags,
                                relevancePercentage = calcPercentage,
                                themeCategory = topic.category
                            ),
                            score
                        )
                    )
                }
            }
        }

        val sorted = scoredResults.distinctBy { "${it.first.surahId}:${it.first.verseNumberInt}" }
            .sortedByDescending { it.second }
            .map { it.first }

        if (sorted.isNotEmpty()) {
            return sorted.take(8)
        }

        // Fallback default suggestions
        return allCuratedTopics.take(4).flatMap { topic ->
            topic.verses.take(1).map { v ->
                QuranAiSearchResult(
                    surahId = v.surahId,
                    surahName = v.surahName,
                    verseNumber = "${v.verseNumber}",
                    verseNumberInt = v.verseNumber,
                    arabic = v.arabic,
                    translation = v.translation,
                    relevanceSummary = v.explanation,
                    aiTafsirInsight = "Kur'an-ı Kerim'in hidayet rehberliğinde derlenmiş hikmetli ayet-i kerime.",
                    keyConcepts = v.tags,
                    relevancePercentage = 90,
                    themeCategory = topic.category
                )
            }
        }
    }

    val allCuratedTopics: List<QuranTopicIndex> by lazy {
        listOf(
            QuranTopicIndex(
                id = "sabr_zorluk",
                title = "Sabır, Metanet & Zorluklar",
                icon = "🌱",
                category = "Kalp & Maneviyat",
                description = "Zorluklar, imtihanlar, darlık ve sabrın ilahi mükafatı",
                verses = listOf(
                    QuranIndexVerseItem(
                        surahId = 2,
                        surahName = "Bakara Suresi",
                        verseNumber = 153,
                        arabic = "يَا أَيُّهَا الَّذِينَ آمَنُوا اسْتَعِينُوا بِالصَّبْرِ وَالصَّلَاةِ ۚ إِنَّ اللَّهَ مَعَ الصَّابِرِينَ",
                        translation = "Ey iman edenler! Sabır ve namazla yardım dileyin. Şüphesiz Allah sabredenlerle beraberdir.",
                        explanation = "Darlık ve sıkıntı anlarında namaz ile sabrın en büyük manevi güç olduğunu hatırlatır.",
                        tags = listOf("Sabır", "Namaz", "İlahi Yardım", "Metanet")
                    ),
                    QuranIndexVerseItem(
                        surahId = 94,
                        surahName = "İnşirâh Suresi",
                        verseNumber = 5,
                        arabic = "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا * إِنَّ مَعَ الْعُسْرِ يُسْرًا",
                        translation = "Şüphesiz zorlukla beraber bir kolaylık vardır. Gerçekten, her zorlukla beraber bir kolaylık vardır.",
                        explanation = "Hiçbir zorluğun kalıcı olmadığını, her darlığın içinde bir kolaylık tohumu saklandığını müjdeler.",
                        tags = listOf("İnşirah", "Ferahlık", "Ümit", "Teselli")
                    ),
                    QuranIndexVerseItem(
                        surahId = 39,
                        surahName = "Zümer Suresi",
                        verseNumber = 10,
                        arabic = "إِنَّمَا يُوَفَّى الصَّابِرُونَ أَجْرَهُمْ بِغَيْرِ حِسَابٍ",
                        translation = "Ancak sabredenlere mükafatları hesapsız bir şekilde tastamam ödenecektir.",
                        explanation = "Sabreden müminlerin ahiretteki karşılığının ölçüsüz ve sınırsız bir lütuf olacağını bildirir.",
                        tags = listOf("Sabır", "Mükafat", "Hesapsız Ecir")
                    ),
                    QuranIndexVerseItem(
                        surahId = 2,
                        surahName = "Bakara Suresi",
                        verseNumber = 155,
                        arabic = "وَلَنَبْلُوَنَّكُمْ بِشَيْءٍ مِنَ الْخَوْفِ وَالْجُوعِ وَنَقْصٍ مِنَ الْأَمْوَالِ وَالْأَنْفُسِ وَالثَّمَرَاتِ ۗ وَبَشِّرِ الصَّابِرِينَ",
                        translation = "Andolsun ki sizi biraz korku ve açlıkla; mallardan, canlardan ve ürünlerden eksiltmekle deneriz. Sabredenleri müjdele!",
                        explanation = "Hayatın iniş çıkışlarının birer olgunlaşma sınavı olduğunu haber verir.",
                        tags = listOf("İmtihan", "Korku", "Müjde", "Sabır")
                    )
                )
            ),
            QuranTopicIndex(
                id = "dua_yakaris",
                title = "Dua, Yakarış & Şifa",
                icon = "🤲",
                category = "İbadet & Dua",
                description = "Allah'a iltica, duaların kabulü ve içsel yakarışlar",
                verses = listOf(
                    QuranIndexVerseItem(
                        surahId = 2,
                        surahName = "Bakara Suresi",
                        verseNumber = 186,
                        arabic = "وَإِذَا سَأَلَكَ عِبَادِي عَنِّي فَإِنِّي قَرِيبٌ ۖ أُجِيبُ دَعْوَةَ الدَّاعِ إِذَا دَعَانِ",
                        translation = "Kullarım sana beni sorduklarında bilsinler ki ben şüphesiz onlara çok yakınım. Bana dua ettiği zaman dua edenin dileğine karşılık veririm.",
                        explanation = "Allah'ın kuluna şah damarından yakın olduğunu ve içten edilen hiçbir duanın cevapsız kalmayacağını açıklar.",
                        tags = listOf("Dua", "Kabul", "İlahi Yakınlık", "Yakarış")
                    ),
                    QuranIndexVerseItem(
                        surahId = 40,
                        surahName = "Mü'min Suresi",
                        verseNumber = 60,
                        arabic = "وَقَالَ رَبُّكُمُ ادْعُونِي أَسْتَجِبْ لَكُمْ",
                        translation = "Rabbiniz şöyle buyurdu: 'Bana dua edin, duanıza icabet edeyim.'",
                        explanation = "Dua etmenin bizatihi ilahi bir emir ve ibadet olduğunu beyan eder.",
                        tags = listOf("Dua", "İcabet", "Kulluk")
                    ),
                    QuranIndexVerseItem(
                        surahId = 21,
                        surahName = "Enbiyâ Suresi",
                        verseNumber = 87,
                        arabic = "لَا إِلَٰهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ الظَّالِمِينَ",
                        translation = "Senden başka hiçbir ilah yoktur. Seni tenzih ederim. Gerçekten ben kendi nefsime zulmedenlerden oldum.",
                        explanation = "Hz. Yunus'un (a.s) balığın karnındaki büyük zikri ve en büyük darlık kurtuluş duasıdır.",
                        tags = listOf("Yunus Duası", "Kurtuluş", "Tevhid", "İstiğfar")
                    ),
                    QuranIndexVerseItem(
                        surahId = 17,
                        surahName = "İsrâ Suresi",
                        verseNumber = 82,
                        arabic = "وَنُنَزِّلُ مِنَ الْقُرْآنِ مَا هُوَ شِفَاءٌ وَرَحْمَةٌ لِلْمُؤْمِنِينَ",
                        translation = "Biz Kur'an'dan, müminler için şifa ve rahmet olan şeyleri indiriyoruz.",
                        explanation = "Kur'an ayetlerinin hem kalbi hem de ruhi hastalıklara birer şifa vesilesi olduğunu beyan eder.",
                        tags = listOf("Şifa", "Rahmet", "Kur'an Nuru")
                    )
                )
            ),
            QuranTopicIndex(
                id = "kalp_huzuru_zikir",
                title = "Kalp Huzuru & Manevi Teselli",
                icon = "❤️",
                category = "Kalp & Maneviyat",
                description = "Gönül rahatlığı, zikir, manevi dinginlik ve huzur",
                verses = listOf(
                    QuranIndexVerseItem(
                        surahId = 13,
                        surahName = "Ra'd Suresi",
                        verseNumber = 28,
                        arabic = "الَّذِينَ آمَنُوا وَتَطْمَئِنُّ قُلُوبُهُمْ بِذِكْرِ اللَّهِ ۗ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
                        translation = "Onlar iman edenler ve kalpleri Allah'ı anmakla huzura kavuşanlardır. İyi bilin ki kalpler ancak Allah'ı anmakla tatmin olur (huzur bulur).",
                        explanation = "İnsan kalbinin yegane gerçek huzur kaynağının Allah'ı zikretmek olduğunu açıklar.",
                        tags = listOf("Zikir", "Kalp Huzuru", "İtminan", "Sekinet")
                    ),
                    QuranIndexVerseItem(
                        surahId = 93,
                        surahName = "Duhâ Suresi",
                        verseNumber = 3,
                        arabic = "مَا وَدَّعَكَ رَبُّكَ وَمَا قَلَىٰ",
                        translation = "Rabbin seni ne terk etti ne de sana darıldı.",
                        explanation = "Yalnızlık ve hüzün anında kulun asla sahipsiz kalmadığını müjdeleyen eşsiz bir teselli hitabıdır.",
                        tags = listOf("Teselli", "Duha", "İlahi Şefkat", "Yalnızlık")
                    ),
                    QuranIndexVerseItem(
                        surahId = 65,
                        surahName = "Talâk Suresi",
                        verseNumber = 3,
                        arabic = "وَمَنْ يَتَوَكَّلْ عَلَى اللَّهِ فَهُوَ حَسْبُهُ",
                        translation = "Kim Allah'a tevekkül ederse, O kendisine yeter.",
                        explanation = "Tüm kaygıları Allah'a havale eden kula, Allah'ın kafi geleceğini bildirir.",
                        tags = listOf("Tevekkül", "Güven", "Kifayet")
                    )
                )
            ),
            QuranTopicIndex(
                id = "adalet_hak",
                title = "Adalet, Hak & Doğruluk",
                icon = "⚖️",
                category = "Ahlak & Toplum",
                description = "Hakkaniyet, adil şahitlik, emanet ve dürüstlük",
                verses = listOf(
                    QuranIndexVerseItem(
                        surahId = 4,
                        surahName = "Nisâ Suresi",
                        verseNumber = 135,
                        arabic = "يَا أَيُّهَا الَّذِينَ آمَنُوا كُونُوا قَوَّامِينَ بِالْقِسْطِ شُهَدَاءَ لِلَّهِ وَلَوْ عَلَىٰ أَنْفُسِكُمْ",
                        translation = "Ey iman edenler! Kendiniz, ana-babanız ve en yakınlarınız aleyhine de olsa, Allah için şahitlik yaparak adaleti titizlikle ayakta tutun.",
                        explanation = "Adaletin hiçbir menfaat veya akrabalık bağıyla çiğnenemeyecek mutlak bir ilke olduğunu vurgular.",
                        tags = listOf("Adalet", "Dürüstlük", "Şahitlik", "Hak")
                    ),
                    QuranIndexVerseItem(
                        surahId = 5,
                        surahName = "Mâide Suresi",
                        verseNumber = 8,
                        arabic = "وَلَا يَجْرِمَنَّكُمْ شَنَآنُ قَوْمٍ عَلَىٰ أَلَّا تَعْدِلُوا ۚ اعْدِلُوا هُوَ أَقْرَبُ لِلتَّقْوَىٰ",
                        translation = "Bir topluluğa olan öfkeniz sizi adaletsizliğe sevk etmesin. Adil olun; bu takvaya daha yakındır.",
                        explanation = "Düşmana dahi adaletle muamele etmeyi emreden evrensel Kur'an ahlakını bildirir.",
                        tags = listOf("Hakkaniyet", "Takva", "Adil Olmak")
                    ),
                    QuranIndexVerseItem(
                        surahId = 16,
                        surahName = "Nahl Suresi",
                        verseNumber = 90,
                        arabic = "إِنَّ اللَّهَ يَأْمُرُ بِالْعَدْلِ وَالْإِحْسَانِ وَإِيتَاءِ ذِي الْقُرْبَىٰ وَيَنْهَىٰ عَنِ الْفَحْشَاءِ وَالْمُنْكَرِ وَالْبَغْيِ",
                        translation = "Şüphesiz Allah adaleti, iyilik yapmayı ve akrabaya yardım etmeyi emreder; hayasızlığı, kötülüğü ve azgınlığı yasaklar.",
                        explanation = "İslam'ın toplumsal ahlak ve nizamının temel omurgasını çizen ayettir.",
                        tags = listOf("Adalet", "İhsan", "İyilik", "Haramlar")
                    )
                )
            ),
            QuranTopicIndex(
                id = "rizik_bereket",
                title = "Rızık, İnfak & Bereket",
                icon = "🌾",
                category = "Ahlak & Toplum",
                description = "Helal rızık, cömertlik, sadaka ve şükür",
                verses = listOf(
                    QuranIndexVerseItem(
                        surahId = 14,
                        surahName = "İbrâhîm Suresi",
                        verseNumber = 7,
                        arabic = "لَئِن شَكَرْتُمْ لَأَزِيدَنَّكُمْ ۖ وَلَئِن كَفَرْتُمْ إِنَّ عَذَابِي لَشَدِيدٌ",
                        translation = "Hani Rabbiniz şöyle duyurmuştu: 'Andolsun, eğer şükrederseniz elbette size nimetimi artırırım...'",
                        explanation = "Şükrün nimetleri bereketlendiren ve çoğaltan ilahi bir yasa olduğunu öğretir.",
                        tags = listOf("Şükür", "Nimet", "Bereket", "Rızık")
                    ),
                    QuranIndexVerseItem(
                        surahId = 2,
                        surahName = "Bakara Suresi",
                        verseNumber = 261,
                        arabic = "مَثَلُ الَّذِينَ يُنْفِقُونَ أَمْوَالَهُمْ فِي سَبِيلِ اللَّهِ كَمَثَلِ حَبَّةٍ أَنْبَتَتْ سَبْعَ سَنَابِلَ فِي كُلِّ سُنْبُلَةٍ مِائَةُ حَبَّةٍ",
                        translation = "Mallarını Allah yolunda harcayanların örneği, yedi başak veren ve her başağında yüz tane bulunan bir tohum gibidir.",
                        explanation = "İnfakın bire yediyüz katına kadar bereketleneceğini somut bir misalle açıklar.",
                        tags = listOf("İnfak", "Sadaka", "Cömertlik", "Bereket")
                    ),
                    QuranIndexVerseItem(
                        surahId = 11,
                        surahName = "Hûd Suresi",
                        verseNumber = 6,
                        arabic = "وَمَا مِنْ دَابَّةٍ فِي الْأَرْضِ إِلَّا عَلَى اللَّهِ رِزْقُهَا",
                        translation = "Yeryüzünde hareket eden hiçbir canlı yoktur ki rızkı Allah'a ait olmasın.",
                        explanation = "Her canlının rızkının Rezzak olan Allah tarafından teminat altına alındığını bildirir.",
                        tags = listOf("Rezzak", "Rızık Teminatı", "Tevekkül")
                    )
                )
            ),
            QuranTopicIndex(
                id = "aile_sevgi",
                title = "Aile, Anne-Baba & Muhabbet",
                icon = "🏡",
                category = "Aile & Sosyal Hayat",
                description = "Anne babaya hürmet, eşler arası sevgi ve evlat terbiyesi",
                verses = listOf(
                    QuranIndexVerseItem(
                        surahId = 17,
                        surahName = "İsrâ Suresi",
                        verseNumber = 23,
                        arabic = "وَقَضَىٰ رَبُّكَ أَلَّا تَعْبُدُوا إِلَّا إِيَّاهُ وَبِالْوَالِدَيْنِ إِحْسَانًا ۚ إِمَّا يَبْلُغَنَّ عِنْدَكَ الْكِبَرَ أَحَدُهُمَا أَوْ كِلَاهُمَا فَلَا تَقُلْ لَهُمَا أُفٍّ",
                        translation = "Rabbin, sadece kendisine kulluk etmenizi ve anne-babanıza iyi davranmanızı emretti. Onlardan biri veya her ikisi yanında yaşlanırsa onlara 'öf' bile deme!",
                        explanation = "Tevhidden hemen sonra en büyük vazifenin anne babaya hürmet ve şefkat olduğunu emreder.",
                        tags = listOf("Anne Baba", "Hürmet", "İhsan", "Şefkat")
                    ),
                    QuranIndexVerseItem(
                        surahId = 30,
                        surahName = "Rûm Suresi",
                        verseNumber = 21,
                        arabic = "وَمِنْ آيَاتِهِ أَنْ خَلَقَ لَكُمْ مِنْ أَنْفُسِكُمْ أَزْوَاجًا لِتَسْكُنُوا إِلَيْهَا وَجَعَلَ بَيْنَكُمْ مَوَدَّةً وَرَحْمَةً",
                        translation = "Kendileriyle huzur bulasınız diye size kendi türünüzden eşler yaratması ve aranıza sevgi ile merhamet koyması O'nun varlığının delillerindendir.",
                        explanation = "Ailenin temel harcının karşılıklı meveddet (sevgi) ve merhamet olduğunu açıklar.",
                        tags = listOf("Evlilik", "Sevgi", "Merhamet", "Huzur")
                    ),
                    QuranIndexVerseItem(
                        surahId = 25,
                        surahName = "Furkân Suresi",
                        verseNumber = 74,
                        arabic = "رَبَّنَا هَبْ لَنَا مِنْ أَزْوَاجِنَا وَذُرِّيَّاتِنَا قُرَّةَ أَعْيُنٍ وَاجْعَلْنَا لِلْمُتَّقِينَ إِمَامًا",
                        translation = "Rabbimiz! Bize eşlerimizden ve çocuklarımızdan göz aydınlığı olacak nesiller bağışla ve bizi takva sahiplerine önder kıl.",
                        explanation = "Hayırlı bir yuva ve göz nuru evlatlar için Kur'ani bir niyazdır.",
                        tags = listOf("Göz Aydınlığı", "Hayırlı Evlat", "Aile Duası")
                    )
                )
            ),
            QuranTopicIndex(
                id = "ilim_kainat",
                title = "İlim, Akıl & Kâinat Tefekkürü",
                icon = "🌌",
                category = "İlim & Tefekkür",
                description = "Evrenin yaratılışı, akletmek, bilim ve araştırma",
                verses = listOf(
                    QuranIndexVerseItem(
                        surahId = 3,
                        surahName = "Âl-i İmrân Suresi",
                        verseNumber = 190,
                        arabic = "إِنَّ فِي خَلْقِ السَّمَاوَاتِ وَالْأَرْضِ وَاخْتِلَافِ اللَّيْلِ وَالنَّهَارِ لَآيَاتٍ لِأُولِي الْأَلْبَابِ",
                        translation = "Şüphesiz göklerin ve yerin yaratılışında, gece ile gündüzün birbiri ardınca gelişinde derin akıl sahipleri için nice deliller vardır.",
                        explanation = "Kainattaki kozmik nizamın ve doğa kanunlarının derin bir tefekkür vesilesi olduğunu bildirir.",
                        tags = listOf("Kozmoloji", "Akıl", "Tefekkür", "Kainat")
                    ),
                    QuranIndexVerseItem(
                        surahId = 39,
                        surahName = "Zümer Suresi",
                        verseNumber = 9,
                        arabic = "قُلْ هَلْ يَسْتَوِي الَّذِينَ يَعْلَمُونَ وَالَّذِينَ لَا يَعْلَمُونَ ۗ إِنَّمَا يَتَذَكَّرُ أُولُو الْأَلْبَابِ",
                        translation = "De ki: 'Hiç bilenlerle bilmeyenler bir olur mu?' Ancak temiz akıl sahipleri ibret alır.",
                        explanation = "İlmin ve bilginin insanı hakikate eriştiren üstün değerini vurgular.",
                        tags = listOf("İlim", "Bilenler", "Akıl Sahipleri")
                    ),
                    QuranIndexVerseItem(
                        surahId = 51,
                        surahName = "Zâriyât Suresi",
                        verseNumber = 47,
                        arabic = "وَالسَّمَاءَ بَنَيْنَاهَا بِأَيْدٍ وَإِنَّا لَمُوسِعُونَ",
                        translation = "Biz göğü kudretimizle kurduk ve şüphesiz biz onu genişletmekteyiz.",
                        explanation = "Modern astrofiziğin evrenin genişlemesi (Hubble Kanunu) keşfini asırlar önce haber veren mucizevi ayettir.",
                        tags = listOf("Evrenin Genişlemesi", "Astrofizik", "Kudret")
                    )
                )
            ),
            QuranTopicIndex(
                id = "tovbe_magfiret",
                title = "Tövbe, Bağışlanma & Ümit",
                icon = "🕊️",
                category = "Kalp & Maneviyat",
                description = "Günahların affı, ilahi rahmet ve samimi tövbe",
                verses = listOf(
                    QuranIndexVerseItem(
                        surahId = 39,
                        surahName = "Zümer Suresi",
                        verseNumber = 53,
                        arabic = "قُلْ يَا عِبَادِيَ الَّذِينَ أَسْرَفُوا عَلَىٰ أَنْفُسِهِمْ لَا تَقْنَطُوا مِنْ رَحْمَةِ اللَّهِ ۚ إِنَّ اللَّهَ يَغْفِرُ الذُّنُوبَ جَمِيعًا",
                        translation = "De ki: 'Ey kendi aleyhlerine haddi aşan kullarım! Allah'ın rahmetinden ümidinizi kesmeyin. Şüphesiz Allah bütün günahları bağışlar. Çünkü O çok bağışlayıcıdır, çok merhametlidir.'",
                        explanation = "Kur'an-ı Kerim'in en büyük ümit ve affedicilik kapısını açan ayetidir.",
                        tags = listOf("Rahmet", "Mağfiret", "Ümit", "Af")
                    ),
                    QuranIndexVerseItem(
                        surahId = 66,
                        surahName = "Tahrîm Suresi",
                        verseNumber = 8,
                        arabic = "يَا أَيُّهَا الَّذِينَ آمَنُوا تُوبُوا إِلَى اللَّهِ تَوْبَةً نَصُوحًا",
                        translation = "Ey iman edenler! Allah'a samimi ve kesin bir tövbe (Tevbe-i Nasuh) ile dönün.",
                        explanation = "Pişmanlık duyarak bir daha kötülüğe dönmeme azmiyle yapılan samimi tövbeyi emreder.",
                        tags = listOf("Nasuh Tövbesi", "Samimiyet", "İhlas")
                    ),
                    QuranIndexVerseItem(
                        surahId = 3,
                        surahName = "Âl-i İmrân Suresi",
                        verseNumber = 135,
                        arabic = "وَالَّذِينَ إِذَا فَعَلُوا فَاحِشَةً أَوْ ظَلَمُوا أَنْفُسَهُمْ ذَكَرُوا اللَّهَ فَاسْتَغْفَرُوا لِذُنُوبِهِمْ",
                        translation = "Onlar çirkin bir iş yaptıkları yahut nefislerine zulmettikleri zaman Allah'ı hatırlayıp hemen günahlarının bağışlanmasını dilerler.",
                        explanation = "Takva sahiplerinin hatada ısrar etmeyip derhal istiğfara yöneldiklerini bildirir.",
                        tags = listOf("İstiğfar", "Takva", "Pişmanlık")
                    )
                )
            ),
            QuranTopicIndex(
                id = "adalet_hakkaniyet",
                title = "Adalet, Emanet & Hakkaniyet",
                icon = "⚖️",
                category = "Toplum & Hukuk",
                description = "Hakkı gözetmek, emaneti ehline vermek ve adil şahitlik",
                verses = listOf(
                    QuranIndexVerseItem(
                        surahId = 4,
                        surahName = "Nisâ Suresi",
                        verseNumber = 58,
                        arabic = "إِنَّ اللَّهَ يَأْمُرُكُمْ أَنْ تُؤَدُّوا الْأَمَانَاتِ إِلَىٰ أَهْلِهَا وَإِذَا حَكَمْتُمْ بَيْنَ النَّاسِ أَنْ تَحْكُمُوا بِالْعَدْلِ",
                        translation = "Şüphesiz Allah, emanetleri ehli olanlara vermenizi ve insanlar arasında hükmettiğiniz zaman adaletle hükmetmenizi emreder.",
                        explanation = "Devlet yönetiminden ticarete ve aile hayatına kadar liyakat ve adaletin temel ilahi emir olduğunu beyan eder.",
                        tags = listOf("Emanet", "Liyakat", "Adalet", "Hüküm")
                    ),
                    QuranIndexVerseItem(
                        surahId = 4,
                        surahName = "Nisâ Suresi",
                        verseNumber = 135,
                        arabic = "يَا أَيُّهَا الَّذِينَ آمَنُوا كُونُوا قَوَّامِينَ بِالْقِسْطِ شُهَدَاءَ لِلَّهِ وَلَوْ عَلَىٰ أَنْفُسِكُمْ أَوِ الْوَالِدَيْنِ وَالْأَقْرَبِينَ",
                        translation = "Ey iman edenler! Kendinizin, anne-babanızın ve yakınlarınızın aleyhine bile olsa Allah için şahitlik yaparak adaleti titizlikle ayakta tutan kimseler olun.",
                        explanation = "Tarafsız ve evrensel adaletin en yüksek standardını ortaya koyar.",
                        tags = listOf("Şahitlik", "Hakkaniyet", "Tarafsızlık")
                    ),
                    QuranIndexVerseItem(
                        surahId = 16,
                        surahName = "Nahl Suresi",
                        verseNumber = 90,
                        arabic = "إِنَّ اللَّهَ يَأْمُرُ بِالْعَدْلِ وَالْإِحْسَانِ وَإِيتَاءِ ذِي الْقُرْبَىٰ وَيَنْهَىٰ عَنِ الْفَحْشَاءِ وَالْمُنْكَرِ وَالْبَغْيِ",
                        translation = "Şüphesiz Allah adaleti, ihsanı (iyiliği ve güzelliği), yakınlara yardım etmeyi emreder; hayasızlığı, kötülüğü ve zorbalığı yasaklar.",
                        explanation = "Cuma hutbelerinin sonunda okunan ve İslam'ın sosyal-ahlaki nizamını özetleyen جامع (câmi) ayettir.",
                        tags = listOf("Adalet", "İhsan", "Hutbe Ayeti")
                    )
                )
            ),
            QuranTopicIndex(
                id = "aile_anne_baba",
                title = "Aile, Anne-Baba & Merhamet",
                icon = "🏡",
                category = "Toplum & Hukuk",
                description = "Anne-babaya ihsan, eşler arası muhabbet ve aile huzuru",
                verses = listOf(
                    QuranIndexVerseItem(
                        surahId = 17,
                        surahName = "İsrâ Suresi",
                        verseNumber = 23,
                        arabic = "وَقَضَىٰ رَبُّكَ أَلَّا تَعْبُدُوا إِلَّا إِيَّاهُ وَبِالْوَالِدَيْنِ إِحْسَانًا ۚ إِمَّا يَبْلُغَنَّ عِنْدَكَ الْكِبَرَ أَحَدُهُمَا أَوْ كِلَاهُمَا فَلَا تَقُلْ لَهُمَا أُفٍّ",
                        translation = "Rabbin, kendisinden başkasına asla ibadet etmemenizi, anne ve babaya iyi davranmanızı kesin olarak emretti. Onlardan biri veya her ikisi senin yanında yaşlanırsa, kendilerine 'Öf!' bile deme; onları azarlama; ikisine de güzel ve tatlı söz söyle.",
                        explanation = "Tevhid emrinden hemen sonra anne-babaya hürmet ve şefkati zikrederek aile vefasının önemini vurgular.",
                        tags = listOf("Anne Baba Hakkı", "Vefa", "Şefkat")
                    ),
                    QuranIndexVerseItem(
                        surahId = 30,
                        surahName = "Rûm Suresi",
                        verseNumber = 21,
                        arabic = "وَمِنْ آيَاتِهِ أَنْ خَلَقَ لَكُمْ مِنْ أَنْفُسِكُمْ أَزْوَاجًا لِتَسْكُنُوا إِلَيْهَا وَجَعَلَ بَيْنَكُمْ مَوَدَّةً وَرَحْمَةً",
                        translation = "Kendileriyle huzur bulasınız diye sizin için türünüzden eşler yaratması ve aranızda bir sevgi (meveddet) ve merhamet var etmesi de O'nun varlığının ve kudretinin delillerindendir.",
                        explanation = "Evlilik ve aile yuvasının temel direklerinin sekînet (huzur), meveddet (sevgi) ve rahmet (şefkat) olduğunu bildirir.",
                        tags = listOf("Evlilik", "Meveddet", "Rahmet", "Huzur")
                    ),
                    QuranIndexVerseItem(
                        surahId = 25,
                        surahName = "Furkân Suresi",
                        verseNumber = 74,
                        arabic = "رَبَّنَا هَبْ لَنَا مِنْ أَزْوَاجِنَا وَذُرِّيَّاتِنَا قُرَّةَ أَعْيُنٍ وَاجْعَلْنَا لِلْمُتَّقِينَ إِمَامًا",
                        translation = "Rabbimiz! Bize eşlerimizden ve nesillerimizden göz aydınlığı olacak (salih) kimseler ihsan eyle ve bizi takva sahiplerine önder kıl.",
                        explanation = "Huzurlu bir aile ve hayırlı nesiller için Kur'an'ın öğrettiği en kapsamlı aile duasıdır.",
                        tags = listOf("Aile Duası", "Hayırlı Evlat", "Göz Aydınlığı")
                    )
                )
            ),
            QuranTopicIndex(
                id = "infak_zekat",
                title = "İnfak, Zekât & Cömertlik",
                icon = "🌾",
                category = "İbadet & Kulluk",
                description = "Allah yolunda paylaşmak, sadaka bereketi ve yardımlaşma",
                verses = listOf(
                    QuranIndexVerseItem(
                        surahId = 2,
                        surahName = "Bakara Suresi",
                        verseNumber = 261,
                        arabic = "مَثَلُ الَّذِينَ يُنْفِقُونَ أَمْوَالَهُمْ فِي سَبِيلِ اللَّهِ كَمَثَلِ حَبَّةٍ أَنْبَتَتْ سَبْعَ سَنَابِلَ فِي كُلِّ سُنْبُلَةٍ مِائَةُ حَبَّةٍ",
                        translation = "Mallarını Allah yolunda harcayanların durumu, her başağında yüz dane olmak üzere yedi başak veren bir danenin durumu gibidir. Allah dilediğine kat kat fazlasını verir.",
                        explanation = "İnfak ve sadakanın malı eksiltmeyip bire yediyüz ve daha fazlasıyla bereketlendirdiğini çarpıcı bir misalle anlatır.",
                        tags = listOf("İnfak", "Sadaka", "Bereket", "Başak Misali")
                    ),
                    QuranIndexVerseItem(
                        surahId = 3,
                        surahName = "Âl-i İmrân Suresi",
                        verseNumber = 92,
                        arabic = "لَنْ تَنَالُوا الْبِرَّ حَتَّىٰ تُنْفِقُوا مِمَّا تُحِبُّونَ ۚ وَمَا تُنْفِقُوا مِنْ شَيْءٍ فَإِنَّ اللَّهَ بِهِ عَلِيمٌ",
                        translation = "Sevdiğiniz şeylerden Allah yolunda harcamadıkça gerçek iyiliğe (birr mertebesine) asla erişemezsiniz. Her ne harcarsanız Allah onu hakkıyla bilir.",
                        explanation = "Gerçek cömertliğin, insanın kendi değer verdiği ve sevdiği nimetlerden paylaşmasıyla gerçekleşeceğini bildirir.",
                        tags = listOf("Birr", "Cömertlik", "Paylaşmak")
                    )
                )
            ),
            QuranTopicIndex(
                id = "guzel_ahlak_dogruluk",
                title = "Güzel Ahlak, Sabır & Doğruluk",
                icon = "🌟",
                category = "Ahlak & Karakter",
                description = "Kötülüğü iyilikle savmak, tevazu, doğruluk ve hüsnüzan",
                verses = listOf(
                    QuranIndexVerseItem(
                        surahId = 41,
                        surahName = "Fussilet Suresi",
                        verseNumber = 34,
                        arabic = "وَلَا تَسْتَوِي الْحَسَنَةُ وَلَا السَّيِّئَةُ ۚ ادْفَعْ بِالَّتِي هِيَ أَحْسَنُ فَإِذَا الَّذِي بَيْنَكَ وَبَيْنَهُ عَدَاوَةٌ كَأَنَّهُ وَلِيٌّ حَمِيمٌ",
                        translation = "İyilikle kötülük bir olmaz. Sen (kötülüğü) en güzel olan davranışla sav; o zaman bir de göreceksin ki seninle arasında düşmanlık bulunan kimse candan, sıcak bir dost oluvermiş!",
                        explanation = "İnsan ilişkilerinde nebevî nezaket ve olgunluğun düşmanlıkları bile dostluğa dönüştüren gücünü açıklar.",
                        tags = listOf("İyilik", "Af", "Dostluk", "Hilm")
                    ),
                    QuranIndexVerseItem(
                        surahId = 49,
                        surahName = "Hucurât Suresi",
                        verseNumber = 12,
                        arabic = "يَا أَيُّهَا الَّذِينَ آمَنُوا اجْتَنِبُوا كَثِيرًا مِنَ الظَّنِّ إِنَّ بَعْضَ الظَّنِّ إِثْمٌ ۖ وَلَا تَجَسَّسُوا وَلَا يَغْتَبْ بَعْضُكُمْ بَعْضًا",
                        translation = "Ey iman edenler! Zannın çoğundan sakının; çünkü zannın bir kısmı günahtır. Birbirinizin gizli hallerini araştırmayın ve biriniz diğerini arkasından çekiştirmesin (gıybet etmesin).",
                        explanation = "Toplumsal güveni ve kardeşlik hukukunu koruyan üç temel ahlaki yasağı (suizan, tecessüs ve gıybet) vazeder.",
                        tags = listOf("Hüsnüzan", "Gıybet", "Mahremiyet", "Kardeşlik")
                    ),
                    QuranIndexVerseItem(
                        surahId = 31,
                        surahName = "Lokmân Suresi",
                        verseNumber = 18,
                        arabic = "وَلَا تُصَعِّرْ خَدَّكَ لِلنَّاسِ وَلَا تَمْشِ فِي الْأَرْضِ مَرَحًا ۖ إِنَّ اللَّهَ لَا يُحِبُّ كُلَّ مُخْتَالٍ فَخُورٍ",
                        translation = "İnsanlardan kibirle yüzünü çevirme ve yeryüzünde böbürlenerek yürüme! Zira Allah, kendini beğenip övünen hiç kimseyi sevmez.",
                        explanation = "Hz. Lokman'ın oğluna öğütleri üzerinden tevazuu ve kibirlenmekten sakınmayı emreder.",
                        tags = listOf("Tevazu", "Kibir", "Lokman Hekim")
                    )
                )
            ),
            QuranTopicIndex(
                id = "ahiret_cennet",
                title = "Ahiret, Kıyamet & Ebedi Hayat",
                icon = "🌅",
                category = "İman & Akide",
                description = "Hesap günü bilinci, amel defteri ve cennet müjdesi",
                verses = listOf(
                    QuranIndexVerseItem(
                        surahId = 99,
                        surahName = "Zilzâl Suresi",
                        verseNumber = 7,
                        arabic = "فَمَنْ يَعْمَلْ مِثْقَالَ ذَرَّةٍ خَيْرًا يَرَهُ ۝ وَمَنْ يَعْمَلْ مِثْقَالَ ذَرَّةٍ شَرًّا يَرَهُ",
                        translation = "Kim zerre ağırlığınca bir hayır işlerse onun karşılığını görür. Kim de zerre ağırlığınca bir kötülük işlerse onun karşılığını görür.",
                        explanation = "İlahi adalet terazisinde en küçük iyiliğin dahi zayi olmayacağını bildirir.",
                        tags = listOf("Zerre", "Mizan", "Ahiret Hesabı")
                    ),
                    QuranIndexVerseItem(
                        surahId = 59,
                        surahName = "Haşr Suresi",
                        verseNumber = 18,
                        arabic = "يَا أَيُّهَا الَّذِينَ آمَنُوا اتَّقُوا اللَّهَ وَلْتَنْظُرْ نَفْسٌ مَا قَدَّمَتْ لِغَدٍ ۖ وَاتَّقُوا اللَّهَ",
                        translation = "Ey iman edenler! Allah'a karşı gelmekten sakının ve herkes yarın (ahiret) için önceden ne gönderdiğine baksın.",
                        explanation = "Mümini her gün nefis muhasebesi yapmaya ve ebedi yarın için salih amel hazırlamaya çağırır.",
                        tags = listOf("Nefis Muhasebesi", "Takva", "Yarın İçin Hazırlık")
                    )
                )
            ),
            QuranTopicIndex(
                id = "kalp_huzuru_zikir",
                title = "Kalp Huzuru, Zikir & Sekînet",
                icon = "💚",
                category = "Kalp & Maneviyat",
                description = "Kalplerin Allah'ı anmakla huzur bulması ve manevi inşirah",
                verses = listOf(
                    QuranIndexVerseItem(
                        surahId = 13,
                        surahName = "Ra'd Suresi",
                        verseNumber = 28,
                        arabic = "الَّذِينَ آمَنُوا وَتَطْمَئِنُّ قُلُوبُهُمْ بِذِكْرِ اللَّهِ ۗ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
                        translation = "Onlar iman eden ve kalpleri Allah'ı anmakla (zikirle) huzura kavuşan kimselerdir. Biliniz ki kalpler ancak Allah'ı anmakla huzur bulur (mutmain olur).",
                        explanation = "Ruhsal daralma ve çağın huzursuzluklarına karşı kalbin gerçek sığınağının zikir olduğunu beyan eder.",
                        tags = listOf("Kalp Huzuru", "Zikir", "İtmi'nan")
                    ),
                    QuranIndexVerseItem(
                        surahId = 29,
                        surahName = "Ankebût Suresi",
                        verseNumber = 45,
                        arabic = "اتْلُ مَا أُوحِيَ إِلَيْكَ مِنَ الْكِتَابِ وَأَقِمِ الصَّلَاةَ ۖ إِنَّ الصَّلَاةَ تَنْهَىٰ عَنِ الْفَحْشَاءِ وَالْمُنْكَرِ ۗ وَلَذِكْرُ اللَّهِ أَكْبَرُ",
                        translation = "Kitaptan sana vahyolunanı oku ve namazı dosdoğru kıl! Şüphesiz namaz, insanı hayasızlıktan ve kötülükten alıkoyar. Allah'ı anmak (zikretmek) ise elbette en büyük ibadettir.",
                        explanation = "Kur'an tilaveti, namaz ve zikrin insanın ahlakını koruyan manevi kalkan olduğunu bildirir.",
                        tags = listOf("Namaz", "Tilavet", "Zikrullah")
                    )
                )
            )
        )
    }
}

