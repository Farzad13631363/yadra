package com.example.yadra

/**
 * مدل اصلی هر نماد هشدار یا وضعیت داشبورد YADRA
 *
 * این کلاس فقط اطلاعات مربوط به نماد را نگهداری می‌کند.
 * هیچ ارتباطی با Wi-Fi، Bluetooth یا ECU ندارد.
 */
data class YadraWarningIcon(

    /**
     * شناسه یکتا
     *
     * مثال:
     * ENGINE_OIL_PRESSURE
     */
    val id: String,

    /**
     * عنوان فارسی
     */
    val titleFa: String,

    /**
     * عنوان انگلیسی
     */
    val titleEn: String,

    /**
     * دسته‌بندی نماد
     */
    val category: YadraWarningCategory,

    /**
     * سطح اهمیت
     */
    val severity: YadraWarningSeverity,

    /**
     * رنگ استاندارد نمایشی
     */
    val color: YadraWarningColor,

    /**
     * نام فایل آیکون در drawable
     *
     * مثال:
     * ic_warning_oil
     */
    val iconName: String,

    /**
     * توضیح فارسی
     */
    val descriptionFa: String,

    /**
     * دلایل احتمالی
     */
    val possibleCausesFa: List<String>,

    /**
     * اقدام پیشنهادی
     */
    val recommendedActionFa: String,

    /**
     * آیا نماد مربوط به علائم استاندارد خودرو است؟
     *
     * توجه:
     * true بودن این مقدار به معنی گواهی ISO برای هر نماد نیست.
     */
    val standardSymbol: Boolean = true
)


/**
 * دسته‌بندی نمادهای داشبورد
 */
enum class YadraWarningCategory {

    /**
     * موتور
     */
    ENGINE,

    /**
     * ترمز و ایمنی
     */
    BRAKE_SAFETY,

    /**
     * پایداری و کنترل کشش
     */
    STABILITY_TRACTION,

    /**
     * گیربکس
     */
    TRANSMISSION,

    /**
     * سوخت و مایعات
     */
    FUEL_FLUIDS,

    /**
     * چراغ‌ها
     */
    LIGHTING,

    /**
     * سیستم‌های کمک راننده
     */
    DRIVER_ASSIST,

    /**
     * درب‌ها و بدنه
     */
    ACCESS_BODY,

    /**
     * وضعیت و امکانات
     */
    COMFORT_STATUS
}


/**
 * سطح اهمیت هشدار
 */
enum class YadraWarningSeverity {

    /**
     * وضعیت بحرانی
     *
     * مثال:
     * فشار روغن
     * دمای شدید موتور
     */
    CRITICAL,

    /**
     * هشدار
     *
     * مثال:
     * ABS
     * فشار باد تایر
     */
    WARNING,

    /**
     * اطلاعات
     *
     * مثال:
     * فعال بودن نور بالا
     */
    INFORMATION,

    /**
     * وضعیت عادی سیستم
     *
     * مثال:
     * Park
     * Drive
     * Eco
     */
    STATUS
}


/**
 * رنگ نمایش نماد
 */
enum class YadraWarningColor {

    /**
     * قرمز
     */
    RED,

    /**
     * نارنجی
     */
    AMBER,

    /**
     * زرد
     */
    YELLOW,

    /**
     * سبز
     */
    GREEN,

    /**
     * آبی
     */
    BLUE,

    /**
     * سفید
     */
    WHITE
}