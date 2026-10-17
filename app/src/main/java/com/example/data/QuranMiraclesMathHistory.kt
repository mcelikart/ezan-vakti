package com.example.data

object QuranMiraclesMathHistory {
    val list: List<MiracleItem> = listOf(
        MiracleItem(
            id = "conquest_of_mecca_prophecy",
            title = "Mekke'nin Fethi ve Mescid-i Haram'a Güvenle Giriş Kehaneti",
            subtitle = "Müslümanlar sürgün ve zayıfken Mekke'nin fethinin önceden müjdelenmesi",
            category = MiracleCategory.HISTORY,
            surahName = "Fetih Sûresi",
            verseNumber = "27. Ayet",
            surahIndex = 48,
            verseIndex = 27,
            arabicVerse = "لَتَدْخُلُنَّ الْمَسْجِدَ الْحَرَامَ إِنْ شَاءَ اللَّهُ آمِنِينَ مُحَلِّقِينَ رُءُوسَكُمْ وَمُقَصِّرِينَ",
            verseTranslation = "Andolsun ki Allah, elçisinin rüyasını doğru çıkardı. Allah dilerse siz güven içinde, başlarınızı tıraş etmiş ve kısaltmış olarak Mescid-i Haram'a mutlaka gireceksiniz!",
            scientificDiscoveryTitle = "Mekke'nin Fethi (MS 630 / Hicri 8) & Tarihsel Gerçekleşme",
            discoveryCentury = "7. Yüzyıl Tarihi",
            scientificExplanation = """
                Hudeybiye Antlaşması yapıldığında Müslümanlar Mekke'ye sokulmamış ve zayıf görünmüştü. Bu ortamda inen ayet; Müslümanların kesinlikle Mekke'ye galip ve 'güven içinde, başlarını tıraş etmiş hacılar olarak' gireceklerini müjdeledi. 2 yıl sonra (MS 630) Mekke tek bir damla kan dökülmeden fethedilmiş ve ayetteki kehanet harfiyen gerçekleşmiştir.
            """.trimIndent(),
            keyScientificFacts = listOf(
                "İmkansız gibi görünen bir siyasi ve askeri geleceğin tam doğrulanması.",
                "Ayette belirtilen tıraş olma ve güven içinde tavaf etme detayları aynen vuku buldu."
            ),
            quotesAndReferences = listOf(
                "İbn Hişâm, 'es-Sîretü'n-Nebeviyye'",
                "Martin Lings, 'Muhammad: His Life Based on the Earliest Sources'"
            ),
            tags = listOf("mekke fethi", "fetih", "mescidi haram", "kehanet", "tarih")
        )
    )
}
