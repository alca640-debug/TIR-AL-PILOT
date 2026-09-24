# Telefonda kurulum

Bilgisayar gerekmiyor.

1. GitHub deposunda **Actions** sekmesine gir.
2. **Build Android APK** iş akışını aç.
3. En son yeşil tikli çalışmayı aç.
4. Sayfanın altındaki **Artifacts** bölümünden `TIR-AI-Pilot-debug-apk` paketini indir.
5. İnen ZIP'i aç.
6. `app-debug.apk` dosyasına dokun ve Android'in istediği “bu kaynaktan uygulama yükleme” iznini ver.
7. Uygulamayı açıp mikrofon, telefon durumu ve bildirim izinlerini ver.
8. **Pilot'u sürekli hazır başlat** düğmesine bas.

## İlk test

- “Hey Pilot, merhaba” de.
- Bildirimde cevap görünmeli; görüşme yoksa cevap sesli okunmalı.
- Telefon görüşmesi başlayınca bildirim **Görüşme Modu** olur.
- Görüşme sırasında Android mikrofonu Pilot'a vermeyebilir. Bu durumda görüşme algılanır fakat sesli komut alınamayabilir.
