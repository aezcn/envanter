# Ev Envanteri

Ev içi gıda ve eşya envanteri. Dolap/konum bazlı ürün takibi, miktar, son kullanma
tarihi uyarıları, alışveriş listesi ve barkod tarama. İki kişilik ortak kullanım
için Supabase üzerinden senkronize olur.

Google Play'e çıkılmıyor; APK doğrudan telefona kuruluyor.

## Durum

| Faz | Kapsam | Durum |
|---|---|---|
| 0 | Proje iskeleti, CI, imzalama, dağıtım | ✅ kod hazır, doğrulama bekliyor |
| 1 | Supabase şeması, RLS, giriş, ev oluştur/katıl | ⬜ |
| 2 | Konumlar + ürünler, Room, temel ekranlar | ⬜ |
| 3 | Senkronizasyon (outbox + realtime) | ⬜ |
| 4 | SKT/azalan kartları, arama, hızlı ± | ⬜ |
| 5 | SKT bildirimleri | ⬜ |
| 6 | Alışveriş listesi | ⬜ |
| 7 | Barkod tarama | ⬜ |
| 8 | Aktivite geçmişi, ayarlar | ⬜ |

## Derleme

Derleme **tamamen GitHub Actions'da** yapılır — geliştirme makinesinde JDK veya
Android SDK kurulu değil. Bu yüzden repoda `gradle-wrapper.jar` da yok; CI
Gradle 8.10.2'yi kendisi kurar.

Yerelde derlemek isterseniz JDK 17 + Android SDK 35 kurup `gradle assembleRelease`
çalıştırmanız yeterli (yaklaşık 8-10 GB yer ister).

## İlk kurulum

### 1. Repoyu GitHub'a gönderin

```bash
git remote add origin git@github.com:<kullanici>/ev-envanteri.git && git push -u origin main
```

### 2. İmzalama anahtarını üretin (tek seferlik)

Actions → **"İmzalama anahtarı üret (tek seferlik)"** → Run workflow.

Biten çalışmadan `imzalama-anahtari` artifact'ını indirin, `secrets.txt` içindeki
değerleri Settings → Secrets and variables → Actions altına şu isimlerle girin:

- `KEYSTORE_BASE64` — `keystore.jks.base64` dosyasının tamamı
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Sonra artifact'ı GitHub'dan silin ve `keystore.jks`'i güvenli bir yerde yedekleyin.
**Bu dosyayı kaybederseniz uygulamayı bir daha güncelleyemezsiniz** — kaldırıp
yeniden kurmanız gerekir.

> Anahtar kurulmadan önce de CI çalışır, ama APK debug anahtarıyla imzalanır.
> O APK'nın üzerine sonradan release imzalı sürüm kurulamaz; anahtarı kurmadan
> telefona kalıcı bir sürüm yüklemeyin.

### 3. Supabase (Faz 1'de gerekecek)

Ücretsiz bir Supabase projesi açıp iki değeri Secrets'a ekleyin:

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`

Anon key gizli değildir — asıl koruma satır düzeyi güvenlik (RLS) politikalarıdır.

### 4. APK'yı telefona kurun

- **Test sürümü:** Actions → son çalışma → `ev-envanteri-apk` artifact'ı
- **Yayın sürümü:** `git tag v0.1.0 && git push --tags` → Releases sekmesinden indirin

Telefonda tarayıcıyla indirip açın. İlk seferde Android "bilinmeyen kaynaklardan
uygulama yükleme" izni isteyecek.

## Mimari

Detaylı tasarım — veri modeli, RLS politikaları, ekran akışı, senkronizasyon
stratejisi — plan dosyasında:
`~/.claude/plans/replicated-cooking-parasol.md`

Özet: Kotlin + Compose, tek modül, MVVM. Room yerel kaynak (offline-first);
yazmalar outbox'a düşer, WorkManager Supabase'e iter, uzak değişiklikler Realtime
ile geri akar. Çakışmalar `updated_at` üzerinden last-write-wins ile çözülür.
