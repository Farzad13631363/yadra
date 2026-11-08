# YADRA Android — قرارداد واقعی سرور فعال‌سازی لایسنس YADRAN

تاریخ بررسی: ۲۰۲۶-۱۱-۰۸. این فایل مستقل و قابل انتقال به لپ‌تاپ اندروید است.

**دامنه کار:** استخراج قرارداد موجود، بررسی فقط‌خواندنی سفارش ۳، و مستندسازی؛ هیچ endpoint، فیلد اجرایی، رکورد، پرداخت، لایسنس یا فعال‌سازی ایجاد/ویرایش نشد. هیچ درخواست واقعی `begin`، `complete` یا login اجرا نشد. این سند مجوز تغییر سرور یا اجرای فعال‌سازی واقعی نیست.

## ۱. نتیجه قطعی برای سفارش ۳

اطلاعات زیر با SQL در تراکنش `READ ONLY` از PostgreSQL کانتینر در حال اجرا بررسی شد؛ کد لایسنس، اطلاعات هویتی مشتری، توکن یا کلید خوانده/چاپ نشد:

| مورد | مقدار تأییدشده |
| --- | --- |
| شماره سفارش | `3` |
| وضعیت سفارش | `paid` |
| پرداخت paid با verified_at و مبلغ برابر سفارش | وجود دارد |
| تعداد لایسنس متعلق به این سفارش در نتیجه بررسی | ۱ |
| `licenseId` واقعی | رشته `"2"` |
| `softwareCode` واقعی | `"diag"` |
| وضعیت لایسنس | `active` |
| انقضای ثبت‌شده لایسنس | `null` |
| مالک لایسنس و حساب سفارش | منطبق |
| فعال‌سازی active قبلی برای این لایسنس | وجود ندارد |

این وضعیت، snapshot زمان بررسی است؛ پیش از فعال‌سازی باید با حساب مالک، فهرست لایسنس‌ها دوباره خوانده شود. از `orderId = 3` به جای `licenseId = "2"` استفاده نکنید. مقدار `yadra-desktop` در بعضی تست‌ها ساختگی است؛ مقدار واقعی این لایسنس **`diag`** است.

**مشکل `licenseId`:** پاسخ واقعی `activation/begin` فاقد این فیلد است، ولی شناسه در متن امضا لازم است. راه موجود و بدون تغییر سرور: ابتدا `GET /api/v1/customer/licenses` با Bearer مشتری، انتخاب همان کد/نرم‌افزار، و نگه‌داری `id` و `softwareCode` آن رکورد. تست یکپارچه موجود سرور نیز دقیقاً شناسه را از همین فهرست می‌گیرد.

## ۲. نسخه و شواهد قرارداد

چهار فایل زیر از دیسک کانتینر `yadran-api` و workspace خوانده شدند؛ SHA-256 بایت‌های هر فایل در هر دو محل یکسان بود:

| فایل | SHA-256 |
| --- | --- |
| `api/customer-routes.js` | `5729253fad5aab2ab5e228eb5fff43b92273833f6b6d8173e6e0bb8cf959d7d8` |
| `api/license-activation-service.js` | `940a7528004416659cc7205000153511f13744f1a8b0728dafa7aafe42830f32` |
| `api/auth-utils.js` | `7fee599f0ec3284e273a2c54ffa1f5354d93a94d8eca01f619eeba665d56edc2` |
| `api/turnstile.js` | `f18bfd869dba4656be809a567a505334c3748a9e1d42d7c582be06966aa54121` |

همسانی فایل کانتینر با سورس، جای تست HTTP روی گوشی یا اثبات کد loadشده در حافظه یک پردازش قدیمی را نمی‌گیرد. فرآیند سرور restart نشد.

منابع محلی تکمیلی بررسی‌شده:

- `api/license-service.js` برای قالب کد و نرمال‌سازی آن؛
- `api/main.js` برای dispatch، خطاهای سطح بالاتر و CORS؛
- `api/migrations/017_license_security_and_plans.sql` و `018_license_activation_device_binding.sql` برای challenge و اتصال دستگاه؛
- `web/app/login/page.tsx` و `web/app/components/turnstile-widget.tsx` برای ورود واقعی سایت؛
- `api/test/license-customer-flow.test.js`؛
- `api/test/license-purchase-activation-integration.test.js`؛
- `api/test/license-readiness-security.test.js`.

این سه فایل تست اجرا شدند: **۱۱ تست موفق، صفر شکست**. آن‌ها از دیتابیس/درگاه شبیه‌سازی‌شده و کلیدهای موقت آزمایشی استفاده می‌کنند؛ لایسنس واقعی سفارش ۳ فعال نشده است. کد APK/پروژه اندروید در این workspace پیدا نشد.

## ۳. Base URL، HTTP و فهرست endpointهای موجود

در پیکربندی موجود سایت، origin کانونی `https://yadran.ir` است. `Caddyfile`، `www.yadran.ir` را به آن هدایت می‌کند و `web/next.config.ts` مسیر `/api/v1/*` را به API متصل می‌کند. همین origin تنظیم‌شده مبنای زیر است؛ دسترسی DNS/TLS از گوشی در این کار تست نشده است.

```text
Origin:   https://yadran.ir
API base: https://yadran.ir/api/v1
```

| کار | Method | مسیر واقعی |
| --- | --- | --- |
| ورود مشتری موجود | POST | `/api/v1/customer/login` |
| فهرست لایسنس‌های حساب | GET | `/api/v1/customer/licenses` |
| شروع فعال‌سازی اولیه | POST | `/api/v1/customer/license-activation/begin` |
| تکمیل فعال‌سازی اولیه | POST | `/api/v1/customer/license-activation/complete` |
| شروع اعتبارسنجی آنلاین دستگاه فعال‌شده | POST | `/api/v1/customer/license-activation/verify/begin` |
| تکمیل اعتبارسنجی آنلاین | POST | `/api/v1/customer/license-activation/verify/complete` |

درخواست‌های POST باید JSON UTF-8 بفرستند. برای چهار مسیر فعال‌سازی و فهرست لایسنس:

```http
Authorization: Bearer <CUSTOMER_ACCESS_TOKEN_FROM_LOGIN>
Content-Type: application/json
Accept: application/json
```

مقدار داخل `<...>` صرفاً جای‌نگهدار است، نه توکن واقعی. GET بدنه ندارد. توکن در query string قرار نگیرد. `userId` در body فرستاده نشود؛ سرور آن را از `sub` توکن می‌گیرد. هدر باید با متن دقیق `Bearer ` شروع شود. کوکی نشست وب یا توکن مدیر جای Bearer مشتری را نمی‌گیرد.

این مسیرها پاسخ JSON با `Content-Type: application/json; charset=utf-8` دارند. مسیرهای فعال‌سازی و فهرست لایسنس با `Cache-Control: private, no-store, max-age=0` پاسخ می‌دهند. فهرست همچنین `Pragma: no-cache` دارد.

**تمام پاسخ‌های موفق دارای `success: true` نیستند.** کلاینت باید DTO هر endpoint را جداگانه بخواند، نه یک envelope عمومی اجباری.

## ۴. احراز هویت و دریافت Bearer Token

### ۴.۱ ورود حساب موجود

`POST /api/v1/customer/login`، بدون Bearer، با فیلدهای واقعی زیر:

```json
{
  "mobile": "<شماره ثبت‌شده همان مشتری>",
  "password": "<رمز واردشده توسط مشتری در زمان اجرا>",
  "turnstileToken": "<خروجی یک‌بارمصرف Turnstile با action صحیح>"
}
```

سه مقدار بالا نمونه ساختاری‌اند و هیچ credential واقعی در این سند وجود ندارد.

- `mobile`: سرور trim می‌کند و فاصله، `(`، `)` و `-` را حذف می‌کند؛ نتیجه باید با `^\+?[0-9]{8,15}$` مطابق باشد. تبدیل `+98` به `09` یا تبدیل ارقام فارسی در این login انجام نمی‌شود؛ شماره حساب باید با قالب ذخیره‌شده مطابق باشد.
- `password`: برای login باید غیرخالی باشد؛ بررسی رمز با hash حساب موجود است.
- `turnstileToken`: رشته غیرخالی با حداکثر طول ۲۰۴۸؛ در حالت عادی سرور موفقیت Cloudflare، hostname برابر `yadran.ir` یا `www.yadran.ir` و action دقیق **`customer_login`** را بررسی می‌کند.
- مسیر `register` هم وجود دارد، اما برای لایسنس سفارش ۳، حساب تازه نسازید؛ حساب مالک فعلی لازم است.

پاسخ HTTP 200:

```json
{
  "token": "<REDACTED_CUSTOMER_ACCESS_TOKEN>",
  "user": {
    "id": "<شناسه حساب از سرور>",
    "name": "<نام حساب>",
    "mobile": "<شماره حساب>",
    "province": "<استان یا null>",
    "city": "<شهر یا null>",
    "address": "<نشانی یا null>",
    "postalCode": "<کد پستی یا null>"
  }
}
```

`token` در سطح root است؛ `data.token`، `accessToken` و `refreshToken` در پاسخ این login وجود ندارند. `user.id` بدون تبدیل صریح از نتیجه PostgreSQL می‌آید؛ آن را در مدل کلاینت به شناسه متنی تبدیل کنید و روی عدد بودنش تکیه نکنید. نمونه بالا nullهای محتمل را فقط برای توضیح در رشته نشان می‌دهد؛ پاسخ واقعی می‌تواند JSON `null` داشته باشد.

توکن صادرشده JWT با امضای سمت سرور و اعتبار **۳۶۰۰ ثانیه** است. `sub` رشته شناسه حساب و `role` برابر `customer` است؛ `iat` و `exp` بر حسب ثانیه Unix هستند. کلاینت توکن را تولید یا امضا نمی‌کند و هیچ secret امضای سرور نباید وارد APK شود. برای این جریان refresh endpoint مستندی در کد بررسی‌شده وجود ندارد؛ پس از 401 ورود مجدد لازم است.

### ۴.۲ ملاحظه واقعی اندروید و Turnstile

ارسال صرف شماره/رمز از native HTTP کافی نیست؛ login موجود به Turnstile نیاز دارد. برای گرفتن proof معتبر، اندروید باید یک جریان وب مورد اعتماد روی hostname مجاز را یکپارچه کند. [مستند رسمی Cloudflare برای موبایل](https://developers.cloudflare.com/turnstile/get-started/mobile-implementation/) استفاده از محیط مرورگر/WebView دارای JavaScript، DOM storage و دسترسی شبکه Turnstile را توضیح می‌دهد.

مسیر وب موجود `/login` پس از ورود موفق، توکن را در localStorage با کلید **`yadra_customer_token`** ذخیره می‌کند و به `/` می‌رود. در سرور فعلی endpoint تبادل توکن native، callback اندروید، deep link احراز هویت یا bridge آماده‌ای مشاهده نشد.

**انتخاب پیاده‌سازی اندروید، نه قرارداد جدید سرور:** اگر از WebView خود اپ و صفحه موجود استفاده می‌کنید، تحویل توکن از همان origin مورد اعتماد به لایه native باید در اپ با کنترل دقیق origin/navigation و رضایت ورود کاربر پیاده‌سازی شود. بازکردن مرورگر خارجی به‌تنهایی توکن را به اپ برنمی‌گرداند. هیچ endpoint فرضی برای دورزدن CAPTCHA نسازید. proof تستی Cloudflare یا کلید عمومی صفحه localhost را برای production به کار نبرید. کلید عمومی لازم از تنظیم وب موجود می‌آید؛ secret Cloudflare هرگز متعلق به اپ نیست.

## ۵. دریافت `licenseId` و `softwareCode` بدون تغییر سرور

`GET /api/v1/customer/licenses` با Bearer همان حساب مشتری:

```json
{
  "success": true,
  "data": {
    "licenses": [
      {
        "id": "2",
        "code": "<REDACTED_LICENSE_CODE_FROM_SERVER>",
        "status": "active",
        "product": "<نام محصول>",
        "software": "<نام نمایشی نرم‌افزار>",
        "softwareCode": "diag",
        "plan": "<نام طرح>",
        "durationDays": null,
        "isPerpetual": true,
        "expiresAt": null,
        "issuedAt": "2030-01-01T00:00:00.000Z",
        "activation": {
          "activated": false,
          "activatedAt": null
        }
      }
    ]
  }
}
```

مثال صرفاً شکل پاسخ را نشان می‌دهد؛ نام‌ها، تاریخ و مقادیر طرح نمونه‌اند. فقط `id = "2"`، `softwareCode = "diag"`، وضعیت، انقضای null و نبود فعال‌سازی، برای سفارش ۳ جداگانه تأیید شدند. محتوای واقعی `code` عمداً خوانده یا درج نشده است.

| فیلد هر رکورد | نوع/معنا در سرور |
| --- | --- |
| `id` | string؛ همان `licenseId` مورد استفاده در امضا |
| `code` | string یا null؛ نسخه رمزگشایی‌شده برای صاحب حساب، نه هش یا hint |
| `status` | وضعیت لایسنس؛ `active`، `suspended`، `revoked` یا `expired`؛ انقضای زمانی نیز در فهرست اعمال می‌شود |
| `product`, `software`, `plan` | نام‌های نمایشی رکوردهای مرتبط، برای ساخت امضا استفاده نمی‌شوند |
| `softwareCode` | slug واقعی نرم‌افزار؛ برای این مورد `diag` |
| `durationDays` | مدت طرح یا null |
| `isPerpetual` | boolean طرح |
| `expiresAt`, `issuedAt` | تاریخ‌های JSON سرور؛ انقضا می‌تواند null باشد |
| `activation.activated` | وجود فعال‌سازی active دارای fingerprint؛ به‌تنهایی اثبات نمی‌کند دستگاه فعلی همان دستگاه است |
| `activation.activatedAt` | تاریخ فعال‌سازی active یا null |

سرور فقط لایسنس‌های متعلق به `sub`، مرتبط با محصول/طرح معتبر و سفارش دارای پرداخت paid/verified و مبلغ برابر را برمی‌گرداند. اگر کد رمز‌شده خوانده نشود، ممکن است کل endpoint با `503 LICENSE_LIST_UNAVAILABLE` شکست بخورد؛ `code: null` را کد معتبر تلقی نکنید.

ترتیب کار در اندروید:

1. ورود با حساب مالک سفارش ۳؛ دریافت Bearer.
2. خواندن فهرست؛ انتخاب رکورد `id = "2"` و `softwareCode = "diag"` در این آزمون اول.
3. در محصول نهایی شناسه ۲ را برای همه کاربران hardcode نکنید؛ رکورد را از پاسخ واقعی انتخاب کنید. اگر کاربر کد را وارد می‌کند، با کد همان رکورد پس از نرمال‌سازی قالب مقایسه شود.
4. اگر رکورد دیده نشد، ابتدا حساب، پرداخت/صدور و خطای API بررسی شود؛ شناسه یا کد حدس زده نشود.
5. نگه‌داری `id`, `softwareCode`, کلید دستگاه و hash دستگاه از قبل از begin تا پایان complete.

این پاسخ **`orderId` ندارد** و فیلتر `?orderId=3` در کد فهرست پیاده‌سازی نشده است. هیچ مسیر فرضی مثل `/customer/licenses/2` یا `/orders/3/license` به این قرارداد اضافه نشده است.

## ۶. قالب داده‌های رمزنگاری

| داده | قرارداد دقیق |
| --- | --- |
| `softwareCode` | در begin فعال‌سازی و verify/begin با `^[a-z0-9][a-z0-9_-]{0,63}$` بررسی می‌شود؛ slug، نه نام محصول/بسته Android |
| `licenseCode` | فقط در activation/begin؛ سرور حروف را uppercase و whitespace و `-` را حذف می‌کند؛ نتیجه باید `YD` و سپس ۴۸ کاراکتر hex بزرگ باشد |
| `licenseId` | شناسه لایسنس به‌صورت string؛ برای سفارش ۳ `"2"`؛ داخل متن امضا لازم است |
| `challengeId` | شناسه challenge به‌صورت string؛ الگوی `[1-9]\d{0,18}` |
| `challenge` | nonce صادرشده سرور؛ ۴۳ کاراکتر Base64URL بدون padding |
| `androidIdHash` | دقیقاً ۶۴ رقم hex **کوچک**، الگوی `^[a-f0-9]{64}$` |
| `publicKeyBase64` | Base64 استانداردِ DER از نوع **SPKI / X.509 SubjectPublicKeyInfo** برای کلید عمومی EC P-256 |
| `fingerprint` | lowercase hexِ `SHA-256(publicKeyDerBytes)`؛ نه هش متن Base64، PEM، certificate یا JWK |
| `signature` | Base64 استانداردِ امضای **DER ASN.1 ECDSA** روی متن دقیق payload با SHA-256 |

منحنی مورد پذیرش سرور `prime256v1` است؛ همان NIST P-256 / `secp256r1` در Java/Android. PEM با BEGIN/END، JSON Web Key و نقطه خام `04 || X || Y` به جای SPKI پذیرفته نیستند.

Base64 کلید و امضا در سرور strict است: nonempty، حداکثر ۸۱۹۲ کاراکتر و بازتولید عین رشته با Base64 استاندارد. از Base64 استاندارد **بدون line wrap و با padding لازم `=`** استفاده کنید. Base64URL فقط برای challenge است، نه برای کلید/امضا.

**نکته `androidIdHash`:** سرور فقط قالب ۶۴ hex را اعتبارسنجی می‌کند؛ از روی این دو فایل نمی‌توان الگوریتم استخراج آن از Android یا salt را استنتاج کرد. سیاست پیشنهادی سمت اپ برای نصب اول می‌تواند `SHA-256(UTF-8(Settings.Secure.ANDROID_ID))` با hex کوچک باشد، ولی این **انتخاب کلاینت** است، نه الگوریتم الزام‌شده در کد سرور. اگر پروژه اندروید سیاست موجودی دارد، قبل از تغییر بررسی شود؛ مقدار باید در همه درخواست‌های همان دستگاه ثابت بماند. raw Android ID ارسال نشود.

سرور possession کلید امضاکننده و تطبیق شناسه‌ها را بررسی می‌کند؛ Android hardware attestation / Play Integrity در این چهار endpoint وجود ندارد. ادعای تأیید سخت‌افزاری اصالت گوشی از این قرارداد نکنید.

## ۷. Challenge چگونه تولید و مصرف می‌شود؟

در `activation/begin` و در شاخه معتبر `verify/begin`:

```text
nonceBytes = crypto.randomBytes(32)
challenge = nonceBytes.toString("base64url")
nonce_hash = SHA256(nonceBytes).hexLowerCase
expires_at = NOW() + INTERVAL '5 minutes'
```

رکورد سرور شامل license، purpose (`activate` یا `verify`)، هش nonce، رشته کلید عمومی، fingerprint، androidIdHash و انقضاست. مقدار خام nonce در دیتابیس ذخیره نمی‌شود؛ خروجی به کلاینت همان `challenge` است.

در complete، سرور `challenge` را Base64URL-decode کرده و هش **بایت‌ها** را مقایسه می‌کند. challenge باید متعلق به همان حساب/لایسنس/purpose/کلید/hash دستگاه باشد، مصرف نشده باشد و زمان سرور هنوز قبل از انقضا باشد.

برای **امضا**، متن ۴۳ کاراکتری challenge را عیناً در payload قرار دهید؛ بایت‌های decodeشده یا هش challenge را داخل payload جایگزین نکنید. challenge را اندروید تولید نمی‌کند.

## ۸. فعال‌سازی اولیه — `begin`

`POST /api/v1/customer/license-activation/begin`

```json
{
  "licenseCode": "<REDACTED_LICENSE_CODE_FROM_SELECTED_RECORD>",
  "softwareCode": "diag",
  "androidIdHash": "<64_LOWERCASE_HEX_DEVICE_HASH>",
  "publicKeyBase64": "<STANDARD_BASE64_SPKI_DER_PUBLIC_KEY>",
  "fingerprint": "<64_LOWERCASE_HEX_SHA256_OF_PUBLIC_KEY_DER>"
}
```

پاسخ HTTP 200، **بدون `success`، `data`، `licenseId` یا `softwareCode`**:

```json
{
  "challengeId": "10001",
  "challenge": "<SERVER_BASE64URL_NONCE_43_CHARACTERS>",
  "expiresAt": "2030-01-01T00:05:00.000Z"
}
```

شناسه challenge و تاریخ نمونه‌اند؛ از پاسخ زنده گرفته شوند. `expiresAt` اینجا انقضای **challenge** است. این درخواست فقط challenge می‌سازد؛ فعال‌سازی هنوز انجام نشده است.

## ۹. امضای فعال‌سازی — قالب دقیق

تابع مرجع سرور: `buildActivationPayload`.

```text
YADRA-LICENSE-ACTIVATE
v1
{challengeId}
{challenge}
{licenseId}
{softwareCode}
{androidIdHash}
{fingerprint}
```

این متن دقیقاً ۸ خط و ۷ جداکننده LF (`\n`، بایت `0A`) دارد:

- prefix دقیق **`YADRA`** است، نه `YADRAN`؛
- هیچ `\r`، BOM، فاصله اضافی یا newline انتهایی وجود ندارد؛
- شناسه‌ها رشته ده‌دهی خود سرورند؛ برای مورد فعلی خط پنجم `2` و خط ششم `diag` است؛
- خود `licenseCode` و خود `publicKeyBase64` داخل متن امضا نیستند؛
- روی بایت‌های UTF-8 تمام همین متن، `SHA256withECDSA` اجرا شود؛ پیام را از قبل hash نکنید، چون الگوریتم Signature خودش SHA-256 را اعمال می‌کند.

خروجی باید امضای DER باشد؛ فرمت ۶۴ بایتی JOSE/IEEE-P1363 از نوع `r || s` با انتظار فعلی سرور یکسان نیست. سرور `crypto.verify("sha256", UTF8(payload), key, signatureBytes)` را بدون تغییر dsaEncoding صدا می‌زند؛ [مرجع Node.js، قالب DER پیش‌فرض](https://nodejs.org/download/release/v24.21.0/docs/api/crypto.html#cryptoverifyalgorithm-data-key-signature-callback).

## ۱۰. فعال‌سازی اولیه — `complete`

`POST /api/v1/customer/license-activation/complete`

```json
{
  "challengeId": "10001",
  "challenge": "<EXACT_CHALLENGE_FROM_ACTIVATION_BEGIN>",
  "softwareCode": "diag",
  "androidIdHash": "<EXACT_SAME_DEVICE_HASH>",
  "publicKeyBase64": "<EXACT_SAME_BASE64_PUBLIC_KEY>",
  "fingerprint": "<EXACT_SAME_PUBLIC_KEY_FINGERPRINT>",
  "signature": "<STANDARD_BASE64_DER_SHA256withECDSA_SIGNATURE>"
}
```

`licenseId` در body این endpoint لازم نیست و route آن را به سرویس فعال‌سازی ارسال نمی‌کند؛ سرور شناسه را از رکورد challenge به دست می‌آورد. **با اضافه‌کردن licenseId به complete، امضای اشتباه اصلاح نمی‌شود.** مقدار درست باید از قبل در متن امضا آمده باشد.

پاسخ HTTP 200:

```json
{
  "success": true,
  "licenseId": "2",
  "status": "active",
  "expiresAt": null
}
```

`expiresAt` در این پاسخ، انقضای **خود لایسنس** است و با انقضای challenge تفاوت دارد. سرور در تراکنش، فعال‌سازی active ثبت می‌کند، challenge را مصرف می‌کند، زمان فعال‌سازی لایسنس و Audit را ثبت می‌کند.

فعال‌سازی اولیه برای لایسنس active غیرمنقضی و بدون فعال‌سازی active قبلی است. یک دستگاه نمی‌تواند هم‌زمان لایسنس active دیگری از همان نرم‌افزار داشته باشد. برای لایسنس ازقبل‌فعال‌شده، به جای تلاش مکرر activate از verify استفاده شود.

## ۱۱. اعتبارسنجی دستگاه موجود — `verify/begin`

`POST /api/v1/customer/license-activation/verify/begin`

```json
{
  "licenseId": "2",
  "softwareCode": "diag",
  "androidIdHash": "<SAME_DEVICE_HASH_USED_AT_ACTIVATION>",
  "publicKeyBase64": "<SAME_BASE64_PUBLIC_KEY_USED_AT_ACTIVATION>",
  "fingerprint": "<SAME_PUBLIC_KEY_FINGERPRINT_USED_AT_ACTIVATION>"
}
```

اینجا `licenseId` ورودی واقعی و لازم است؛ `licenseCode` لازم نیست.

اگر لایسنس active، غیرمنقضی و فعال‌سازی active دقیقاً با همان دستگاه/کلید موجود باشد، HTTP 200:

```json
{
  "status": "needs_online",
  "challengeId": "10002",
  "challenge": "<SERVER_VERIFY_BASE64URL_NONCE_43_CHARACTERS>",
  "expiresAt": "2030-01-01T00:05:00.000Z"
}
```

`needs_online` به معنی موفقیت نهایی لایسنس نیست؛ فقط اجازه ادامه اثبات امضاست. باید verify/complete انجام شود.

اگر لایسنس برای حساب پیدا نشود، دستگاه/کلید منطبق نباشد یا وضعیت اجازه ندهد، ممکن است همان HTTP 200 با **فقط یک فیلد** بیاید:

```json
{"status":"device_invalid"}
```

یا:

```json
{"status":"expired"}
```

یا:

```json
{"status":"revoked"}
```

در این سه پاسخ challenge وجود ندارد؛ کلاینت نباید امضا بسازد یا با فیلدهای null، verify/complete را صدا بزند. روی اولین دستگاهی که هنوز activate نشده، `device_invalid` در verify/begin طبیعی است؛ جریان اولیه باید activate باشد.

## ۱۲. امضای اعتبارسنجی و `verify/complete`

تابع مرجع سرور: `buildVerificationPayload`.

```text
YADRA-LICENSE-VERIFY
v1
{challengeId}
{challenge}
{licenseId}
{softwareCode}
{androidIdHash}
{fingerprint}
```

همان قواعد UTF-8/LF/نبود newline انتهایی/DER/Base64 بخش فعال‌سازی برقرار است، اما prefix باید **VERIFY** باشد. امضای ACTIVATE برای VERIFY معتبر نیست. challenge دو purpose قابل تعویض نیست.

`POST /api/v1/customer/license-activation/verify/complete`:

```json
{
  "challengeId": "10002",
  "challenge": "<EXACT_CHALLENGE_FROM_VERIFY_BEGIN>",
  "softwareCode": "diag",
  "androidIdHash": "<SAME_DEVICE_HASH>",
  "publicKeyBase64": "<SAME_BASE64_PUBLIC_KEY>",
  "fingerprint": "<SAME_PUBLIC_KEY_FINGERPRINT>",
  "signature": "<STANDARD_BASE64_DER_VERIFY_SIGNATURE>"
}
```

شناسه لایسنس باز هم از challenge سمت سرور گرفته می‌شود؛ فرستادن `licenseId` در این complete لازم نیست، ولی در payload امضا لازم است.

پاسخ نهایی معتبر HTTP 200:

```json
{"status":"active"}
```

اگر وضعیت در این فاصله تغییر کند، HTTP 200 با `status` برابر `expired`، `revoked` یا `device_invalid` ممکن است. در پاسخ verify/complete فیلدهای `success`، `data`، `licenseId`، `expiresAt` یا توکن تازه وجود ندارند. challenge بعد از امضای معتبر و رسیدن به نتیجه، مصرف می‌شود.

## ۱۳. وضعیت‌ها و خطاهای واقعی

### ۱۳.۱ چهار endpoint فعال‌سازی/اعتبارسنجی

خطای احراز هویت، HTTP 401:

```json
{"error":"CUSTOMER_AUTH_REQUIRED"}
```

در این 401، `success` وجود ندارد. در مقابل، همین خطا در endpoint فهرست لایسنس، `success: false` نیز دارد.

خطاهای catch فعال‌سازی:

```json
{
  "success": false,
  "error": "SIGNATURE_INVALID"
}
```

| HTTP | `error` دقیق | معنا/رفتار پیشنهادی کلاینت |
| --- | --- | --- |
| 400 | `INVALID_DEVICE_ID` | hash دستگاه قالب مورد انتظار را ندارد؛ در verify/complete، fingerprint بدقالب نیز با همین کد رد می‌شود |
| 400 | `INVALID_FINGERPRINT` | قالب fingerprint نامعتبر؛ به‌ویژه در beginها |
| 400 | `INVALID_PUBLIC_KEY` | Base64 کلید خالی/نامعتبر/بیش‌ازحد؛ برخی خطاهای parse DER به خطای عمومی 500 می‌روند |
| 400 | `INVALID_DEVICE_KEY_TYPE` | کلید غیر EC P-256 |
| 400 | `FINGERPRINT_MISMATCH` | SHA-256 بایت‌های DER با fingerprint برابر نیست |
| 400 | `INVALID_SOFTWARE_CODE` | قالب softwareCode نامعتبر؛ در مسیرهایی که صریحاً آن را اعتبارسنجی می‌کنند |
| 400 | `INVALID_LICENSE_CODE` | قالب کد بعد از نرمال‌سازی نامعتبر |
| 400 | `INVALID_ID` | challengeId یا licenseId مربوط به آن مرحله قالب معتبر ندارد |
| 400 | `INVALID_CHALLENGE` | قالب nonce ارسالی غلط است |
| 400 | `INVALID_SIGNATURE` | رشته Base64 امضا نامعتبر/خالی/بیش‌ازحد |
| 403 | `LICENSE_INACTIVE` | فعال‌سازی اولیه برای لایسنس غیرفعال یا منقضی مجاز نیست |
| 403 | `SIGNATURE_INVALID` | proof معتبر نیست؛ canonical payload، شناسه، DER و کلید بررسی شوند |
| 404 | `LICENSE_NOT_FOUND` | لایسنس متعلق به حساب/نرم‌افزار نیست یا شروط محصول/طرح/پرداخت معتبر در begin برقرار نیست |
| 409 | `CHALLENGE_INVALID_OR_EXPIRED` | nonce منقضی/مصرف‌شده یا عدم تطبیق مالک/purpose/کلید/hash دستگاه |
| 409 | `DEVICE_ALREADY_HAS_SOFTWARE_LICENSE` | دستگاه از قبل لایسنس active دیگری برای همان نرم‌افزار دارد |
| 409 | `LICENSE_ALREADY_ACTIVATED` | این لایسنس فعال‌سازی active دارد؛ با همان دستگاه به verify بروید، با کلید تازه انتظار انتقال خودکار نداشته باشید |
| 500 | `LICENSE_ACTIVATION_FAILED` | خطای غیرنگاشت‌شده، مشکل JSON/DB/parse رمزنگاری و موارد داخلی؛ موفقیت فرض نشود |

ترتیب اعتبارسنجی مهم است: مثلاً کلید یا fingerprint متفاوت در complete اغلب ابتدا با عدم تطبیق challenge به **409** می‌رسد، نه الزاماً 400. خطای Base64 معتبر ولی DER خراب ممکن است 500 باشد. جدول فهرست نگاشت کد است، نه وعده یک خطای ثابت برای هر ورودی خراب.

`verify/begin` و `verify/complete` از `status`های HTTP 200 برای اعلام revoked/expired/device_invalid هم استفاده می‌کنند. HTTP 200 به‌تنهایی مجوز استفاده از نرم‌افزار نیست. `suspended` در تابع `currentLicenseState` به `revoked` نگاشت می‌شود، هرچند فهرست لایسنس می‌تواند وضعیت `suspended` را نشان دهد.

### ۱۳.۲ login و فهرست

| مسیر | HTTP و body واقعی |
| --- | --- |
| login، شماره نامعتبر | 400 و `{"error":"شماره موبایل معتبر نیست."}` |
| login، رمز خالی | 400 و `{"error":"رمز عبور را وارد کن."}` |
| login، حساب/رمز نادرست | 401 و `{"error":"شماره موبایل یا رمز عبور اشتباه است."}` |
| login، proof امنیتی نامعتبر | 403 و `{"success":false,"error":"لطفاً تأیید امنیتی را دوباره انجام دهید."}` |
| login، Turnstile تنظیم‌نشده یا در دسترس نیست | 503 و `{"success":false,"error":"سرویس امنیتی موقتاً در دسترس نیست."}` |
| licenses، Bearer نامعتبر | 401 و `{"success":false,"error":"CUSTOMER_AUTH_REQUIRED"}` |
| licenses، خواندن/رمزگشایی ناموفق | 503 و `{"success":false,"error":"LICENSE_LIST_UNAVAILABLE"}` |

متن فارسی login، کد ماشینی پایدار انگلیسی نیست؛ بر اساس HTTP و مرحله UI مدیریت شود. خطاهای خارج از catch در customer route در `main.js` می‌توانند HTTP 500 با `{"success":false,"message":"Customer request failed"}` بدهند. maintenance mode نیز HTTP 503 با `message` فارسی می‌دهد. کلاینت باید غیبت `error` و پاسخ غیر JSON پروکسی/شبکه را تحمل کند.

Method اشتباه برای چهار مسیر فعال‌سازی در dispatcher فعلی به پاسخ **404** با `error` فارسی «مسیر پیدا نشد.» می‌رسد؛ قرارداد 405 استانداردی در این routeها پیاده نشده است.

### ۱۳.۳ اندازه بدنه

`readBody` روی `data.length > 16384` درخواست را destroy می‌کند؛ این کنترل طول رشته جاوااسکریپت است، نه یک قرارداد تضمین‌شده HTTP 413. JSON خراب داخل try فعال‌سازی به خطای عمومی 500 می‌رود و بدنه بسیار بزرگ ممکن است فقط خطای شبکه ایجاد کند. JSON کوچک و معتبر، بدون داده اضافی/attestation/گواهی کامل فرستاده شود.

## ۱۴. نمونه Kotlin برای بخش رمزنگاری

این نمونه **پیشنهاد پیاده‌سازی سمت Android** بر پایه قرارداد استخراج‌شده است؛ فایل اجرایی سرور یا endpoint جدید نیست. روی گوشی اجرا/کامپایل نشده است. نمونه AndroidKeyStore به API 23+ نیاز دارد. اگر پروژه YADRA کلید نصب موجود دارد، alias آن بررسی و کلید همان نصب دوباره استفاده شود؛ تغییر alias/پاک‌کردن کلید پس از فعال‌سازی، اتصال دستگاه را می‌شکند.

```kotlin
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

fun getOrCreateInstallKey(alias: String): KeyPair {
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    if (store.containsAlias(alias)) {
        val privateKey = store.getKey(alias, null) as PrivateKey
        return KeyPair(store.getCertificate(alias).publicKey, privateKey)
    }
    val generator = KeyPairGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore"
    )
    generator.initialize(
        KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()
    )
    return generator.generateKeyPair()
}

fun sha256Hex(bytes: ByteArray): String {
    val alphabet = "0123456789abcdef"
    return buildString {
        for (byte in MessageDigest.getInstance("SHA-256").digest(bytes)) {
            val value = byte.toInt() and 0xff
            append(alphabet[value ushr 4])
            append(alphabet[value and 15])
        }
    }
}

data class DeviceProofFields(
    val publicKeyBase64: String,
    val fingerprint: String,
    val androidIdHash: String
)

fun deviceFields(keyPair: KeyPair, stableDeviceHash: String): DeviceProofFields {
    require(Regex("^[a-f0-9]{64}$").matches(stableDeviceHash))
    val spkiDer = keyPair.public.encoded
    return DeviceProofFields(
        Base64.encodeToString(spkiDer, Base64.NO_WRAP),
        sha256Hex(spkiDer),
        stableDeviceHash
    )
}

fun canonicalPayload(
    verify: Boolean,
    challengeId: String,
    challenge: String,
    licenseId: String,
    softwareCode: String,
    device: DeviceProofFields
): ByteArray {
    require(Regex("^[1-9]\\d{0,18}$").matches(challengeId))
    require(Regex("^[1-9]\\d{0,18}$").matches(licenseId))
    require(Regex("^[A-Za-z0-9_-]{43}$").matches(challenge))
    require(Regex("^[a-z0-9][a-z0-9_-]{0,63}$").matches(softwareCode))
    return listOf(
        if (verify) "YADRA-LICENSE-VERIFY" else "YADRA-LICENSE-ACTIVATE",
        "v1", challengeId, challenge, licenseId, softwareCode,
        device.androidIdHash, device.fingerprint
    ).joinToString("\n").toByteArray(Charsets.UTF_8)
}

fun signPayload(keyPair: KeyPair, payloadUtf8: ByteArray): String {
    val signer = Signature.getInstance("SHA256withECDSA")
    signer.initSign(keyPair.private)
    signer.update(payloadUtf8)
    return Base64.encodeToString(signer.sign(), Base64.NO_WRAP)
}
```

مرجع API کلید و الگوریتم: [Android KeyGenParameterSpec](https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec). کلید خصوصی export یا به سرور ارسال نمی‌شود؛ فقط public SPKI ارسال می‌شود. تولید کلید تازه در هر begin یا hash کردن متن publicKeyBase64 به جای DER از خطاهای رایج ناسازگاری است.

## ۱۵. ترتیب دقیق پیاده‌سازی و اولین آزمون روی گوشی

1. اپ را با شبکه/TLS عادی به origin تنظیم‌شده متصل کنید؛ TLS verification خاموش نشود.
2. جریان ورود حساب مالک سفارش ۳ و Turnstile را کامل کنید؛ هیچ credential را در کد/لاگ hardcode نکنید.
3. با Bearer، GET فهرست؛ رکورد `"2" / "diag"` را تأیید و `code` را فقط در حافظه لازم دریافت کنید. لاگ کامل body این endpoint ممنوع باشد.
4. یک کلید نصب پایدار EC P-256 از AndroidKeyStore و یک hash ثابت دستگاه تهیه کنید؛ DER، Base64 و fingerprint را بسازید.
5. activation/begin را با code همان رکورد بفرستید؛ سه فیلد پاسخ را بخوانید، بدون انتظار `success` یا `data`.
6. با شناسه **لایسنس از فهرست** و challenge از begin، payload ACTIVATE را بسازید؛ SHA256withECDSA و Base64 استاندارد.
7. activation/complete با همان Bearer/کلید/hashها/nonce؛ فقط پاسخ موفق دقیق آن مرحله را ثبت کنید.
8. برای اطمینان از اتصال دستگاه فعلی، verify/begin اجرا کنید؛ فقط اگر `status == "needs_online"` بود با payload VERIFY ادامه دهید و در verify/complete انتظار `status == "active"` داشته باشید.
9. پس از پایان، جزئیات challenge و code غیرضروری را از حافظه موقت حذف کنید. نگه‌داری Bearer/شناسه‌ها باید در storage مناسب اپ باشد؛ کلید private داخل KeyStore بماند. فهرست/کد/امضاها وارد logcat، crash report یا Analytics نشوند.

HTTP را روی thread شبکه/IO انجام دهید. از همان JSON serializer و HTTP stack موجود پروژه اندروید استفاده کنید؛ افزودن SDK/کتابخانه یا نسخه جدید برای اجرای این قرارداد الزامی نیست. مدل پاسخ هر مرحله جداست؛ `status` license و `status` verification را با status HTTP اشتباه نگیرید.

## ۱۶. خطا، timeout و بازیابی بدون فعال‌سازی تکراری

- **401:** ورود مجدد حساب؛ سپس مرحله لازم با توکن تازه. challenge به حساب متصل است، نه به یک رشته JWT خاص، اما انقضای پنج‌دقیقه‌ای همچنان برقرار است.
- **409 challenge:** از begin مربوط به همان purpose، challenge تازه بگیرید؛ challenge مصرف‌شده دوباره امضا نشود.
- **403 signature:** پیش از retry، شناسه لایسنس، prefix، LF، DER، Base64 و کلید ثابت بررسی شوند؛ payload دلخواه یا hash دوباره نفرستید.
- **timeout پس از ارسال activation/complete:** نتیجه نامعلوم است؛ ممکن است سرور Commit کرده باشد. ابتدا با **همان** key/device/id از verify/begin و verify/complete وضعیت دستگاه را تعیین کنید. پاسخ list با `activated: true` به‌تنهایی هویت دستگاه را ثابت نمی‌کند.
- **LICENSE_ALREADY_ACTIVATED:** با کلید و hash قبلی verify کنید؛ اگر نصب/کلید عوض شده، سرور مسیر انتقال خودکار دستگاه در این چهار endpoint ندارد. درخواست انتقال/لغو باید از روند مجاز مدیریت انجام شود.
- **revoked/expired/device_invalid:** دسترسی موفق فرض نشود؛ برای `device_invalid` در نصب اول هنوز activate لازم است، اما روی نصب قبلاً فعال‌شده باید وضعیت حساب/کلید/دستگاه بررسی شود.
- بازه مجاز استفاده آفلاین، TTL مجوز آفلاین یا server-signed license file در این پاسخ‌ها تعریف نشده است. ۲۰۰ active آنلاین را به مجوز دائمی آفلاین تبدیل نکنید؛ سیاست محصول جداگانه لازم است.

## ۱۷. نقص‌ها و محدودیت‌های باقی‌مانده سرور — بدون تغییر کد

| موضوع | وضعیت واقعی و اثر | پیشنهاد صرفاً برای تأیید بعدی |
| --- | --- | --- |
| نبود licenseId در activation/begin | نقص استقلال handshake؛ کلاینتی که فقط کد را دارد، از begin به‌تنهایی قادر به ساخت امضا نیست | پاسخ افزودنی `licenseId` و ترجیحاً `softwareCode`؛ فعلاً GET فهرست راه موجود است |
| نبود orderId در فهرست مشتری | انتخاب بر اساس شماره سفارش از این endpoint ممکن نیست | در صورت نیاز افزودن ارتباط سفارش؛ برای مورد فعلی فقط نگاشت خواندنی ۳ → لایسنس ۲ تأیید شده است |
| ورود native و Turnstile | endpoint مخصوص اندروید/تبادل توکن آماده وجود ندارد؛ login خام بدون proof رد می‌شود | پیاده‌سازی صحیح جریان وب/تحویل توکن در Android یا طراحی جداگانه احراز هویت، فقط با تأیید |
| پاسخ‌های نامتقارن | begin/verifyها envelope واحد ندارند و وضعیت نامعتبر دستگاه گاهی HTTP 200 است | DTO مجزا در اپ؛ یکسان‌سازی سرور تغییر قراردادی جداگانه است |
| JSON/DER خراب و محدودیت بدنه | برخی خطاهای ورودی به 500 یا قطع اتصال می‌روند؛ تضمین 400/413 نیست | بهبود نگاشت ورودی‌ها بدون افشای خطای داخلی، با تأیید |
| suspended در verify | به revoked تبدیل می‌شود | در صورت نیاز تفکیک وضعیت محصول؛ فعلاً رفتار همین است |
| verify/complete | softwareCode را مانند begin با regex صریح چک نمی‌کند؛ هویت challenge و payload مبنای نتیجه است | سخت‌گیری و یکسان‌سازی اعتبارسنجی در تغییر بعدی |
| دامنه شروط پرداخت | begin اولیه پرداخت/سفارش/محصول را join می‌کند؛ complete و revalidation وضعیت/مالک/دستگاه لایسنس را می‌خوانند و همان join پرداخت را دوباره اجرا نمی‌کنند | اگر انتظار محصول، ابطال خودکار با تغییر پرداخت است، باید جداگانه طراحی/تأیید شود؛ قرارداد فعلی را گسترده‌تر فرض نکنید |
| الگوریتم androidIdHash | سرور فقط قالب را مشخص کرده، سیاست استخراج Android را نه | سیاست ثابت در پروژه Android تعیین و مستند شود؛ تغییر بعد از bind مشکل ایجاد می‌کند |
| retry complete | idempotent response طراحی نشده؛ تکرار ممکن است 409 بدهد | در اپ از verify برای تشخیص نتیجه نامعلوم استفاده شود |
| بررسی واقعی APK | هنوز انجام نشده؛ همسانی فایل‌ها با پردازش حافظه یا موفقیت روی گوشی یکسان نیست | آزمون کنترل‌شده با اجازه اجرا، بدون ایجاد پرداخت/لایسنس تازه |

هیچ‌کدام از پیشنهادهای ستون آخر در این کار روی سرور اعمال نشده‌اند. نبود licenseId در begin با مسیر موجود فهرست قابل دورزدن است؛ برای اولین آزمون لازم نیست صرفاً به این دلیل سرور تغییر کند.

## ۱۸. چک‌لیست انتقال به Codex اندروید

- این فایل را به پروژه Android منتقل کنید؛ سورس سایت برای فهم wire contract لازم نیست.
- Base URL تنظیم‌شده: `https://yadran.ir`؛ softwareCode این سفارش: `diag`؛ شناسه این لایسنس: `"2"`؛ شماره سفارش: `3`.
- از حساب مالک و GET فهرست، کد را در زمان اجرا دریافت کنید؛ هیچ مقدار محرمانه‌ای در این فایل وجود ندارد.
- قبل از درخواست واقعی، تطابق byte-for-byte payload، SPKI fingerprint و SHA256withECDSA/DER را در تست محلی Android بررسی کنید.
- بخش ورود/Turnstile و سیاست hash دستگاه تصمیم‌های لازم سمت اپ هستند؛ endpoint یا فیلد اضافه‌ای حدس نزنید.
- هیچ پرداخت یا لایسنس جدیدی برای این آزمون نسازید. begin خودش challenge را در دیتابیس می‌نویسد و complete دستگاه را bind می‌کند؛ اجرای واقعی این مراحل باید آگاهانه و با مجوز جداگانه باشد.
- پس از تست گوشی، فقط نتیجه، HTTP/status/error و metadata لازم گزارش شود؛ توکن، رمز، private key و کد کامل لایسنس منتشر نشوند.
