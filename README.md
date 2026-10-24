# VakitBox Pro

VakitBox Pro, namaz vakitlerini ve günlük ibadet araçlarını tek bir Android uygulamasında bir araya getirir. Mevcut uygulama ikonu korunmuştur.

## Özellikler

- Konuma göre namaz vakitleri ve vakte kalan süreyi gösteren ana ekran.
- Ezan bildirimleri: vakitten 10 dakika önce ve vakit girdiğinde uyarı; sürekli kalan süre bildirimi; vakit bildirimi 5 dakika sonra kapanır.
- Güneş doğuşuna göre birden fazla günlük alarm. Varsayılan olarak güneşten 30 dakika önce çalar; süre değiştirilebilir. Telefonun varsayılan alarm sesi ve erteleme/kapatma kontrolleri kullanılır.
- API kullanmadan cihazda çalışan Kur’an fihristi. Konunun anlamına göre ayetleri sıralar; 114 sure ve 6.236 ayet, arama modeliyle birlikte uygulamanın içindedir.
- Kıble pusulası, Kur’an ve sesli tilavet, imsakiye, hicrî takvim, zikir sayacı, namaz rehberi, adım sayar ve ana ekran araç takımı.
- Büyük ve okunaklı durum çubuğu sayacı; farklı ekran ölçülerine uyumlu arayüz.

Kur’an fihristi internet bağlantısı veya API anahtarı istemez. Hava durumu, harita ve isteğe bağlı asistan özellikleri gibi çevrimiçi hizmetler bağlantı gerektirebilir.

## Kurulum

Uygulamayı yayımlanmış paketlerden indirmek için [GitHub Releases](https://github.com/mcelikart/vakitbox-pro/releases) sayfasını kullanın. Kaynak kodundan derlemek için Android Studio ve JDK 17 gerekir.

Arama modeli Git LFS üzerinden saklanır. Depoyu klonladıktan sonra:

1. Git LFS'i kurup etkinleştirin: `git lfs install`
2. Model dosyasını alın: `git lfs pull`
3. Android Studio'da projeyi açın veya `./gradlew assembleDebug` komutunu çalıştırın.

## Sürüm

Mevcut sürüm **5.3.0 (10)**. Uygulamadaki Hakkında ekranı sürüm ve yapı numarasını gösterir. Sürüm numaralarının nasıl ilerlediği için [VERSIONING.md](VERSIONING.md) dosyasına bakın.

## Veri ve lisanslar

Kur’an metni, arama modeli ve kaynak bilgileri uygulama içindeki `app/src/main/assets/quran_search` dizininde açıklanmıştır. Model ve veri lisanslarını dağıtım koşullarına uygun biçimde koruyun.
