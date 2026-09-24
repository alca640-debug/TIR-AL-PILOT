# AI backend sözleşmesi

Uygulamada **AI sunucusu** alanına bir HTTPS URL girilirse, uygulama o adrese aşağıdaki JSON ile `POST` yapar:

```json
{
  "question": "kullanıcının sorusu",
  "mode": "drive veya call",
  "language": "tr"
}
```

Beklenen cevap:

```json
{
  "answer": "kısa ve sürüş sırasında kolay anlaşılır cevap"
}
```

## Güvenlik

- OpenAI veya başka sağlayıcıların gizli API anahtarını **APK içine gömmeyin**.
- Anahtar sunucu tarafında tutulmalı.
- HTTPS kullanın.
- Sürüş modunda cevapları kısa tutun.
- Telefon görüşmesinin karşı tarafının sesini kaydetmeyin veya sunucuya göndermeyin.
