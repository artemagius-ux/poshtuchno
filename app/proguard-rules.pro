# Правила ProGuard/R8 для release-сборки.
# Room и Compose поставляют свои правила через consumer rules, дополнять не нужно.

# Сохраняем имена enum-констант: они пишутся в базу как строки (Converters.kt).
-keepclassmembers enum io.github.artemagius.poshtuchno.data.db.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
