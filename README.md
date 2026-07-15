# PyLearn

Python'u adım adım öğreten Android uygulaması. Mimo tarzı **harita/patika** arayüzü: her ders bir düğüm, sırayla açılır. Teori yok — kısa anlatım, çalışan örnek, sonra sen yazarsın.

## Ders akışı

Her ders 3 adım:
1. **Anlatım** — 1-2 cümle, sadece işe yarayan bilgi
2. **Örnek** — çalışan kod bloğu
3. **Alıştırma** — kendin yazarsın, uygulama anında kontrol eder

Cevap kontrolü kodu cihazda çalıştırmaz; yazdığını normalize edip (boşluk/tırnak toleranslı) beklenen çözümle karşılaştırır.

## İçerik

`print → değişken → sayı/metin → input → if/else → döngü → liste → fonksiyon`

Ders eklemek/düzenlemek: sadece `app/src/main/assets/lessons.json` dosyasını değiştir. Kod değişikliği gerekmez.

## Derleme

Yerel kurulum gerekmez — **GitHub Actions** her push'ta APK üretir:
`Actions → Build APK → pylearn-debug-apk` artifact'ını indir.

Yerelde denemek istersen (Android SDK gerekli):
```
./gradlew assembleDebug
```

## Teknik

Kotlin + Jetpack Compose, minSdk 24. Harita `Canvas` ile çizilen bezier patika + tıklanabilir düğümler. İlerleme `SharedPreferences`'ta tutulur.
