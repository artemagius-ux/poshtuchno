package io.github.artemagius.poshtuchno.data.product

/**
 * Автоматические подкатегории по повторяющимся словам.
 *
 * Идея: если в истории покупок несколько разных товаров содержат одно и то же
 * слово, это слово почти наверняка обозначает вид товара или бренд. «Энергетик
 * BURN», «Энергетик Adrenaline Rush» и «Энергетик Flash» дают группу
 * «Энергетик», а «Молоко Простоквашино» и «Кефир Простоквашино» — группу
 * «Простоквашино».
 *
 * Справочника товаров здесь нет намеренно: группы возникают из того, что человек
 * реально покупает, а не из чужой классификации. Поэтому и работает на любых
 * товарах, включая местные и редкие.
 */
object ProductGrouping {

    data class ProductInfo(
        val id: Long,
        val name: String,
        val tokens: List<String>,
    )

    data class GroupSuggestion(
        /** Нормализованное слово-ключ: «энергетик». */
        val token: String,
        /** Как показывать: «Энергетик». */
        val title: String,
        val productIds: List<Long>,
    ) {
        val productCount: Int get() = productIds.size
    }

    /**
     * Предлагает группы по товарам.
     *
     * @param minProducts сколько разных товаров должно содержать слово,
     *   чтобы оно стало группой. Меньше двух смысла не имеет — группа из
     *   одного товара это сам товар.
     */
    fun suggest(
        products: List<ProductInfo>,
        minProducts: Int = 2,
    ): List<GroupSuggestion> {
        if (products.size < minProducts) return emptyList()

        val byToken = LinkedHashMap<String, MutableList<Long>>()
        products.forEach { product ->
            product.tokens.distinct().forEach { token ->
                byToken.getOrPut(token) { mutableListOf() }.add(product.id)
            }
        }

        val candidates = byToken
            .filterValues { it.size >= minProducts }
            .map { (token, ids) ->
                GroupSuggestion(
                    token = token,
                    title = token.replaceFirstChar(Char::titlecase),
                    productIds = ids.distinct().sorted(),
                )
            }

        // Слова, которые всегда встречаются вместе, дают одинаковый состав группы.
        // «Энергетический напиток» -> и «энергетик», и «напиток» покрывают одно
        // и то же. Оставляем более длинное слово: оно конкретнее.
        return candidates
            .groupBy { it.productIds }
            .map { (_, sameMembers) -> sameMembers.maxBy { it.token.length } }
            .sortedWith(
                compareByDescending<GroupSuggestion> { it.productCount }
                    .thenByDescending { it.token.length }
                    .thenBy { it.token },
            )
    }

    /**
     * Ключ для сопоставления одинаковых товаров.
     *
     * «Энергет.напиток BURN 0,449л» и «BURN энергетик ж/б 449 мл» должны
     * попасть в одну карточку товара, иначе повторы не склеятся и статистика
     * развалится на почти одинаковые строки.
     *
     * Слова сортируются, поэтому порядок в чеке не важен. Размер входит в ключ:
     * банка 0,449 и бутылка 1 л — разные товары с разной ценой за литр.
     */
    fun matchKey(parsed: ProductNameParser.Parsed): String {
        val words = parsed.tokens.sorted().joinToString("-")
        val size = when {
            parsed.volumeMl != null -> "v${parsed.volumeMl}"
            parsed.weightG != null -> "w${parsed.weightG}"
            else -> ""
        }
        val pack = parsed.packCount?.let { "p$it" }.orEmpty()
        return listOf(words, size, pack).filter { it.isNotEmpty() }.joinToString("|")
    }
}
