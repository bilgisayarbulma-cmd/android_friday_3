# Android Studio Olmadan APK Alma (GitHub Actions ile)

Bu yöntem bilgisayarına HİÇBİR ŞEY kurmaz. Kodu GitHub'a yüklersin,
GitHub'ın kendi ücretsiz sunucuları arka planda APK'yı derler, sen de
sonucu indirirsin.

## 1) GitHub'da yeni bir repo (depo) oluştur

1. https://github.com adresine git, giriş yap
2. Sağ üstte **+** işaretine tıkla → **New repository**
3. Repository name: `friday-android` yaz (istediğin ismi verebilirsin)
4. **Private** seçeneğini işaretle (kodun herkese açık olmasın)
5. Diğer ayarları değiştirme, **Create repository** butonuna bas

## 2) Bu klasördeki dosyaları GitHub'a yükle

GitHub'ın web arayüzünden, komut satırı kullanmadan yükleyebilirsin:

1. Oluşturduğun repo sayfasında **"uploading an existing file"** linkine
   tıkla (ya da "Add file" → "Upload files")
2. Bu klasördeki (`friday-android`) TÜM dosya ve klasörleri sürükleyip
   bırak — `.github` klasörü dahil (bu klasör gizli görünebilir, dosya
   gezgininde "gizli dosyaları göster" açman gerekebilir)
3. Alt kısımda **Commit changes** butonuna bas

ÖNEMLİ: `.github/workflows/build-apk.yml` dosyasının da yüklendiğinden
emin ol — APK'yı otomatik derleyen kısım budur.

## 3) Derlemenin başlamasını bekle

1. Repo sayfasında üstteki **Actions** sekmesine tıkla
2. "Friday APK Derle" adında bir çalışma (workflow run) görünmeli,
   otomatik başlamış olmalı (sarı/turuncu nokta = çalışıyor)
3. Üzerine tıkla, ilerlemeyi izleyebilirsin. Derleme yaklaşık 3-5 dakika
   sürer (ilk seferde Gradle indirme nedeniyle biraz daha uzun olabilir)
4. Yeşil tik ✓ görünce derleme tamamlanmış demektir

Eğer kırmızı X görünürse, üzerine tıkla, hangi adımda hata olduğunu
görebilirsin — hata mesajını bana gönder, birlikte çözeriz.

## 4) APK'yı indir

1. Tamamlanan workflow çalışmasına tıkla (üstte "Friday APK Derle ✓" yazan)
2. Sayfanın altına kaydır, **Artifacts** bölümünde **friday-debug-apk**
   adında bir indirme linki göreceksin
3. Tıkla, bir .zip dosyası inecek
4. Zip'i aç, içinde `app-debug.apk` dosyası olacak

## 5) APK'yı telefonuna kur

1. `app-debug.apk` dosyasını telefonuna gönder (Google Drive, WhatsApp
   kendine mesaj, USB kablo, e-posta — herhangi bir yöntem)
2. Telefonda dosyaya dokun, kurulumu başlat
3. İlk seferde "Bilinmeyen kaynaklardan yükleme" izni isteyebilir —
   Ayarlar'dan o izni aç, kuruluma devam et
4. Kurulum bitince Friday ikonunu ana ekranda göreceksin

## Güncelleme yapmak istersen

Kodda bir değişiklik yapıp tekrar APK almak istersen:
1. GitHub'da değişen dosyaları tekrar yükle (Add file → Upload files)
2. Commit et — Actions otomatik tekrar derlemeye başlayacak
3. Yukarıdaki 3-4. adımları tekrarla

## Bir sorun olursa

Actions sekmesindeki kırmızı X'e tıklayıp çıkan hata logunu (kırmızı
yazılı kısmı) kopyalayıp bana gönder, birlikte çözeriz.
