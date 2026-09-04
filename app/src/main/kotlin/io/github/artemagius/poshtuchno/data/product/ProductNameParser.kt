package io.github.artemagius.poshtuchno.data.product

/**
 * Разбор названия товара из чека.
 *
 * Чеки пишут названия сжато и по-разному: «Энергет.напиток BURN Original ж/б 0,449л»,
 * «Напиток энерг. ADRENALINE RUSH 0.5 л», «Молоко Простоквашино 3,2% 930мл».
 * Задача — вытащить из этого измеримую часть (объём, вес, количество в упаковке)
 * и набор нормализованных слов, по которым товары можно сопоставлять между собой.
 *
 * Парсер намеренно не пытается «понять» товар. Он приводит строку к сравнимому
 * виду, а решение о группировке принимает [ProductGrouping] — по фактическим
 * повторениям в истории покупок, а не по встроенному справочнику.
 */
object ProductNameParser {

    /** Единица измерения товара. */
    enum class UnitKind { Piece, Volume, Weight }

    data class Parsed(
        /** Название без размерной части: «Энергетик BURN Original». */
        val cleanName: String,
        /** Объём одной единицы в миллилитрах. */
        val volumeMl: Int? = null,
        /** Вес одной единицы в граммах. */
        val weightG: Int? = null,
        /** Штук в упаковке: «6х0,33л» -> 6. */
        val packCount: Int? = null,
        /** Слова для сопоставления: нижний регистр, без сокращений и мусора. */
        val tokens: List<String> = emptyList(),
        /** Слово, похожее на бренд: латиница или ЗАГЛАВНЫЕ в исходной строке. */
        val brand: String? = null,
    ) {
        val unitKind: UnitKind
            get() = when {
                volumeMl != null -> UnitKind.Volume
                weightG != null -> UnitKind.Weight
                else -> UnitKind.Piece
            }

        /** Общий объём с учётом упаковки — для «сколько литров куплено». */
        val totalVolumeMl: Int?
            get() = volumeMl?.let { it * (packCount ?: 1) }

        val totalWeightG: Int?
            get() = weightG?.let { it * (packCount ?: 1) }
    }

    // Сокращения, которые чеки используют вместо слов. Раскрываем их,
    // иначе «энергет.» и «энергетик» не сойдутся при сопоставлении.
    private val abbreviations = mapOf(
        "энерг" to "энергетик",
        "энергет" to "энергетик",
        "энергетич" to "энергетик",
        "напит" to "напиток",
        "нап" to "напиток",
        "газ" to "газированный",
        "мол" to "молоко",
        "молоч" to "молочный",
        "шок" to "шоколад",
        "шоколадн" to "шоколад",
        "кол" to "колбаса",
        "колб" to "колбаса",
        "сыр" to "сыр",
        "хл" to "хлеб",
        "печ" to "печенье",
        "конф" to "конфеты",
        "сок" to "сок",
        "вод" to "вода",
        "минер" to "минеральный",
        "пив" to "пиво",
        "смет" to "сметана",
        "твор" to "творог",
        "маслo" to "масло",
        "масл" to "масло",
        "сосис" to "сосиски",
        "пельм" to "пельмени",
        "морож" to "мороженое",
        "йог" to "йогурт",
        "йогур" to "йогурт",
    )

    /**
     * Слова-шум: тара, фасовка, маркетинг. Не несут смысла для группировки,
     * но мешают: «ж/б» и «пэт» разводят один и тот же напиток по разным группам.
     */
    private val stopWords = setOf(
        "жб", "ж", "б", "пэт", "пет", "пл", "бут", "бутылка", "банка", "стекло",
        "уп", "упак", "упаковка", "пак", "пакет", "шт", "штука", "кор", "коробка",
        "тетра", "пак", "дой", "пачка", "фас", "весовой", "вес", "вес.",
        "новый", "новинка", "акция", "скидка", "промо", "лимитированный",
        "охл", "охлажденный", "заморож", "замороженный", "свежий",
        "тм", "трейд", "ооо", "оао", "зао", "ип",
    )

    private val volumeRegex = Regex(
        """(\d+(?:[.,]\d+)?)\s*(мл|ml|л|l|литр\w*)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val weightRegex = Regex(
        """(\d+(?:[.,]\d+)?)\s*(мг|mg|г|гр|g|кг|kg|килограмм\w*|грамм\w*)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val packRegex = Regex("""(\d+)\s*[хx*×]\s*(?=\d)""", RegexOption.IGNORE_CASE)
    private val percentRegex = Regex("""\d+(?:[.,]\d+)?\s*%""")
    private val latinRegex = Regex("""^[a-z][a-z'\-]{1,}$""", RegexOption.IGNORE_CASE)

    fun parse(rawName: String): Parsed {
        val original = rawName.trim()
        if (original.isEmpty()) return Parsed(cleanName = "")

        var working = original

        val packCount = packRegex.find(working)?.groupValues?.get(1)?.toIntOrNull()
        if (packCount != null) working = packRegex.replace(working, " ")

        val volumeMl = volumeRegex.find(working)?.let { match ->
            val value = match.groupValues[1].replace(',', '.').toDoubleOrNull()
            val unit = match.groupValues[2].lowercase()
            value?.let {
                when {
                    unit.startsWith("мл") || unit.startsWith("ml") -> it.toInt()
                    else -> (it * 1000).toInt()
                }
            }
        }

        val weightG = weightRegex.find(working)?.let { match ->
            val value = match.groupValues[1].replace(',', '.').toDoubleOrNull()
            val unit = match.groupValues[2].lowercase()
            value?.let {
                when {
                    unit.startsWith("мг") || unit.startsWith("mg") -> (it / 1000).toInt()
                    unit.startsWith("кг") || unit.startsWith("kg") || unit.startsWith("килограмм") ->
                        (it * 1000).toInt()
                    else -> it.toInt()
                }
            }
        }

        // Размерность и проценты жирности убираем из названия: они уже разобраны.
        var clean = volumeRegex.replace(working, " ")
        clean = weightRegex.replace(clean, " ")
        clean = percentRegex.replace(clean, " ")
        clean = clean.replace(Regex("""\s{2,}"""), " ").trim(' ', ',', '.', '-', '/')

        val tokens = tokenize(original)
        val brand = detectBrand(original)

        return Parsed(
            cleanName = clean.ifEmpty { original },
            volumeMl = volumeMl,
            weightG = weightG,
            packCount = packCount?.takeIf { it > 1 },
            tokens = tokens,
            brand = brand,
        )
    }

    /**
     * Приводит название к списку значимых слов.
     * Сокращения раскрываются, шум и размерности выбрасываются.
     */
    fun tokenize(rawName: String): List<String> {
        var text = rawName.lowercase()
        text = volumeRegex.replace(text, " ")
        text = weightRegex.replace(text, " ")
        text = percentRegex.replace(text, " ")

        return text
            .split(Regex("""[^\p{L}\p{Nd}]+"""))
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { abbreviations[it] ?: it }
            .filter { it.length >= 2 }
            .filter { it !in stopWords }
            // Чистые числа не несут смысла после удаления размерностей.
            .filter { !it.all(Char::isDigit) }
            .distinct()
            .toList()
    }

    /**
     * Бренд: латинское слово или слово, написанное в исходной строке заглавными.
     * Чеки почти всегда выделяют бренд именно так.
     */
    private fun detectBrand(rawName: String): String? {
        val words = rawName.split(Regex("""[^\p{L}\p{Nd}'\-]+""")).filter { it.length >= 3 }

        words.firstOrNull { word ->
            latinRegex.matches(word) && word.lowercase() !in stopWords
        }?.let { return it.lowercase().replaceFirstChar(Char::titlecase) }

        return words.firstOrNull { word ->
            word.any { it.isLetter() } &&
                word.filter(Char::isLetter).all { it.isUpperCase() } &&
                word.lowercase() !in stopWords &&
                word.lowercase() !in abbreviations
        }?.lowercase()?.replaceFirstChar(Char::titlecase)
    }
}
