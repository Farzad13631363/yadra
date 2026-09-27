package com.example.yadra

/**
 * Repository مرکزی برای دسترسی به کتابخانه هشدارهای YADRA
 *
 * این کلاس هیچ ارتباطی با:
 * Wi-Fi
 * Bluetooth
 * ECU
 * YadraConnectionManager
 *
 * ندارد.
 *
 * وظیفه این کلاس فقط مدیریت و فیلتر کردن
 * اطلاعات موجود در YadraWarningIconLibrary است.
 */
object YadraWarningRepository {

    /**
     * دریافت تمام نمادها
     */
    fun getAll(): List<YadraWarningIcon> {
        return YadraWarningIconLibrary.getAll()
    }

    /**
     * دریافت یک نماد بر اساس ID
     */
    fun getById(
        id: String
    ): YadraWarningIcon? {

        return YadraWarningIconLibrary.getById(
            id
        )
    }

    /**
     * دریافت هشدارهای بحرانی
     */
    fun getCritical(): List<YadraWarningIcon> {

        return YadraWarningIconLibrary.getCritical()
    }

    /**
     * دریافت هشدارهای معمولی
     */
    fun getWarnings(): List<YadraWarningIcon> {

        return YadraWarningIconLibrary.getWarnings()
    }

    /**
     * دریافت پیام‌های اطلاعاتی
     */
    fun getInformation(): List<YadraWarningIcon> {

        return YadraWarningIconLibrary.getInformation()
    }

    /**
     * دریافت نمادهای وضعیت
     */
    fun getStatusIcons(): List<YadraWarningIcon> {

        return YadraWarningIconLibrary.getStatusIcons()
    }

    /**
     * دریافت نمادهای یک دسته خاص
     */
    fun getByCategory(
        category: YadraWarningCategory
    ): List<YadraWarningIcon> {

        return YadraWarningIconLibrary.getByCategory(
            category
        )
    }

    /**
     * جستجوی نمادها
     *
     * جستجو در:
     * - نام فارسی
     * - نام انگلیسی
     * - ID
     * - توضیحات فارسی
     */
    fun search(
        query: String
    ): List<YadraWarningIcon> {

        return YadraWarningIconLibrary.search(
            query
        )
    }

    /**
     * دریافت تعداد کل نمادها
     */
    fun count(): Int {

        return YadraWarningIconLibrary.count()
    }

    /**
     * دریافت نمادهای مربوط به موتور
     */
    fun getEngineWarnings(): List<YadraWarningIcon> {

        return getByCategory(
            YadraWarningCategory.ENGINE
        )
    }

    /**
     * دریافت نمادهای مربوط به ترمز و ایمنی
     */
    fun getBrakeSafetyWarnings(): List<YadraWarningIcon> {

        return getByCategory(
            YadraWarningCategory.BRAKE_SAFETY
        )
    }

    /**
     * دریافت نمادهای مربوط به پایداری و کشش
     */
    fun getStabilityWarnings(): List<YadraWarningIcon> {

        return getByCategory(
            YadraWarningCategory.STABILITY_TRACTION
        )
    }

    /**
     * دریافت نمادهای مربوط به گیربکس
     */
    fun getTransmissionWarnings(): List<YadraWarningIcon> {

        return getByCategory(
            YadraWarningCategory.TRANSMISSION
        )
    }

    /**
     * دریافت نمادهای مربوط به سوخت و مایعات
     */
    fun getFuelAndFluidWarnings(): List<YadraWarningIcon> {

        return getByCategory(
            YadraWarningCategory.FUEL_FLUIDS
        )
    }

    /**
     * دریافت نمادهای مربوط به چراغ‌ها
     */
    fun getLightingWarnings(): List<YadraWarningIcon> {

        return getByCategory(
            YadraWarningCategory.LIGHTING
        )
    }

    /**
     * دریافت نمادهای کمک راننده
     */
    fun getDriverAssistWarnings(): List<YadraWarningIcon> {

        return getByCategory(
            YadraWarningCategory.DRIVER_ASSIST
        )
    }

    /**
     * دریافت نمادهای درب و بدنه
     */
    fun getAccessBodyWarnings(): List<YadraWarningIcon> {

        return getByCategory(
            YadraWarningCategory.ACCESS_BODY
        )
    }

    /**
     * دریافت نمادهای وضعیت و امکانات
     */
    fun getComfortStatusIcons(): List<YadraWarningIcon> {

        return getByCategory(
            YadraWarningCategory.COMFORT_STATUS
        )
    }
}