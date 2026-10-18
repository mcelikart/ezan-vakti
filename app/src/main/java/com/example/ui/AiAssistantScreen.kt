package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HadithRepository
import com.example.ui.theme.AppTheme
import com.example.ui.theme.LocalAppTheme
import com.example.ui.theme.LocalThemeIsDark
import com.example.ui.theme.buttonBorder
import com.example.ui.theme.cardShape
import com.example.ui.theme.glassSurfaceColor
import com.example.ui.theme.navBarBottomPadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class AiChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

private object AiChatStore {
    private const val PREF_NAME = "islamic_ai_assistant_prefs"
    private const val KEY_HISTORY = "chat_history_minimal_v2"

    fun loadMessages(context: Context): List<AiChatMessage> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<AiChatMessage>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    AiChatMessage(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        text = obj.optString("text", ""),
                        isUser = obj.optBoolean("isUser", false),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveMessages(context: Context, messages: List<AiChatMessage>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        messages.takeLast(60).forEach { msg ->
            val obj = JSONObject().apply {
                put("id", msg.id)
                put("text", msg.text)
                put("isUser", msg.isUser)
                put("timestamp", msg.timestamp)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    fun clearMessages(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_HISTORY)
            .apply()
    }
}

private data class IlmihalTopic(
    val title: String,
    val keywords: List<String>,
    val answer: String
)

private object OfflineIslamicKnowledgeEngine {

    private val topics = listOf(
        IlmihalTopic(
            title = "Abdestin Farzları, Sünnetleri ve Alınışı",
            keywords = listOf("abdest", "abdestin farz", "abdest nasıl", "abdesti bozan", "abdest al"),
            answer = """**Abdestin Farzları (4'tür):**
1. Yüzü bir kez yıkamak
2. Kolları dirseklerle beraber bir kez yıkamak
3. Başın dörtte birini (1/4) meshetmek
4. Ayakları topuklarla beraber bir kez yıkamak

**Abdestin Sünnetleri ve Alınışı:**
• Niyet etmek ve "Eûzü-Besmele" çekmek
• Elleri bileklere kadar üç kez yıkamak
• Ağza ve burna üçer kez su vermek (mazmaza ve istinşak)
• Yüzü, kolları ve ayakları üçer kez yıkamak, başı ve kulakları meshetmek
• Azaları ara vermeden ve sırasıyla yıkamak

**Abdesti Bozan Başlıca Durumlar:**
• İdrar, dışkı veya yel çıkması
• Vücuttan kan veya irin çıkıp etrafına dağılması
• Ağız dolusu kusmak
• Yatarak veya yaslanarak uyumak, bayılmak
• Namazda yanındakinin duyacağı kadar gülmek"""
        ),
        IlmihalTopic(
            title = "Gusül (Boy Abdesti) Nasıl Alınır?",
            keywords = listOf("gusül", "gusul", "boy abdest", "cünüp", "hayız", "nifas", "adet"),
            answer = """**Guslün Farzları (3'tür):**
1. **Mazmaza:** Ağza bol su alıp boğaza kadar çalkalamak
2. **İstinşak:** Burna genze kadar su çekip temizlemek
3. **Bedenin Tamamını Yıkamak:** Vücutta iğne ucu kadar kuru yer kalmayacak şekilde tepeden tırnağa yıkanmak

**Sünnete Uygun Gusül Alınışı:**
• Önce "Niyet ettim Allah rızası için gusül abdesti almaya" diyerek Besmele çekilir.
• Eller yıkanır ve bedende necaset varsa temizlenir.
• Namaz abdesti gibi abdest alınır; ardından ağza ve burna üçer defa su verilir.
• Önce başa, sonra sağ omuza, ardından sol omuza üçer defa su dökülüp bütün vücut ovularak yıkanır."""
        ),
        IlmihalTopic(
            title = "Teyemmüm Nasıl Alınır ve Şartları Nelerdir?",
            keywords = listOf("teyemmüm", "teyemmum", "toprakla abdest", "su bulunma"),
            answer = """**Teyemmümün Farzları (2'dir):**
1. **Niyet etmek**
2. **Elleri temiz toprağa (veya toprak cinsinden bir yüzeye) iki kez vurup önce yüzü, sonra kolları meshetmek**

**Uygulanışı:**
• Su bulunamadığında veya suyu kullanmaya sağlık engeli olduğunda teyemmüm yapılır.
• Niyet edilip Besmele çekilir; parmaklar açık şekilde temiz toprağa vurulup silkelenir ve yüzün tamamı meshedilir.
• Eller ikinci kez toprağa vurulup önce sağ kol, sonra sol kol dirseklerle birlikte meshedilir."""
        ),
        IlmihalTopic(
            title = "Namazın Farzları (12 Farz)",
            keywords = listOf("namazın farz", "namazin farz", "12 farz", "içindeki farz", "dışındaki farz", "şartları", "rükün"),
            answer = """**Namazın Dışındaki Farzlar (Şartları - 6):**
1. **Hadesten Taharet:** Abdestsizlik veya cünüplük halini gidermek
2. **Necasetten Taharet:** Beden, elbise ve namaz kılınacak yeri pislikten temizlemek
3. **Setr-i Avret:** Örtülmesi gereken yerleri örtmek
4. **İstikbâl-i Kıble:** Kâbe (Kıble) yönüne dönmek
5. **Vakit:** Namazın vaktinin girmiş olması
6. **Niyet:** Kılınacak namaza kalben niyet etmek

**Namazın İçindeki Farzlar (Rükünleri - 6):**
1. **İftitah Tekbiri:** Namaza "Allâhü Ekber" diyerek başlamak
2. **Kıyam:** Namazda ayakta durmak
3. **Kıraat:** Ayakta Kur'ân'dan en az üç kısa ayet veya bir uzun ayet okumak
4. **Rükû:** Elleri diz kapaklarına koyarak eğilmek
5. **Secde:** Alın, burun, eller, dizler ve ayak parmaklarını yere koyarak iki kez kapanmak
6. **Ka'de-i Ahîre:** Namazın sonunda "Ettehiyyâtü" okuyacak kadar oturmak"""
        ),
        IlmihalTopic(
            title = "Beş Vakit Namazın Rekatları",
            keywords = listOf("kaç rekat", "kac rekat", "rekat", "sabah namazı", "öğle namazı", "ikindi namazı", "akşam namazı", "yatsı namazı", "vitir"),
            answer = """**Beş Vakit Namazın Rekat Tablosu (Toplam 40 Rekat):**

• **Sabah Namazı (4 Rekat):** 2 rekat sünnet + 2 rekat farz
• **Öğle Namazı (10 Rekat):** 4 rekat ilk sünnet + 4 rekat farz + 2 rekat son sünnet
• **İkindi Namazı (8 Rekat):** 4 rekat sünnet + 4 rekat farz
• **Akşam Namazı (5 Rekat):** 3 rekat farz + 2 rekat sünnet
• **Yatsı Namazı (10 Rekat) + Vitir (3 Rekat):** 4 rekat ilk sünnet + 4 rekat farz + 2 rekat son sünnet + 3 rekat vacip vitir namazı"""
        ),
        IlmihalTopic(
            title = "Sehiv Secdesi Nasıl ve Ne Zaman Yapılır?",
            keywords = listOf("sehiv", "yanılma secdesi", "namazda yanıl", "namazda şaşır", "namazda unut"),
            answer = """**Sehiv Secdesi Nedir?**
Namazda farzlardan birinin geciktirilmesi veya vaciplerden birinin unutularak terk edilmesi/geciktirilmesi durumunda namazın sonunda yapılan iki secdedir.

**Sehiv Secdesi Nasıl Yapılır?**
1. Namazın son oturuşunda sadece **"Ettehiyyâtü"** okunur.
2. Sağa (veya iki tarafa) selam verilir.
3. **"Allâhü Ekber"** diyerek arka arkaya **iki secde** yapılır.
4. Tekrar oturulup **Ettehiyyâtü, Salli-Bârik ve Rabbenâ** duaları okunur, ardından sağa ve sola selam verilerek namaz tamamlanır."""
        ),
        IlmihalTopic(
            title = "Kaza Namazı Nasıl Hesaplanır ve Kılınır?",
            keywords = listOf("kaza namaz", "geçmiş namaz", "kazaya kalan", "kaza borcu"),
            answer = """**Kaza Namazı Hükmü ve Kılınışı:**
• Vaktinde kılınamayan **beş vakit farz namazlar** ile **vitir namazının** kazası yapılır; sünnetlerin kazası (o günün sabah namazı sünneti öğleye kadar kılınırsa hariç) yapılmaz.
• **Kaza Namazı Rekatları:** Sabah 2, Öğle 4, İkindi 4, Akşam 3, Yatsı 4, Vitir 3 rekattır.
• **Niyet:** *"Niyet ettim Allah rızası için vaktinde kılamadığım ilk/son [Sabah/Öğle/İkindi/Akşam/Yatsı] namazının farzını kaza etmeye"* şeklinde niyet edilir.
• **Kerahat Vakitleri:** Güneş doğarken, tam tepedeyken (öğleden ~40 dk önce) ve güneş batarken kaza namazı kılınmaz; bunun dışındaki tüm vakitlerde kılınabilir."""
        ),
        IlmihalTopic(
            title = "Seferilik (Yolculukta Namaz) Hükümleri",
            keywords = listOf("seferi", "yolculuk", "90 km", "misafir", "kısalt", "cem et"),
            answer = """**Seferilik Şartları ve Hükümleri:**
• Bulunduğu yerleşim yerinden **en az 90 km** uzaklıktaki bir yere gitmek üzere yola çıkan ve orada **15 günden az** kalmaya niyet eden kimse dinen **seferî (yolcu)** sayılır.
• **4 rekatlı farz namazlar (Öğle, İkindi, Yatsı) 2 rekat** olarak kılınır. Sabah (2 rekat), Akşam (3 rekat) ve Vitir (3 rekat) namazları kısaltılmaz.
• Seferî olan kimse mukim (yolcu olmayan) bir imama uyarsa, imama tâbi olarak 4 rekatı tam kılar.
• Yolculuk esnasında vakit darlığı veya güçlük varsa öğle ile ikindi, akşam ile yatsı namazları cem edilerek kılınabilir."""
        ),
        IlmihalTopic(
            title = "Orucu Bozan ve Bozmayan Durumlar",
            keywords = listOf("orucu boz", "oruç bozulur", "diş fırçala", "iğne", "damla", "unutarak ye", "kusmak", "kan ver", "sakız", "kefaret"),
            answer = """**Orucu Bozan Başlıca Durumlar (Kaza Gerektirenler):**
• Yanlışlıkla (abdest alırken vb.) boğaza su kaçması
• Besleyici/vitamin içerikli serum veya iğne yaptırmak
• Ağız dolusu kendi isteğiyle kusmak
• İmsak vakti girdikten sonra veya akşam ezanı okunmadan önce yanılarak yiyip içmek

**Hem Kaza Hem Kefaret Gerektiren Durum:**
• Ramazan orucunu geçerli bir mazeret olmaksızın bilerek yemek, içmek veya cinsel ilişkide bulunmak.

**Orucu Bozmayan Durumlar:**
• **Unutarak yemek veya içmek** (Hatırladığı an ağzındakini çıkarıp oruca devam eder)
• Boğaza kaçırmadan diş fırçalamak, ağzı çalkalamak veya gusül almak
• Göz ve kulak damlası kullanmak
• Besleyici olmayan ağrı kesici/tedavi amaçlı iğne yaptırmak veya kan vermek"""
        ),
        IlmihalTopic(
            title = "Zekât, Nisap Miktarı ve Fitre Hükümleri",
            keywords = listOf("zekat", "zekât", "nisap", "fitre", "fıtır", "sadaka", "kimlere verilir", "altın zekat"),
            answer = """**Zekât Şartları ve Hesaplaması:**
• Temel ihtiyaçları ve borçları dışında **nisap miktarı (80.18 gram altın veya değerinde para/ticaret malı)** mala sahip olan ve üzerinden **1 kamerî yıl (354 gün)** geçen Müslümanlara zekât farzdır.
• **Zekât Oranı:** Malın **40'ta 1'i (%2,5)** oranındadır.

**Zekât ve Fitre Kimlere Verilir?**
• Fakirlere, düşkünlere, borçlulara, yolda kalmışlara ve ilim talebelerine verilir.

**Zekât ve Fitre Kimlere Verilmez?**
• **Usûl ve Fürûa verilmez:** Kişi kendi **annesine, babasına, dedesine/ninesine, kendi çocuklarına, torunlarına ve eşine** zekât veya fitre veremez. (Kardeş, teyze, hala, amca, dayı ve yeğenlere fakir iseler verilebilir.)"""
        ),
        IlmihalTopic(
            title = "Cuma, Bayram ve Teravih Namazları",
            keywords = listOf("cuma namaz", "bayram namaz", "teravih", "cenaze namaz"),
            answer = """**Cuma Namazı (10 Rekat):**
• 4 rekat ilk sünnet + 2 rekat farz (imamla cemaat halinde) + 4 rekat son sünnet.

**Bayram Namazı (2 Rekat - Vacip):**
• 1. rekatta Sübhaneke'den sonra 3 ilave (zâid) tekbir alınır.
• 2. rekatta kıraatten sonra rükûya gitmeden önce 3 ilave tekbir alınır, 4. tekbirle rükûya gidilir.

**Teravih Namazı (20 Rekat - Sünnet-i Müekkede):**
• Ramazan ayında yatsı namazının son sünnetinden sonra, vitir namazından önce 2'şer veya 4'er rekat halinde toplam 20 rekat kılınır.

**Cenaze Namazı (Farz-ı Kifaye):**
• Rükû ve secdesi yoktur; ayakta **4 tekbirle** kılınır: 1. tekbirde Sübhaneke (ve celle senâük ile), 2. tekbirde Salli-Bârik, 3. tekbirde cenaze duası (veya Rabbenâ), 4. tekbirde sağa ve sola selam verilir."""
        ),
        IlmihalTopic(
            title = "Nafile Namazlar (Teheccüd, Duha, Evvabin, İstihare, Hacet)",
            keywords = listOf("teheccüd", "duha", "kuşluk", "evvabin", "istihare", "hacet", "tesbih namaz", "işrak"),
            answer = """**Faziletli Nafile Namazlar:**
• **Teheccüd Namazı:** Yatsı namazından sonra bir miktar uyuyup gece kalkarak imsak vaktine kadar 2 ila 8 rekat kılınan çok faziletli gece namazıdır.
• **İstihare Namazı:** Hayırlı bir iş konusunda karar verirken 2 rekat namaz kılıp İstihare duası okumak sünnettir.
• **Hacet Namazı:** Bir dileği veya sıkıntısı olan kimsenin yatsıdan sonra 2 veya 4 rekat kılıp Allah'a dua etmesidir.
• **Duha (Kuşluk) Namazı:** Güneş doğduktan ~45 dk sonra başlayıp öğle kerahatine kadar 2, 4 veya 8 rekat kılınır.
• **Evvabin Namazı:** Akşam namazının sünnetinden sonra 2, 4 veya 6 rekat olarak kılınır."""
        ),
        IlmihalTopic(
            title = "Kurban İbadeti ve Şartları",
            keywords = listOf("kurban", "adak", "akika"),
            answer = """**Kurban Hükümleri:**
• Akıllı, ergenlik çağına ulaşmış, mukim ve nisap miktarı (80.18 gr altın değerinde) mala sahip olan Müslümanlara Kurban Bayramı'nda kurban kesmek **vaciptir**.
• **Kurbanlık Hayvanlar:** Koyun ve keçi (en az 1 kamerî yaşını doldurmuş, koyun 6 aylık olup 1 yaşındaki gibi gösterişliyse caizdir) **1 kişi** adına; sığır ve manda (2 yaşını doldurmuş) **1 ila 7 kişi** ortak olarak kesilebilir.
• **Adak Kurbanı:** Adak adayan kimse adadığı kurbanın etinden kendisi, eşi, anne-babası ve çocukları yiyemez; tamamını fakirlere dağıtır."""
        ),
        IlmihalTopic(
            title = "Namaz Duaları ve Kısa Sureler",
            keywords = listOf("sübhaneke", "ettehiyyatü", "salli", "barik", "rabbena", "kunut", "ayetel kürsi", "fatiha", "ihlas", "felak", "nas"),
            answer = """**Temel Namaz Duaları:**
• **Sübhaneke:** *"Sübhânekellâhümme ve bi hamdik ve tebârekesmük ve teâlâ ceddük (ve celle senâük - sadece cenaze namazında) ve lâ ilâhe ğayrük."*
• **Ettehiyyâtü:** *"Ettehiyyâtü lillâhi vessalevâtü vettayyibât. Esselâmü aleyke eyyühen-nebiyyü ve rahmetullâhi ve berekâtüh. Esselâmü aleynâ ve alâ ibâdillâhis-sâlihîn. Eşhedü en lâ ilâhe illallâh ve eşhedü enne Muhammeden abdühû ve rasûlüh."*
• **Rabbenâ Âtinâ:** *"Rabbenâ âtinâ fid'dünyâ haseneten ve fil'âhirati haseneten ve kınâ azâbennâr."*
• **Rabbenâğfirlî:** *"Rabbenâğfirlî ve li-vâlideyye ve lil-mü'minîne yevme yekûmül hisâb."*"""
        ),
        IlmihalTopic(
            title = "Tövbe, İstiğfar, Vesvese ve Manevi Huzur",
            keywords = listOf("tövbe", "tevbe", "istiğfar", "günah", "affedilir", "vesvese", "sıkıntı", "huzur", "bunal", "kul hakkı"),
            answer = """**Tövbe ve İstiğfarın Şartları:**
1. İşlenen günahtan kalben pişmanlık duymak
2. Günahı derhal terk etmek
3. Bir daha işlememeye kesin azmetmek
4. Eğer **kul hakkı** varsa hak sahibiyle helalleşmek veya hakkını ödemek

**Seyyidü'l-İstiğfar / Kısa İstiğfar:**
*"Estağfirullâhe'l-azîm ellezî lâ ilâhe illâ hüve'l-hayye'l-kayyûme ve etûbü ileyh."*

**Vesveseden Kurtulma:**
• Abdest veya namazda gelen vesveselere itibar edilmez; "Şeytan vesvese veriyor" diyerek üzerinde durmamak ve ibadete devam etmek en doğru yoldur."""
        )
    )

    fun generateAnswer(question: String): String {
        val normalized = question.lowercase(Locale("tr")).trim()
        if (normalized.isEmpty()) return "Lütfen merak ettiğiniz dini konuyu yazın."

        // Greeting check
        if (normalized in listOf("selam", "selamün aleyküm", "selamun aleykum", "merhaba", "iyi günler", "hayırlı günler")) {
            return "Ve aleyküm selam ve rahmetullah. Namaz, abdest, gusül, oruç, zekât, dua, hadis ve ilmihal konularında merak ettiğiniz soruları doğrudan yazabilirsiniz."
        }

        val queryWords = normalized
            .replace(Regex("[^a-zçğıöşü0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 3 }

        // 1. Score Ilmihal Topics
        val scoredTopics = topics.map { topic ->
            var score = 0
            topic.keywords.forEach { kw ->
                if (normalized.contains(kw)) score += 6
            }
            queryWords.forEach { word ->
                val stem = word.take(5)
                if (topic.title.lowercase(Locale("tr")).contains(stem)) score += 3
                if (topic.answer.lowercase(Locale("tr")).contains(stem)) score += 1
            }
            topic to score
        }.filter { it.second > 0 }.sortedByDescending { it.second }

        // 2. Search matching Hadiths from HadithRepository
        val matchingHadiths = HadithRepository.hadiths.filter { hadith ->
            queryWords.any { w ->
                val stem = w.take(4)
                hadith.category.lowercase(Locale("tr")).contains(stem) ||
                    hadith.text.lowercase(Locale("tr")).contains(stem) ||
                    hadith.explanation.lowercase(Locale("tr")).contains(stem)
            }
        }.take(2)

        val sb = StringBuilder()

        if (scoredTopics.isNotEmpty()) {
            val best = scoredTopics.first().first
            sb.append("### ${best.title}\n\n")
            sb.append(best.answer.trim())

            if (scoredTopics.size > 1 && scoredTopics[1].second >= 5) {
                val second = scoredTopics[1].first
                sb.append("\n\n---\n### ${second.title}\n\n")
                sb.append(second.answer.trim())
            }
        }

        if (matchingHadiths.isNotEmpty()) {
            if (sb.isNotEmpty()) sb.append("\n\n---\n")
            sb.append("### İlgili Hadis-i Şerif\n")
            matchingHadiths.forEach { h ->
                sb.append("\n• “${h.text}” _(${h.source})_\n")
            }
        }

        if (sb.isEmpty()) {
            return """Sorunuzla ilgili temel ilmihal özeti:

• **İbadetlerde Temel Ölçü:** İslam'da ameller niyetlere göredir. Farz ibadetlerin (namaz, oruç, zekât, hac) şartlarına riayet edilmesi, şüpheli durumlarda ise kolaylık ve ihtiyat ilkesine uyulması tavsiye edilir.
• **Daha Detaylı Arama İçin:** Sorunuzu *"abdestin farzları"*, *"sehiv secdesi"*, *"orucu bozan şeyler"*, *"kaza namazı"*, *"zekât kimlere verilir"*, *"gusül nasıl alınır"* veya *"şifa duası"* gibi anahtar kelimelerle yazabilirsiniz."""
        }

        return sb.toString().trim()
    }
}

@Composable
fun AiAssistantScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val activeTheme = LocalAppTheme.current
    val isDark = LocalThemeIsDark.current
    val isGlass = activeTheme != AppTheme.CLASSIC_EMERALD && activeTheme.spec.isGlass
    val bottomBarPad = navBarBottomPadding()

    var messages by remember { mutableStateOf(AiChatStore.loadMessages(context)) }
    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    fun sendQuestion(questionText: String) {
        val trimmed = questionText.trim()
        if (trimmed.isEmpty() || isThinking) return

        keyboardController?.hide()
        inputText = ""

        val userMsg = AiChatMessage(text = trimmed, isUser = true)
        val updatedWithUser = messages + userMsg
        messages = updatedWithUser
        AiChatStore.saveMessages(context, updatedWithUser)
        isThinking = true

        coroutineScope.launch {
            delay(180)
            val answer = OfflineIslamicKnowledgeEngine.generateAnswer(trimmed)
            val assistantMsg = AiChatMessage(text = answer, isUser = false)
            val finalMessages = updatedWithUser + assistantMsg
            messages = finalMessages
            AiChatStore.saveMessages(context, finalMessages)
            isThinking = false
            delay(60)
            if (finalMessages.isNotEmpty()) {
                listState.animateScrollToItem(finalMessages.size - 1)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = bottomBarPad)
                .imePadding()
        ) {
            // Minimal Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "Soru Sor",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (messages.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            messages = emptyList()
                            AiChatStore.clearMessages(context)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Sohbeti Temizle",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
            )

            // Main Chat Content (Minimal Empty State without preset question templates)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 72.dp, bottom = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.QuestionAnswer,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Dini Soru & İlmihal Rehberi",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Namaz, abdest, oruç, zekât, dua ve hadis konularında merak ettiğiniz soruyu aşağıdan yazabilirsiniz.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                } else {
                    items(messages, key = { it.id }) { msg ->
                        MinimalChatBubble(
                            message = msg,
                            isGlass = isGlass,
                            activeTheme = activeTheme,
                            isDark = isDark,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(msg.text))
                                Toast.makeText(context, "Kopyalandı", Toast.LENGTH_SHORT).show()
                            },
                            onSaveToNotes = {
                                NotesStore.addQuickNote(
                                    context = context,
                                    title = "Soru-Cevap Notu",
                                    content = msg.text,
                                    category = "Genel"
                                )
                                Toast.makeText(context, "Not defterine kaydedildi", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // Minimal Input Bar
            Surface(
                color = if (isGlass) glassSurfaceColor(activeTheme) else MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f)
                ),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = "Sorunuzu yazın...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        },
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = { sendQuestion(inputText) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { sendQuestion(inputText) },
                        enabled = inputText.isNotBlank() && !isThinking,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Gönder",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimalChatBubble(
    message: AiChatMessage,
    isGlass: Boolean,
    activeTheme: AppTheme,
    isDark: Boolean,
    onCopy: () -> Unit,
    onSaveToNotes: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val cleanDisplay = remember(message.text) {
        message.text
            .replace("### ", "")
            .replace("**", "")
            .replace("_", "")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.isUser) 18.dp else 6.dp,
                bottomEnd = if (message.isUser) 6.dp else 18.dp
            ),
            color = if (message.isUser) {
                MaterialTheme.colorScheme.primary
            } else if (isGlass) {
                glassSurfaceColor(activeTheme)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.78f else 0.94f)
            },
            border = if (message.isUser) null else if (isGlass) activeTheme.buttonBorder else BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
            ),
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                if (!message.isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "İlmihal Yanıtı",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = cleanDisplay,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = if (message.isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormat.format(Date(message.timestamp)),
                        fontSize = 10.sp,
                        color = if (message.isUser) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        }
                    )

                    if (!message.isUser) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = onCopy,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Kopyala",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            IconButton(
                                onClick = onSaveToNotes,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkAdd,
                                    contentDescription = "Notlara Kaydet",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
