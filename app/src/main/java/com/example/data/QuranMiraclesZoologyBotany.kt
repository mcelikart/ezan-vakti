package com.example.data

object QuranMiraclesZoologyBotany {
    val list: List<MiracleItem> = listOf(
        MiracleItem(
            id = "dog_panting_thermoregulation",
            title = "Köpeğin Dilini Çıkarıp Soluması (Termoregülasyon)",
            subtitle = "Köpekte ter bezi olmaması ve ısıyı dilden soluyarak atması",
            category = MiracleCategory.ZOOLOGY_BOTANY,
            surahName = "A'râf Sûresi",
            verseNumber = "176. Ayet",
            surahIndex = 7,
            verseIndex = 176,
            arabicVerse = "فَمَثَلُهُ كَمَثَلِ الْكَلْبِ إِنْ تَحْمِلْ عَلَيْهِ يَلْهَثْ أَوْ تَتْرُكْهُ يَلْهَثْ",
            verseTranslation = "Onun durumu tıpkı bir köpeğin haline benzer: Üzerine varsan da dilini sarkıtıp solur (yelhes), kendi haline bıraksan da dilini sarkıtıp solur.",
            scientificDiscoveryTitle = "Veteriner Fizyolojisi & Köpeklerde Panting (Termoregülasyon)",
            discoveryCentury = "20. Yüzyıl Hayvan Fizyolojisi",
            scientificExplanation = """
                Köpeklerin derisinde insanlardaki gibi ter bezleri bulunmaz (yalnızca patilerinde minimaldir). Bu yüzden köpekler ister koşsunlar ister gölgede dursunlar vücut ısılarını düşürmek için daima dillerini sarkıtarak hızlı hızlı solumak (Panting / Yelhes - يَلْهَثْ) zorundadırlar. Ayet köpeğin bu değişmez fizyolojik davranışını harika bir benzetmeyle zikreder.
            """.trimIndent(),
            keyScientificFacts = listOf(
                "Köpekler vücut ısısını terleyerek değil, dakikada 300-400 kez dilden soluyarak dengeler.",
                "'Yelhes' fiili köpeğin kesintisiz soluma refleksini tam karşılar."
            ),
            quotesAndReferences = listOf(
                "C.R. Taylor, 'The Thermoregulatory Significance of Panting in Dogs'",
                "Veterinary Physiology Textbook"
            ),
            tags = listOf("köpek", "termoregülasyon", "yelhes", "soluma", "araf")
        )
    )
}
