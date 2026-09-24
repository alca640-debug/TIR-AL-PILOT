# TIR AI Pilot 🚛🧠

Android için sürekli hazır sürüş ve görüşme asistanı prototipi.

## Bilgisayarsız APK alma

Bu depo GitHub Actions ile bulutta APK derler. Bilgisayar gerekmez.

1. GitHub'da bu depoyu aç.
2. **Actions** bölümüne gir.
3. **Build Android APK** çalışmasını aç.
4. Yeşil tik oluşunca çalışmayı aç.
5. **Artifacts** bölümündeki `TIR-AI-Pilot-debug-apk` paketini indir.
6. ZIP'i telefonda aç ve `app-debug.apk` dosyasını kur.

Her `main` güncellemesinde yeni APK otomatik derlenir.

## Prototip 0.1

- Android 12+ (`minSdk 31`)
- Android 16 hedefi (`targetSdk 36`)
- Görünür mikrofon foreground servisi
- “Hey Pilot” komutlarını deneme amaçlı dinleme
- Telefon görüşmesinin aktif olup olmadığını algılama
- Normal kullanımda sesli yanıt
- Görüşmede cevabı bildirimde gösterme
- Bildirimden tek dokunuşla **DURDUR**
- İsteğe bağlı HTTPS AI backend bağlantısı

## Teknik sınır

Android'in standart `SpeechRecognizer` API'si gerçek bir düşük güç hotword motoru değildir. İlk prototip mimariyi test etmek için dinlemeyi yeniden başlatır. Üretim sürümünde cihaz içi özel wake-word motoru kullanılacaktır.

Normal üçüncü taraf Android uygulaması hücresel telefon görüşmesindeki karşı tarafın sesini serbestçe yakalayamaz. Bu proje bu güvenlik sınırını aşmaya çalışmaz ve karşı tarafın sesini kaydetmez.

## Sonraki hedefler

- Gerçek düşük güç “Hey Pilot” wake-word motoru
- GPS ve sınır algılama
- Kapıkule / Frigo canlı takip
- Sefer, yakıt, fiş ve termokin kayıtları
- Bluetooth kulaklık kısa cevap modu
- Sürüşe uygun büyük kartlar
