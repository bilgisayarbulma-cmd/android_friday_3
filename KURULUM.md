# FRIDAY Android - Kurulum Kılavuzu

Bu, Friday'in Android telefonlar için gerçek bir uygulama sürümüdür. Windows
sürümünden tamamen ayrı bir projedir (Kotlin ile yazılmıştır), aynı Gemini
API anahtarını kullanabilirsin ama ikisi birbirinden bağımsız çalışır.

## Mobilde neler farklı / neler eksik

Windows sürümündeki bazı özellikler mobilde anlamsız olduğu için
**kasıtlı olarak eklenmedi**:
- "Not defteri aç", "hesap makinesi aç" gibi masaüstü uygulama açma →
  yerine `open_app` ile telefon uygulamaları (WhatsApp, YouTube, Spotify,
  Chrome, Kamera, Ayarlar, Telefon) açılıyor
- Masaüstü kısayolu → Android'de zaten ana ekrana yüklenince ikon otomatik
  oluşur
- Webcam analizi, el takibi, ekran görüntüsü, Google Takvim/Gmail, WhatsApp
  Web otomasyonu, Canlı Mod (Live API) → bu ilk sürümde YOK, sadece temel
  özellikler var (aşağıdaki tabloya bak). İstersen bunları ayrı adımlarda
  ekleyebiliriz.

## Bu sürümde olan özellikler

- Mikrofon ile sesli komut (Android'in yerleşik ses tanıma motoru, ek
  kütüphane/API key gerektirmez)
- Sesli yanıt (Android'in yerleşik TTS motoru)
- Yazarak komut gönderme (metin kutusu)
- Gemini AI ile doğal dil anlama + function calling (Windows'taki gibi)
- Çoklu API anahtarı desteği (biri limit dolunca otomatik diğerine geçer)
- Saat, hava durumu, hesap makinesi, hafıza
- Uygulama açma (WhatsApp, YouTube, Spotify, Chrome, Kamera, Ayarlar, Telefon)
- Web'de arama, YouTube/Spotify'da müzik arama
- SMS/arama hazırlama (GÜVENLİK: otomatik göndermez/aramaz, sen onaylarsın)

## Kurulum (bilgisayarında, derlemek için)

### 1) Android Studio'yu kur

https://developer.android.com/studio adresinden ücretsiz indir, kur
(~1GB+ indirme, kurulum biraz sürebilir).

### 2) Projeyi aç

1. Android Studio'yu aç
2. "Open" veya "Open an existing project" seç
3. Bu klasördeki `friday-android` klasörünü seç
4. Android Studio projeyi tanıyıp gerekli Gradle dosyalarını (gradle-wrapper.jar
   dahil) otomatik tamamlayacak — ilk açılışta internet bağlantısı gerekir,
   biraz sürebilir ("Gradle sync" işlemi)

### 3) Telefonunu bağla veya emülatör kullan

**Gerçek telefon (önerilen):**
1. Telefonunda Ayarlar → Telefon Hakkında → "Yapım Numarası"na 7 kez dokun
   (Geliştirici Seçenekleri açılır)
2. Ayarlar → Geliştirici Seçenekleri → "USB Hata Ayıklama"yı aç
3. Telefonu USB ile bilgisayara bağla, telefonda çıkan izin isteğini onayla
4. Android Studio'da üstteki cihaz listesinde telefonun görünmeli

**Emülatör (telefon olmadan test):**
1. Android Studio'da "Device Manager" → "Create Device"
2. Bir telefon modeli seç (örn. Pixel 7), sistem imajı indir, oluştur

### 4) Çalıştır

Android Studio'da üstteki yeşil "▶ Run" butonuna bas. Uygulama telefona
(veya emülatöre) kurulup otomatik açılacak.

## Uygulamayı kullanma

1. İlk açılışta mikrofon izni isteyecek, "İzin Ver" de
2. Sağ üstteki ⚙ (dişli) ikonuna tıkla, Gemini API anahtarını gir
   (aistudio.google.com/apikey adresinden ücretsiz alınır), şehrini yaz,
   KAYDET'e bas
3. Ana ekrana dön, mikrofon butonuna basıp konuş, ya da alttaki metin
   kutusuna yazıp gönder

## APK olarak dışa aktarma (telefonuna kurulum dosyası)

Eğer Android Studio'yu her seferinde açmak istemiyorsan, kurulabilir bir
APK dosyası oluşturabilirsin:

1. Android Studio'da üst menüden **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Derleme bitince çıkan bildirimde "locate" linkine tıkla
3. `app-debug.apk` dosyasını telefonuna kopyala (USB, Google Drive, vb.)
4. Telefonda dosyaya dokunup kur (ilk seferde "bilinmeyen kaynaklardan
   yükleme"ye izin vermen istenebilir, Ayarlar'dan onaylayabilirsin)

## Bir hata görürsen

Android Studio'nun alt kısmındaki "Build" veya "Logcat" panelinde hatayı
görebilirsin. Hata mesajının tam metnini paylaşırsan birlikte çözeriz.

## Sıradaki adımlar (istersen ekleyebileceğimiz özellikler)

- Webcam/kamera ile görüntü analizi (OCR dahil)
- El takibi ve jest kontrolü
- Google Takvim/Gmail entegrasyonu
- "Canlı Mod" (kesintisiz sesli sohbet, Gemini Live API ile)
- Ekran görüntüsü alma
- Daha fazla telefon uygulaması açma desteği
