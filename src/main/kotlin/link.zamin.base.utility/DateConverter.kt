package link.zamin.base.utility


import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import java.util.*


object DateConverter {
    private val persianCal: Calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
    private val calendar = Calendar.getInstance()
    fun getJalaliText(date: Long): String {

        val gregorianDate = Date(date)
        persianCal.time = gregorianDate
        return getJalaliText(
            gregorian_to_jalali(
                persianCal.get(Calendar.YEAR),
                persianCal.get(Calendar.MONTH) + 1,
                persianCal.get(Calendar.DAY_OF_MONTH),
            )
        )
    }

    fun getJalaliText(rawJalali: IntArray): String {
        return rawJalali[0].toString() + "/" + rawJalali[1].toString() + "/" + rawJalali[2].toString()
    }

    fun getDaysAgoOrPast(daysAgo: Int, fromDate: Date): Date {
        calendar.time = fromDate
        calendar.add(Calendar.DAY_OF_YEAR, daysAgo)
        return calendar.time
    }

    fun getMinutesAgoOrPast(minutes: Int, fromDate: Date): Date {
        calendar.time = fromDate
        calendar.add(Calendar.MINUTE, minutes)
        return calendar.time
    }

    fun jalaliToGregorian(year: Int, month: Int, day: Int, hour: Int = 0, min: Int = 0, sec: Int = 0): DateTime {
        val date = jalali_to_gregorian(year, month, day)
        val timeZone = DateTimeZone.forID("Asia/Tehran")
        var localDateTime: LocalDateTime = LocalDateTime(timeZone)
            .withYear(date[0])
            .withMonthOfYear(date[1])
            .withDayOfMonth(date[2])
            .withHourOfDay(hour)
            .withMinuteOfHour(min)
            .withSecondOfMinute(sec)
        if (timeZone.isLocalDateTimeGap(localDateTime)) {
            localDateTime = localDateTime.withHourOfDay(1)
        }

        return localDateTime.toDateTime(timeZone)
    }

    fun jalaliToGregorianUTC(year: Int, month: Int, day: Int, hour: Int = 0, min: Int = 0, sec: Int = 0): DateTime {
        return jalaliToGregorian(year, month, day, hour, min, sec).toDateTime(DateTimeZone.UTC)
    }

    fun gregorian_to_jalali(gy: Int, gm: Int, gd: Int): IntArray {
        var _gy = gy
        val g_d_m = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        var jy: Int
        if (_gy > 1600) {
            jy = 979
            _gy -= 1600
        } else {
            jy = 0
            _gy -= 621
        }
        val gy2 = if (gm > 2) _gy + 1 else _gy
        var days = 365 * _gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 - 80 + gd + g_d_m[gm - 1]
        jy += 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm = if (days < 186) 1 + days / 31 else 7 + (days - 186) / 30
        val jd = 1 + if (days < 186) days % 31 else (days - 186) % 30
        return intArrayOf(jy, jm, jd)
    }

    private fun jalali_to_gregorian(jy: Int, jm: Int, jd: Int): IntArray {
        var _jy = jy
        var gy: Int
        if (_jy > 979) {
            gy = 1600
            _jy -= 979
        } else {
            gy = 621
        }
        var days =
            365 * _jy + _jy / 33 * 8 + (_jy % 33 + 3) / 4 + 78 + jd + if (jm < 7) (jm - 1) * 31 else (jm - 7) * 30 + 186
        gy += 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            gy += 100 * (--days / 36524)
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val sal_a = intArrayOf(
            0,
            31,
            if (gy % 4 == 0 && gy % 100 != 0 || gy % 400 == 0) 29 else 28,
            31,
            30,
            31,
            30,
            31,
            31,
            30,
            31,
            30,
            31
        )
        var gm = 0
        while (gm < 13) {
            val v = sal_a[gm]
            if (gd <= v) break
            gd -= v
            gm++
        }
        return intArrayOf(gy, gm, gd)
    }

    fun convertSecondsToHoursAndMinutes(seconds: Long): String {
        return "${(seconds % 3600) / 60}:${(seconds % 3600) % 60}"
    }
}
