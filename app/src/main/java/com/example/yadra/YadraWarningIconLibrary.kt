package com.example.yadra

/**
 * کتابخانه مستقل نمادهای هشدار و وضعیت داشبورد YADRA
 *
 * این فایل هیچ ارتباطی با:
 * Wi-Fi
 * Bluetooth
 * ECU Connection
 * YadraConnectionManager
 *
 * ندارد.
 */
object YadraWarningIconLibrary {

    private val warnings =
        listOf(

            // =========================================================
            // ENGINE
            // =========================================================

            YadraWarningIcon(
                id = "ENGINE_OIL_PRESSURE",
                titleFa = "فشار روغن موتور",
                titleEn = "Engine Oil Pressure",
                category = YadraWarningCategory.ENGINE,
                severity = YadraWarningSeverity.CRITICAL,
                color = YadraWarningColor.RED,
                iconName = "ic_warning_oil",
                descriptionFa = "کاهش یا از دست رفتن فشار روغن موتور.",
                possibleCausesFa = listOf(
                    "کمبود روغن موتور",
                    "خرابی پمپ روغن",
                    "نشتی روغن",
                    "خرابی سنسور فشار روغن"
                ),
                recommendedActionFa =
                    "در صورت روشن ماندن چراغ، خودرو را متوقف کرده و سطح روغن را بررسی کنید."
            ),

            YadraWarningIcon(
                id = "ENGINE_OVERHEAT",
                titleFa = "دمای بالای موتور",
                titleEn = "Engine Overheat",
                category = YadraWarningCategory.ENGINE,
                severity = YadraWarningSeverity.CRITICAL,
                color = YadraWarningColor.RED,
                iconName = "ic_warning_temperature",
                descriptionFa = "دمای مایع خنک‌کننده یا موتور بیش از حد مجاز است.",
                possibleCausesFa = listOf(
                    "کمبود مایع خنک‌کننده",
                    "خرابی فن",
                    "خرابی ترموستات",
                    "نشتی سیستم خنک‌کننده"
                ),
                recommendedActionFa =
                    "خودرو را متوقف کنید و اجازه دهید موتور خنک شود."
            ),

            YadraWarningIcon(
                id = "CHECK_ENGINE",
                titleFa = "بررسی موتور",
                titleEn = "Check Engine",
                category = YadraWarningCategory.ENGINE,
                severity = YadraWarningSeverity.WARNING,
                color = YadraWarningColor.AMBER,
                iconName = "ic_warning_check_engine",
                descriptionFa = "سیستم مدیریت موتور خطایی را شناسایی کرده است.",
                possibleCausesFa = listOf(
                    "خطای سنسورها",
                    "مشکل احتراق",
                    "مشکل سیستم سوخت",
                    "خطای ECU"
                ),
                recommendedActionFa =
                    "کد خطا را با دستگاه دیاگ بررسی کنید."
            ),

            YadraWarningIcon(
                id = "GLOW_PLUG",
                titleFa = "شمع گرم‌کن دیزل",
                titleEn = "Glow Plug",
                category = YadraWarningCategory.ENGINE,
                severity = YadraWarningSeverity.INFORMATION,
                color = YadraWarningColor.AMBER,
                iconName = "ic_warning_glow_plug",
                descriptionFa = "سیستم گرم‌کن شمع‌های موتور دیزل فعال است.",
                possibleCausesFa = listOf(
                    "گرم شدن اولیه موتور",
                    "خرابی شمع گرم‌کن",
                    "مشکل رله گرم‌کن"
                ),
                recommendedActionFa =
                    "قبل از استارت منتظر خاموش شدن چراغ باشید."
            ),

            YadraWarningIcon(
                id = "ENGINE_SERVICE",
                titleFa = "سرویس موتور",
                titleEn = "Engine Service",
                category = YadraWarningCategory.ENGINE,
                severity = YadraWarningSeverity.WARNING,
                color = YadraWarningColor.AMBER,
                iconName = "ic_warning_engine_service",
                descriptionFa = "ممکن است موتور نیاز به سرویس داشته باشد.",
                possibleCausesFa = listOf(
                    "رسیدن زمان سرویس",
                    "ثبت خطای موتور",
                    "نیاز به بررسی سیستم احتراق"
                ),
                recommendedActionFa =
                    "سیستم موتور را با دیاگ بررسی کنید."
            ),

            // =========================================================
            // BRAKE & SAFETY
            // =========================================================

            YadraWarningIcon(
                id = "BRAKE_SYSTEM",
                titleFa = "سیستم ترمز",
                titleEn = "Brake System",
                category = YadraWarningCategory.BRAKE_SAFETY,
                severity = YadraWarningSeverity.CRITICAL,
                color = YadraWarningColor.RED,
                iconName = "ic_warning_brake",
                descriptionFa = "در سیستم ترمز خودرو خطا یا وضعیت غیرعادی وجود دارد.",
                possibleCausesFa = listOf(
                    "کمبود روغن ترمز",
                    "خرابی سیستم ترمز",
                    "فرسودگی لنت",
                    "خرابی سنسور"
                ),
                recommendedActionFa =
                    "با احتیاط خودرو را متوقف کرده و سیستم ترمز را بررسی کنید."
            ),

            YadraWarningIcon(
                id = "PARKING_BRAKE",
                titleFa = "ترمز دستی",
                titleEn = "Parking Brake",
                category = YadraWarningCategory.BRAKE_SAFETY,
                severity = YadraWarningSeverity.STATUS,
                color = YadraWarningColor.RED,
                iconName = "ic_warning_parking_brake",
                descriptionFa = "ترمز دستی فعال است.",
                possibleCausesFa = listOf(
                    "فعال بودن ترمز دستی"
                ),
                recommendedActionFa =
                    "قبل از حرکت ترمز دستی را آزاد کنید."
            ),

            YadraWarningIcon(
                id = "ABS",
                titleFa = "ABS",
                titleEn = "ABS Warning",
                category = YadraWarningCategory.BRAKE_SAFETY,
                severity = YadraWarningSeverity.WARNING,
                color = YadraWarningColor.AMBER,
                iconName = "ic_warning_abs",
                descriptionFa = "سیستم ضدقفل ترمز دارای خطا است.",
                possibleCausesFa = listOf(
                    "خرابی سنسور چرخ",
                    "خرابی یونیت ABS",
                    "مشکل سیم‌کشی"
                ),
                recommendedActionFa =
                    "سیستم ABS را با دیاگ بررسی کنید."
            ),

            YadraWarningIcon(
                id = "BRAKE_PAD",
                titleFa = "لنت ترمز",
                titleEn = "Brake Pad",
                category = YadraWarningCategory.BRAKE_SAFETY,
                severity = YadraWarningSeverity.WARNING,
                color = YadraWarningColor.AMBER,
                iconName = "ic_warning_brake_pad",
                descriptionFa = "ضخامت لنت ترمز ممکن است کم باشد.",
                possibleCausesFa = listOf(
                    "فرسودگی لنت"
                ),
                recommendedActionFa =
                    "لنت‌های ترمز را بررسی و در صورت نیاز تعویض کنید."
            ),

            YadraWarningIcon(
                id = "AIRBAG",
                titleFa = "ایربگ",
                titleEn = "Airbag",
                category = YadraWarningCategory.BRAKE_SAFETY,
                severity = YadraWarningSeverity.CRITICAL,
                color = YadraWarningColor.RED,
                iconName = "ic_warning_airbag",
                descriptionFa = "سیستم ایربگ خطا دارد.",
                possibleCausesFa = listOf(
                    "خرابی سنسور",
                    "مشکل سیم‌کشی",
                    "خرابی یونیت ایربگ"
                ),
                recommendedActionFa =
                    "سیستم SRS و ایربگ را توسط تعمیرکار متخصص بررسی کنید."
            ),

            YadraWarningIcon(
                id = "SEAT_BELT",
                titleFa = "کمربند ایمنی",
                titleEn = "Seat Belt",
                category = YadraWarningCategory.BRAKE_SAFETY,
                severity = YadraWarningSeverity.WARNING,
                color = YadraWarningColor.RED,
                iconName = "ic_warning_seatbelt",
                descriptionFa = "کمربند ایمنی یکی از سرنشینان بسته نشده است.",
                possibleCausesFa = listOf(
                    "بسته نبودن کمربند",
                    "تشخیص وزن روی صندلی"
                ),
                recommendedActionFa =
                    "کمربند ایمنی را ببندید."
            ),

            // =========================================================
            // STABILITY & TRACTION
            // =========================================================

            YadraWarningIcon(
                id = "ESP",
                titleFa = "کنترل پایداری",
                titleEn = "Electronic Stability Program",
                category = YadraWarningCategory.STABILITY_TRACTION,
                severity = YadraWarningSeverity.WARNING,
                color = YadraWarningColor.AMBER,
                iconName = "ic_warning_esp",
                descriptionFa = "سیستم کنترل پایداری فعال یا دارای خطا است.",
                possibleCausesFa = listOf(
                    "لغزش خودرو",
                    "خرابی سنسور",
                    "خطای سیستم ESP"
                ),
                recommendedActionFa =
                    "در صورت روشن ماندن چراغ، سیستم ESP را بررسی کنید."
            ),

            YadraWarningIcon(
                id = "TRACTION_CONTROL",
                titleFa = "کنترل کشش",
                titleEn = "Traction Control",
                category = YadraWarningCategory.STABILITY_TRACTION,
                severity = YadraWarningSeverity.INFORMATION,
                color = YadraWarningColor.AMBER,
                iconName = "ic_warning_traction",
                descriptionFa = "سیستم کنترل کشش در حال مداخله است.",
                possibleCausesFa = listOf(
                    "لغزش چرخ‌ها",
                    "سطح لغزنده"
                ),
                recommendedActionFa =
                    "سرعت را متناسب با شرایط جاده تنظیم کنید."
            ),

            YadraWarningIcon(
                id = "TIRE_PRESSURE",
                titleFa = "فشار باد تایر",
                titleEn = "Tire Pressure",
                category = YadraWarningCategory.STABILITY_TRACTION,
                severity = YadraWarningSeverity.WARNING,
                color = YadraWarningColor.AMBER,
                iconName = "ic_warning_tire_pressure",
                descriptionFa = "فشار یکی از تایرها پایین یا غیرعادی است.",
                possibleCausesFa = listOf(
                    "کمبود باد",
                    "پنچری",
                    "خرابی سنسور TPMS"
                ),
                recommendedActionFa =
                    "فشار تمام تایرها را بررسی کنید."
            ),

            // =========================================================
            // TRANSMISSION
            // =========================================================

            YadraWarningIcon(
                id = "TRANSMISSION",
                titleFa = "گیربکس",
                titleEn = "Transmission",
                category = YadraWarningCategory.TRANSMISSION,
                severity = YadraWarningSeverity.WARNING,
                color = YadraWarningColor.AMBER,
                iconName = "ic_warning_transmission",
                descriptionFa = "سیستم انتقال قدرت خطا دارد.",
                possibleCausesFa = listOf(
                    "دمای بالای گیربکس",
                    "کمبود روغن",
                    "خطای سنسور",
                    "خطای TCU"
                ),
                recommendedActionFa =
                    "سطح روغن و خطاهای گیربکس را بررسی کنید."
            ),

            YadraWarningIcon(
                id = "TRANSMISSION_OVERHEAT",
                titleFa = "دمای بالای گیربکس",
                titleEn = "Transmission Overheat",
                category = YadraWarningCategory.TRANSMISSION,
                severity = YadraWarningSeverity.CRITICAL,
                color = YadraWarningColor.RED,
                iconName = "ic_warning_transmission_temp",
                descriptionFa = "دمای گیربکس بیش از حد مجاز است.",
                possibleCausesFa = listOf(
                    "بار زیاد",
                    "کمبود روغن",
                    "خرابی خنک‌کننده گیربکس"
                ),
                recommendedActionFa =
                    "خودرو را متوقف کرده و اجازه دهید گیربکس خنک شود."
            ),

            // =========================================================
            // FUEL & FLUIDS
            // =========================================================

            YadraWarningIcon(
                id = "LOW_FUEL",
                titleFa = "سوخت کم",
                titleEn = "Low Fuel",
                category = YadraWarningCategory.FUEL_FLUIDS,
                severity = YadraWarningSeverity.WARNING,
                color = YadraWarningColor.AMBER,
                iconName = "ic_warning_fuel",
                descriptionFa = "سطح سوخت پایین است.",
                possibleCausesFa = listOf(
                    "کم بودن سوخت"
                ),
                recommendedActionFa =
                    "در اولین فرصت سوخت‌گیری کنید."
            ),

            YadraWarningIcon(
                id = "FUEL_CAP",
                titleFa = "درب باک",
                titleEn = "Fuel Cap",
                category = YadraWarningCategory.FUEL_FLUIDS,
                severity = YadraWarningSeverity.WARNING,
                color = YadraWarningColor.AMBER,
                iconName = "ic_warning_fuel_cap",
                descriptionFa = "درب باک ممکن است به‌درستی بسته نشده باشد.",
                possibleCausesFa = listOf(
                    "باز بودن درب باک",
                    "خرابی درب باک"
                ),
                recommendedActionFa =
                    "درب باک را بررسی و کاملاً بسته کنید."
            ),

            YadraWarningIcon(
                id = "LOW_COOLANT",
                titleFa = "سطح مایع خنک‌کننده",
                titleEn = "Low Coolant",
                category = YadraWarningCategory.FUEL_FLUIDS,
                severity = YadraWarningSeverity.WARNING,
                color = YadraWarningColor.RED,
                iconName = "ic_warning_coolant",
                descriptionFa = "سطح مایع خنک‌کننده پایین است.",
                possibleCausesFa = listOf(
                    "نشتی",
                    "کمبود مایع",
                    "خرابی سیستم خنک‌کننده"
                ),
                recommendedActionFa =
                    "پس از خنک شدن موتور سطح مایع را بررسی کنید."
            ),

            YadraWarningIcon(
                id = "WINDSHIELD_WASHER",
                titleFa = "مایع شیشه‌شور",
                titleEn = "Windshield Washer",
                category = YadraWarningCategory.FUEL_FLUIDS,
                severity = YadraWarningSeverity.INFORMATION,
                color = YadraWarningColor.AMBER,
                iconName = "ic_warning_washer",
                descriptionFa = "مایع شیشه‌شور کم است.",
                possibleCausesFa = listOf(
                    "کمبود مایع شیشه‌شور"
                ),
                recommendedActionFa =
                    "مخزن شیشه‌شور را پر کنید."
            ),

            // =========================================================
            // LIGHTING
            // =========================================================

            YadraWarningIcon(
                id = "HIGH_BEAM",
                titleFa = "نور بالا",
                titleEn = "High Beam",
                category = YadraWarningCategory.LIGHTING,
                severity = YadraWarningSeverity.STATUS,
                color = YadraWarningColor.BLUE,
                iconName = "ic_warning_high_beam",
                descriptionFa = "نور بالای چراغ‌های جلو فعال است.",
                possibleCausesFa = listOf(
                    "فعال بودن نور بالا"
                ),
                recommendedActionFa =
                    "در صورت نزدیک شدن خودرو مقابل، نور بالا را خاموش کنید."
            ),

            YadraWarningIcon(
                id = "LOW_BEAM",
                titleFa = "نور پایین",
                titleEn = "Low Beam",
                category = YadraWarningCategory.LIGHTING,
                severity = YadraWarningSeverity.STATUS,
                color = YadraWarningColor.GREEN,
                iconName = "ic_warning_low_beam",
                descriptionFa = "چراغ نور پایین روشن است.",
                possibleCausesFa = listOf(
                    "فعال بودن چراغ نور پایین"
                ),
                recommendedActionFa =
                    "وضعیت چراغ‌ها را متناسب با شرایط جاده تنظیم کنید."
            ),

            YadraWarningIcon(
                id = "FOG_LIGHT",
                titleFa = "چراغ مه‌شکن",
                titleEn = "Fog Light",
                category = YadraWarningCategory.LIGHTING,
                severity = YadraWarningSeverity.STATUS,
                color = YadraWarningColor.GREEN,
                iconName = "ic_warning_fog",
                descriptionFa = "چراغ مه‌شکن فعال است.",
                possibleCausesFa = listOf(
                    "فعال بودن مه‌شکن"
                ),
                recommendedActionFa =
                    "در شرایط عادی در صورت عدم نیاز مه‌شکن را خاموش کنید."
            ),

            YadraWarningIcon(
                id = "LIGHT_FAILURE",
                titleFa = "خرابی چراغ",
                titleEn = "Light Failure",
                category = YadraWarningCategory.LIGHTING,
                severity = YadraWarningSeverity.WARNING,
                color = YadraWarningColor.AMBER,
                iconName = "ic_warning_light_failure",
                descriptionFa = "یکی از چراغ‌های خودرو ممکن است خراب باشد.",
                possibleCausesFa = listOf(
                    "سوختن لامپ",
                    "مشکل سیم‌کشی",
                    "خرابی فیوز"
                ),
                recommendedActionFa =
                    "چراغ‌های خودرو را بررسی کنید."
            ),

            // =========================================================
            // DRIVER ASSIST
            // =========================================================

            YadraWarningIcon(
                id = "CRUISE_CONTROL",
                titleFa = "کروز کنترل",
                titleEn = "Cruise Control",
                category = YadraWarningCategory.DRIVER_ASSIST,
                severity = YadraWarningSeverity.STATUS,
                color = YadraWarningColor.GREEN,
                iconName = "ic_warning_cruise",
                descriptionFa = "کروز کنترل فعال است.",
                possibleCausesFa = listOf(
                    "فعال بودن Cruise Control"
                ),
                recommendedActionFa =
                    "برای کنترل دستی سرعت، کروز کنترل را غیرفعال کنید."
            ),

            YadraWarningIcon(
                id = "ADAPTIVE_CRUISE",
                titleFa = "کروز کنترل تطبیقی",
                titleEn = "Adaptive Cruise Control",
                category = YadraWarningCategory.DRIVER_ASSIST,
                severity = YadraWarningSeverity.STATUS,
                color = YadraWarningColor.GREEN,
                iconName = "ic_warning_adaptive_cruise",
                descriptionFa = "سیستم کروز کنترل تطبیقی فعال است.",
                possibleCausesFa = listOf(
                    "فعال بودن ACC"
                ),
                recommendedActionFa =
                    "فاصله ایمن با خودروهای جلو را حفظ کنید."
            ),

            YadraWarningIcon(
                id = "LANE_DEPARTURE",
                titleFa = "هشدار خروج از خط",
                titleEn = "Lane Departure Warning",
                category = YadraWarningCategory.DRIVER_ASSIST,
                severity = YadraWarningSeverity.WARNING,
                color = YadraWarningColor.AMBER,
                iconName = "ic_warning_lane",
                descriptionFa = "سیستم خروج خودرو از مسیر را تشخیص داده است.",
                possibleCausesFa = listOf(
                    "خروج از مسیر",
                    "عدم تشخیص خطوط جاده",
                    "کثیف بودن دوربین"
                ),
                recommendedActionFa =
                    "مسیر خودرو را اصلاح کنید."
            ),

            YadraWarningIcon(
                id = "BLIND_SPOT",
                titleFa = "نقطه کور",
                titleEn = "Blind Spot",
                category = YadraWarningCategory.DRIVER_ASSIST,
                severity = YadraWarningSeverity.WARNING,
                color = YadraWarningColor.AMBER,
                iconName = "ic_warning_blind_spot",
                descriptionFa = "خودرویی در نقطه کور شناسایی شده است.",
                possibleCausesFa = listOf(
                    "وجود خودرو در نقطه کور"
                ),
                recommendedActionFa =
                    "قبل از تغییر مسیر آینه‌ها را بررسی کنید."
            ),

            YadraWarningIcon(
                id = "FORWARD_COLLISION",
                titleFa = "خطر برخورد جلو",
                titleEn = "Forward Collision Warning",
                category = YadraWarningCategory.DRIVER_ASSIST,
                severity = YadraWarningSeverity.CRITICAL,
                color = YadraWarningColor.RED,
                iconName = "ic_warning_collision",
                descriptionFa = "احتمال برخورد با مانع یا خودرو تشخیص داده شده است.",
                possibleCausesFa = listOf(
                    "نزدیک شدن سریع به مانع",
                    "عدم رعایت فاصله"
                ),
                recommendedActionFa =
                    "فوراً سرعت را کاهش داده و فاصله ایمن ایجاد کنید."
            ),

            // =========================================================
            // ACCESS & BODY
            // =========================================================

            YadraWarningIcon(
                id = "DOOR_OPEN",
                titleFa = "درب باز",
                titleEn = "Door Open",
                category = YadraWarningCategory.ACCESS_BODY,
                severity = YadraWarningSeverity.WARNING,
                color = YadraWarningColor.RED,
                iconName = "ic_warning_door",
                descriptionFa = "یکی از درب‌های خودرو باز است.",
                possibleCausesFa = listOf(
                    "باز بودن درب",
                    "خرابی سنسور درب"
                ),
                recommendedActionFa =
                    "تمام درب‌ها را به‌درستی ببندید."
            ),

            YadraWarningIcon(
                id = "HOOD_OPEN",
                titleFa = "کاپوت باز",
                titleEn = "Hood Open",
                category = YadraWarningCategory.ACCESS_BODY,
                severity = YadraWarningSeverity.WARNING,
                color = YadraWarningColor.RED,
                iconName = "ic_warning_hood",
                descriptionFa = "کاپوت خودرو باز است.",
                possibleCausesFa = listOf(
                    "باز بودن کاپوت",
                    "خرابی سنسور"
                ),
                recommendedActionFa =
                    "کاپوت را کاملاً ببندید."
            ),

            YadraWarningIcon(
                id = "TRUNK_OPEN",
                titleFa = "صندوق باز",
                titleEn = "Trunk Open",
                category = YadraWarningCategory.ACCESS_BODY,
                severity = YadraWarningSeverity.WARNING,
                color = YadraWarningColor.RED,
                iconName = "ic_warning_trunk",
                descriptionFa = "صندوق عقب باز است.",
                possibleCausesFa = listOf(
                    "باز بودن صندوق",
                    "خرابی سنسور"
                ),
                recommendedActionFa =
                    "صندوق عقب را ببندید."
            ),

            // =========================================================
            // STATUS
            // =========================================================

            YadraWarningIcon(
                id = "PARKING_ASSIST",
                titleFa = "کمک پارک",
                titleEn = "Parking Assist",
                category = YadraWarningCategory.COMFORT_STATUS,
                severity = YadraWarningSeverity.STATUS,
                color = YadraWarningColor.GREEN,
                iconName = "ic_warning_parking_assist",
                descriptionFa = "سیستم کمک پارک فعال است.",
                possibleCausesFa = listOf(
                    "فعال بودن Parking Assist"
                ),
                recommendedActionFa =
                    "هنگام پارک از سنسورها و دوربین استفاده کنید."
            ),

            YadraWarningIcon(
                id = "ECO_MODE",
                titleFa = "حالت اقتصادی",
                titleEn = "Eco Mode",
                category = YadraWarningCategory.COMFORT_STATUS,
                severity = YadraWarningSeverity.STATUS,
                color = YadraWarningColor.GREEN,
                iconName = "ic_warning_eco",
                descriptionFa = "حالت رانندگی اقتصادی فعال است.",
                possibleCausesFa = listOf(
                    "فعال بودن Eco Mode"
                ),
                recommendedActionFa =
                    "این حالت برای کاهش مصرف سوخت طراحی شده است."
            ),

            YadraWarningIcon(
                id = "SPORT_MODE",
                titleFa = "حالت اسپرت",
                titleEn = "Sport Mode",
                category = YadraWarningCategory.COMFORT_STATUS,
                severity = YadraWarningSeverity.STATUS,
                color = YadraWarningColor.RED,
                iconName = "ic_warning_sport",
                descriptionFa = "حالت رانندگی اسپرت فعال است.",
                possibleCausesFa = listOf(
                    "فعال بودن Sport Mode"
                ),
                recommendedActionFa =
                    "در این حالت واکنش موتور و گیربکس ممکن است تهاجمی‌تر باشد."
            ),

            YadraWarningIcon(
                id = "PARK",
                titleFa = "پارک",
                titleEn = "Park",
                category = YadraWarningCategory.COMFORT_STATUS,
                severity = YadraWarningSeverity.STATUS,
                color = YadraWarningColor.RED,
                iconName = "ic_warning_gear_p",
                descriptionFa = "گیربکس در وضعیت پارک قرار دارد.",
                possibleCausesFa = listOf(
                    "انتخاب وضعیت P"
                ),
                recommendedActionFa =
                    "برای حرکت وضعیت مناسب گیربکس را انتخاب کنید."
            ),

            YadraWarningIcon(
                id = "REVERSE",
                titleFa = "دنده عقب",
                titleEn = "Reverse",
                category = YadraWarningCategory.COMFORT_STATUS,
                severity = YadraWarningSeverity.STATUS,
                color = YadraWarningColor.WHITE,
                iconName = "ic_warning_reverse",
                descriptionFa = "گیربکس در وضعیت دنده عقب است.",
                possibleCausesFa = listOf(
                    "انتخاب وضعیت R"
                ),
                recommendedActionFa =
                    "هنگام حرکت به عقب اطراف خودرو را بررسی کنید."
            ),

            YadraWarningIcon(
                id = "NEUTRAL",
                titleFa = "خلاص",
                titleEn = "Neutral",
                category = YadraWarningCategory.COMFORT_STATUS,
                severity = YadraWarningSeverity.STATUS,
                color = YadraWarningColor.WHITE,
                iconName = "ic_warning_neutral",
                descriptionFa = "گیربکس در وضعیت خلاص قرار دارد.",
                possibleCausesFa = listOf(
                    "انتخاب وضعیت N"
                ),
                recommendedActionFa =
                    "برای حرکت وضعیت مناسب گیربکس را انتخاب کنید."
            ),

            YadraWarningIcon(
                id = "DRIVE",
                titleFa = "حرکت",
                titleEn = "Drive",
                category = YadraWarningCategory.COMFORT_STATUS,
                severity = YadraWarningSeverity.STATUS,
                color = YadraWarningColor.GREEN,
                iconName = "ic_warning_drive",
                descriptionFa = "گیربکس در وضعیت حرکت قرار دارد.",
                possibleCausesFa = listOf(
                    "انتخاب وضعیت D"
                ),
                recommendedActionFa =
                    "با توجه به شرایط جاده رانندگی کنید."
            )
        )

    // =============================================================
    // PUBLIC METHODS
    // =============================================================

    /**
     * دریافت تمام نمادها
     */
    fun getAll(): List<YadraWarningIcon> {
        return warnings
    }

    /**
     * دریافت نماد بر اساس ID
     */
    fun getById(
        id: String
    ): YadraWarningIcon? {

        return warnings.firstOrNull { warning ->
            warning.id == id
        }
    }

    /**
     * دریافت هشدارهای بحرانی
     */
    fun getCritical(): List<YadraWarningIcon> {

        return warnings.filter { warning ->
            warning.severity ==
                    YadraWarningSeverity.CRITICAL
        }
    }

    /**
     * دریافت هشدارهای معمولی
     */
    fun getWarnings(): List<YadraWarningIcon> {

        return warnings.filter { warning ->
            warning.severity ==
                    YadraWarningSeverity.WARNING
        }
    }

    /**
     * دریافت پیام‌های اطلاعاتی
     */
    fun getInformation(): List<YadraWarningIcon> {

        return warnings.filter { warning ->
            warning.severity ==
                    YadraWarningSeverity.INFORMATION
        }
    }

    /**
     * دریافت نمادهای وضعیت
     */
    fun getStatusIcons(): List<YadraWarningIcon> {

        return warnings.filter { warning ->
            warning.severity ==
                    YadraWarningSeverity.STATUS
        }
    }

    /**
     * دریافت نمادهای یک دسته
     */
    fun getByCategory(
        category: YadraWarningCategory
    ): List<YadraWarningIcon> {

        return warnings.filter { warning ->
            warning.category == category
        }
    }

    /**
     * جستجو در عنوان فارسی و انگلیسی
     */
    fun search(
        query: String
    ): List<YadraWarningIcon> {

        val text =
            query.trim()

        if (text.isEmpty()) {
            return emptyList()
        }

        return warnings.filter { warning ->

            warning.titleFa.contains(
                text,
                ignoreCase = true
            ) ||

                    warning.titleEn.contains(
                        text,
                        ignoreCase = true
                    ) ||

                    warning.id.contains(
                        text,
                        ignoreCase = true
                    ) ||

                    warning.descriptionFa.contains(
                        text,
                        ignoreCase = true
                    )
        }
    }

    /**
     * تعداد کل نمادها
     */
    fun count(): Int {
        return warnings.size
    }
}