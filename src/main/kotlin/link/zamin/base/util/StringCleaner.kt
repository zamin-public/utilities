package link.zamin.base.util


class StringCleaner {
    companion object {
        val charList = hashMapOf(
            ' ' to ' ',
            'ۀ' to 'ه',
            'ﮤ' to 'ه',
            'ە' to 'ه',
            'ﺓ' to 'ه',
            'ة' to 'ه',
            'آ' to 'ا',
            'أ' to 'ا',
            'إ' to 'ا',
            'ؤ' to 'و',
            'ئ' to 'ی',
            'ى' to 'ی',
            'ٔ' to 'ی',
            'ك' to 'ک',
            '‌' to ' ',
            'ي' to 'ی',
            '…' to null,
            'ـ' to null,
            'َ' to null,
            'ِ' to null,
            'ُ' to null,
            'ً' to null,
            'ٍ' to null,
            'ٌ' to null,
            'ّ' to null,
            'ْ' to null,
            'ٓ' to null,
        )

        fun convertArabicToPersian(text: String) =
            text.mapNotNull { charList.getOrDefault(it, it) }.joinToString("")


        fun replaceNumberToEnglish(text: String) = text
            .replace("۰", "0")
            .replace("۱", "1")
            .replace("۲", "2")
            .replace("۳", "3")
            .replace("۴", "4")
            .replace("۵", "5")
            .replace("۶", "6")
            .replace("۷", "7")
            .replace("۸", "8")
            .replace("۹", "9")

    }

    fun cleaner(text: String?): String {
        if (text.isNullOrEmpty())
            return ""
        val sb = StringBuilder(text.length)
        text.forEach {
            val ch = if (charList.containsKey(it)) charList[it] else it
            if (ch != null) sb.append(ch)
        }
        return sb.toString()
    }

    fun trimSpace(text: String): String {
        return text.trimStart(' ').trimEnd(' ')
    }

    fun replaceNumberToPersian(text: String) = text
        .replace("0", "۰")
        .replace("1", "۱")
        .replace("2", "۲")
        .replace("3", "۳")
        .replace("4", "۴")
        .replace("5", "۵")
        .replace("6", "۶")
        .replace("7", "۷")
        .replace("8", "۸")
        .replace("9", "۹")
}
