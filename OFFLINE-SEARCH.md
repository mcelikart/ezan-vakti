# Çevrimdışı Kur’an fihristi

Fihrist araması yalnızca APK içindeki dosyaları kullanır. `OfflineQuranSearch` model ve dizini arka plan iş parçacığında açar, soruyu `query: ` önekiyle kodlar ve yerel ayet vektörlerine göre sıralar. Ağ istemcisi veya Gemini bağlantısı yoktur. Ekrandan çıkınca model kapatılır; iptal edilen aramalar yeni sonuçların üzerine yazamaz.

Veri: 114 sure, 6.236 özgün Arapça ayet ve QuranEnc Rowwad Türkçe meali. 29 yayıncı dipnotu ayrı saklanır. Arapça ve meal metinleri yeniden üretilmez veya değiştirilemez. Kaynak sürümleri, yeniden dağıtım koşulları ve SHA-256 değerleri `app/src/main/assets/quran_search/{SOURCE.json,NOTICE.txt}` içindedir.

Arama modeli: MIT lisanslı `intfloat/multilingual-e5-small`, sabit `ccc66d3` sürümünün `onnx/model_qint8_avx512_vnni.onnx` dosyası. ONNX graph CPU sağlayıcısıyla çalışır; dosya adındaki AVX512 bir uygulama donanım şartı değildir. Gerçek Kotlin/JVM çıkarımı Python ONNX sonuçlarıyla karşılaştırılarak doğrulanmıştır. Fiziksel Android cihaz ölçümü ayrıca yapılmalıdır.

Her sorgu için SentencePiece normalleştirme ve Unigram parçalama cihazda çalışır. Maskeli ortalama ve L2 normalleştirme ile 384 boyutlu vektör hesaplanır. Ayetler, uzun ayet parçaları ve kısa pasaj bağlamları önceden kodlanmıştır. Konu adları, eş anlamlı ifadeler ve doğal dil örnekleri ayrıca `query: ` uzayında bağımsız anlam vektörleri olarak saklanır; uzun örneklerle ortalamaları alınarak kısa konu adlarının sinyali kaybedilmez. Çalışma sırasında sözcükten ayete sabit bir eşleştirme yapılmaz: model konu vektörlerini anlamsal olarak karşılaştırır, eşik ve rakip konu farkını değerlendirir. Ayet metninin kendi anlam vektörü sonuçları ayrıca sıralar. Konu ile zayıf ilişkili bütün ayetler aynı puanla öne çıkarılmaz. Çok düşük benzerlikte zorunlu bir yedek ayet döndürülmez. Anlamsal sıralama her soruyu kusursuz anlayacağı veya bir dinî hüküm vereceği anlamına gelmez.

Yerel dosyaları yeniden üretmek yalnızca geliştirme aşamasında ağ ve Python araçları gerektirir. Uygulama kullanıcısının bunlara ihtiyacı yoktur.

```sh
python3 -m venv work/semantic-venv
work/semantic-venv/bin/pip install onnxruntime==1.22.0 tokenizers==0.21.4 numpy
python3 scripts/download-quran-corpus.py
python3 scripts/prepare-quran-corpus.py
```

Aynı sabit model sürümünden `tokenizer.json` ve `onnx/model_qint8_avx512_vnni.onnx` dosyalarını indirin. Modeli `app/src/main/assets/quran_search/encoder.onnx`, tokenizer dosyasını örneğin `work/model-source/tokenizer.json` olarak saklayın. İndirilen dosya kimliklerini `semantic-index.json` ve `UNIGRAM-SOURCE.json` değerleriyle karşılaştırın. `semantic-topics.json` sürüm kontrollü veri dosyası olarak korunmalıdır.

```sh
work/semantic-venv/bin/python scripts/export-e5-tokenizer.py --source work/model-source/tokenizer.json
work/semantic-venv/bin/python scripts/prepare-semantic-index.py \
  --tokenizer work/model-source/tokenizer.json --report work/semantic-evaluation-e5.json
python3 scripts/export-semantic-test-fixture.py work/semantic-evaluation-e5.json
```

`semantic-index.json` paketlenen dosya kimliklerini ve formatı tanımlar. `vectors.bin` özgün ayet/pasaj vektörlerini (QSV2), `concept-anchors.bin` bağımsız konu vektörlerini (QCA1) içerir; ikisinde de aynı sabit E5 modeli kullanılır. `QuranSemanticSearchTest` kapsam, veri bütünlüğü, gerçek model eşdeğerliği, doğal dil sorularına ilişkin ayetler ve bozuk girdi denetimlerini yapar. `XlmRobertaTokenizerTest` bağımsız HF tokenizer örnekleriyle çalışır. Ekran testleri küçük ekranda büyük yazıyı, arama iptalini ve açılır metinleri doğrular.

Model APK içinde saklandığından paket boyutu artar. Model dosyası sıkıştırılmadan paketlenip belleğe eşlenir; akışla okunan dizin ve tokenizer varlıkları APK içinde sıkıştırılır; ilk aramada RAM ve yükleme maliyeti vardır. Saniyeli kalıcı bildirim sayacı ile fihrist farklı bileşenlerde çalışır.


Tek APK, desteklenen dört Android mimarisinin tamamını korur (arm64-v8a, armeabi-v7a, x86, x86_64); dosya boyutu tek bir telefon modeline göre uyumluluk azaltılarak küçültülmez.
