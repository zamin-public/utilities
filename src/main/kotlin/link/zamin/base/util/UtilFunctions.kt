package link.zamin.base.util

import link.zamin.base.model.dto.AgeDto
import org.joda.time.DateTime
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.random.Random

fun String.normalizePhoneNumber(): String {
    if (this.startsWith('0'))
        return this.replaceFirst("0", "98")
    if (this.toLongOrNull() != null && this.length == 10)
        return "98$this"
    return this
}

fun removeSpaceFullName(name: String, surname: String) =
    StringCleaner.convertArabicToPersian("${name.replace(" ", "")}${surname.replace(" ", "")}")

fun fullName(name: String, surname: String) =
    StringCleaner.convertArabicToPersian("${name.replace(" ", "")} ${surname.replace(" ", "")}")

fun removeSpaceFullName(text: String) =
    StringCleaner.convertArabicToPersian(text.replace(" ", ""))

fun String.isZeroOrBlank() = this.isBlank() || this == "0"

fun getStartOfDayUnixTimestamp(unixTimestamp: Long): Long {
    val localDate = Instant.ofEpochMilli(unixTimestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    return localDate.atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

fun convertUnixTimestampToFormattedString(unixTimestamp: Long): String {
    val instant = Instant.ofEpochSecond(unixTimestamp)
    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

    val defaultZoneId = ZoneId.systemDefault()
    val offset = defaultZoneId.rules.getOffset(Instant.now())

    val offsetDateTime = OffsetDateTime.ofInstant(instant, offset)
    return offsetDateTime.format(dateTimeFormatter)
}


fun minutesToMilliseconds(minutes: Long): Long {
    return minutes * 60000
}

fun getAge(birthDateMillis: Long): AgeDto {

    val now = DateTime()
    val nowJalali = DateConverter.gregorian_to_jalali(
        gy = now.year,
        gm = now.monthOfYear,
        gd = now.dayOfMonth
    )

    val birthDate = DateTime(birthDateMillis)
    val birthDateJalali = DateConverter.gregorian_to_jalali(
        gy = birthDate.year,
        gm = birthDate.monthOfYear,
        gd = birthDate.dayOfMonth
    )

    val offset = if (birthDateJalali[1] <= 6) 1 else 0

    return AgeDto(
        age = (nowJalali[0] - birthDateJalali[0]) + offset,
        jalaliDate = "${birthDateJalali[0]}/${birthDateJalali[1]}/${birthDateJalali[2]}"
    )
}

fun getRandomNumber(length: Int = 14) =
    Random.nextInt(1, 9).toString() + (0..length)
        .map { Random.nextInt(0, 9) }.joinToString("")

fun findBirthDateWithSlash(dateFa: String): Long? {
    val dateArr = dateFa.split("/")
    return if (dateArr.size == 3)
        DateConverter.jalaliToGregorian(
            year = dateArr[0].toInt(),
            month = dateArr[1].toInt(),
            day = dateArr[2].toInt(),
        ).millis
    else null
}

fun findBirthDate(dateFa: String): Long? {
    if (dateFa.isZeroOrBlank())
        return null
    if (dateFa.contains("/"))
        return null
    return DateConverter.jalaliToGregorian(
        year = dateFa.substring(0, 4).toInt(),
        month = dateFa.substring(4, 6).toInt(),
        day = dateFa.substring(6).toInt(),
    ).millis
}

