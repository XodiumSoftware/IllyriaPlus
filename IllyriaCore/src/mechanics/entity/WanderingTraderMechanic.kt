package org.xodium.illyriacore.mechanics.entity

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.entity.WanderingTrader
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.Utils.MM
import org.xodium.illyriacore.data.WanderingTraderItemData
import org.xodium.illyriacore.gui.stock.StockBuyGui
import org.xodium.illyriacore.gui.stock.StockCategoriesGui
import org.xodium.illyriacore.gui.stock.StockHandlers
import org.xodium.illyriacore.gui.stock.StockSellGui
import org.xodium.illyriacore.mechanics.MechanicInterface
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64

/**
 * Replaces the wandering trader's trade GUI with a custom shop GUI shared by all wandering traders.
 * stock is shared runtime state filled by players, with supply-and-demand pricing in credit
 * points: items trade at [BASE_PRICE] points by default, drifting by [PRICE_STEP_POINTS] points
 * per [DEMAND_PER_PRICE_STEP] net items bought or sold (clamped to [[MIN_PRICE]; [MAX_PRICE]]),
 * and selling pays out at [SELL_PRICE_RATIO] of the current price. Prices are displayed in
 * emeralds; payment is accepted in any [Currency], converted at its point value. Individual
 * items within bulk transactions are priced along the moving price curve rather than at a flat
 * starting rate, and open shop windows refresh for all viewers on every stock or price change.
 */
internal object WanderingTraderMechanic : MechanicInterface {
    private const val PURCHASE_MSG = "<green>Purchase successful!"
    private const val NO_FUNDS_MSG = "<firewatch>You can't afford this item!</gradient>"
    private const val OUT_OF_STOCK_MSG = "<firewatch>The trader is out of stock!</gradient>"
    private const val SOLD_MSG = "<green>The trader accepted your items!"
    private const val CURRENCY_REJECTED_MSG = "<firewatch>The trader doesn't accept currency items!</gradient>"
    private const val STOCK_FILE_NAME = "wandering_trader_stock.yml"

    /** Debounce window before pending stock changes are persisted, in ticks. */
    private const val SAVE_DELAY_TICKS = 100L

    /** Base price per item in credit points (2 emeralds), shifted by supply and demand. */
    private const val BASE_PRICE = 32

    /** The fraction of the current price the trader pays when buying items from players. */
    private const val SELL_PRICE_RATIO = 0.5

    /** Net bought-minus-sold items needed to shift the price by [PRICE_STEP_POINTS] points. */
    private const val DEMAND_PER_PRICE_STEP = 32

    /** Points one price step is worth (one emerald). */
    private const val PRICE_STEP_POINTS = 16

    /** The lowest possible price in points per item, regardless of demand (1 emerald). */
    private const val MIN_PRICE = 16

    /** The highest possible price in points per item, regardless of demand (64 emeralds). */
    private const val MAX_PRICE = 1024

    private val PURCHASE_SOUND: Sound =
        Sound.sound(Key.key("entity.experience_orb.pickup"), Sound.Source.PLAYER, 1.0f, 1.0f)
    private val NO_FUNDS_SOUND: Sound =
        Sound.sound(Key.key("entity.villager.no"), Sound.Source.PLAYER, 1.0f, 1.0f)

    /** Plain currency templates used to identify payment-eligible inventory slots via [ItemStack.isSimilar]. */
    private val CURRENCY_TEMPLATES = Currency.entries.associateWith { ItemStack.of(it.material) }

    private val stockFile = File(instance.dataFolder, STOCK_FILE_NAME)

    /** Staging file for atomic stock writes; see [writeStockFile]. */
    private val stockTempFile = File(instance.dataFolder, "$STOCK_FILE_NAME.tmp")

    /**
     * Shared stock of all wandering traders, keyed by serialized single-item stacks (see [keyOf])
     * so item meta is preserved; counts are individual items.
     */
    private val stock = mutableMapOf<String, StockEntry>()

    /** Net bought-minus-sold items per [Material]; shifts prices via [priceOf]. */
    private val demand = mutableMapOf<Material, Int>()

    /** Guards against interleaved synchronous and asynchronous stock file writes. */
    private val writeLock = Any()

    /** Version of the newest built snapshot, incremented on the main thread per build. */
    private var snapshotVersion = 0L

    /** Version of the last snapshot written to disk, only updated under [writeLock]. */
    private var writtenVersion = 0L

    private var stockLoaded = false

    /** The pending debounced save task, or null when no save is scheduled. */
    private var saveTask: BukkitTask? = null

    /** Set when shutdown begins, suppressing further debounced save scheduling. */
    private var shuttingDown = false

    @EventHandler(ignoreCancelled = true)
    fun on(event: PlayerInteractEntityEvent) = handleInteract(event)

    override fun onDisable() {
        // Suppress save scheduling, then close tracked windows so pending deposits land in the stock before flushing.
        shuttingDown = true
        StockCategoriesGui.closeAll()
        StockBuyGui.closeAll()
        StockSellGui.closeAll()
        flushStock()
    }

    /**
     * Opens the custom shop GUI when a player right-clicks any wandering trader,
     * suppressing the vanilla trade window. Lead and name tag interactions are left to vanilla.
     *
     * @param event The PlayerInteractEntityEvent triggered when a player interacts with an entity.
     */
    private fun handleInteract(event: PlayerInteractEntityEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (event.rightClicked !is WanderingTrader) return
        val player = event.player
        val inHand = player.inventory.itemInMainHand.type
        if (inHand == Material.LEAD || inHand == Material.NAME_TAG) return
        event.isCancelled = true
        StockCategoriesGui.open(
            player,
            StockHandlers(
                items = ::stockedTrades,
                stockOf = ::stockOf,
                purchase = ::purchase,
                deposit = ::processDeposit,
            ),
        )
    }

    /**
     * Returns the trade entries currently in stock, one per stocked item variant, ordered by
     * material name, priced at their current demand-driven value.
     */
    private fun stockedTrades(): List<WanderingTraderItemData> {
        if (!loadStock()) return emptyList()
        return stock
            .values
            .filter { it.count > 0 }
            .sortedWith(compareBy<StockEntry> { it.template.type.name }.thenBy { keyOf(it.template) })
            .map {
                WanderingTraderItemData(
                    it.template.asOne(),
                    ItemStack.of(
                        Material.EMERALD,
                        (priceOf(it.template.type) + Currency.EMERALD.points - 1) / Currency.EMERALD.points,
                    ),
                )
            }
    }

    /**
     * Executes a purchase: buys up to [units] items of [item], capped by the available stock and
     * the player's credit points across all accepted currencies. Each item is priced at the rising
     * demand curve, so bulk purchases pay progressively more per item instead of a flat rate.
     * Payment is taken from cheapest currency first; if a larger denomination is needed to cover
     * the remainder, change is returned in smaller denominations. Deducts the total cost, then
     * delivers the result; overflow items are dropped at the player's feet.
     *
     * @param player The purchasing player.
     * @param item The entry being purchased.
     * @param units The maximum number of items to buy (bulk purchase amount).
     */
    private fun purchase(
        player: Player,
        item: WanderingTraderItemData,
        units: Int,
    ) {
        if (!loadStock()) return
        val entry = stock[keyOf(item.result)]
        val inStock = (entry?.count ?: 0) / item.result.amount
        if (entry == null || inStock == 0) {
            player.sendActionBar(MM.deserialize(OUT_OF_STOCK_MSG))
            player.playSound(NO_FUNDS_SOUND)
            return
        }

        val wallet = collectCurrency(player)
        val funds = wallet.entries.sumOf { (currency, stacks) -> currency.points * stacks.values.sumOf { it.amount } }
        val maxUnits = minOf(units, inStock)
        val demandBefore = demand[item.result.type] ?: 0
        var bought = 0
        var cost = 0
        while (bought < maxUnits) {
            val next = priceFor(demandBefore + bought * item.result.amount)
            if (cost + next > funds) break
            cost += next
            bought++
        }
        if (bought == 0) {
            player.sendActionBar(MM.deserialize(NO_FUNDS_MSG))
            player.playSound(NO_FUNDS_SOUND)
            return
        }

        val change = deductCurrency(player, wallet, cost)
        if (change > 0) payOut(player, change)

        entry.count -= item.result.amount * bought
        demand.merge(item.result.type, item.result.amount * bought, Int::plus)
        saveStock()
        give(player, item.result.clone().apply { amount = item.result.amount * bought })
        player.sendActionBar(MM.deserialize(PURCHASE_MSG))
        player.playSound(PURCHASE_SOUND)
    }

    /**
     * Collects all currency items in the player's inventory, grouped by [Currency] and keeping
     * their inventory slots so they can be deducted later.
     */
    private fun collectCurrency(player: Player): Map<Currency, Map<Int, ItemStack>> =
        Currency
            .entries
            .mapNotNull { currency ->
                val template = CURRENCY_TEMPLATES.getValue(currency)
                val stacks = player.inventory.all(currency.material).filterValues { it.isSimilar(template) }
                stacks.takeIf { it.isNotEmpty() }?.let { currency to it }
            }.toMap()

    /**
     * Deducts [cost] credit points from [wallet] in the player's inventory, spending cheapest
     * currency first. When no exact combination exists, spends one extra item of the smallest
     * denomination that covers the remainder and returns the overpayment in points as change.
     *
     * @return The change owed to the player in credit points (0 when payment is exact).
     */
    private fun deductCurrency(
        player: Player,
        wallet: Map<Currency, Map<Int, ItemStack>>,
        cost: Int,
    ): Int {
        var remaining = cost
        for (currency in Currency.entries) {
            if (remaining <= 0) break
            val stacks = wallet[currency] ?: continue
            for ((slot, stack) in stacks) {
                if (remaining <= 0) break
                val deduct = minOf(remaining / currency.points, stack.amount)
                stack.amount -= deduct
                remaining -= deduct * currency.points
                if (remaining in 1..<currency.points && stack.amount > 0) {
                    // Overspend one item to cover the remainder; the excess is returned as change.
                    stack.amount -= 1
                    remaining -= currency.points
                }
                player.inventory.setItem(slot, stack.takeIf { it.amount > 0 })
            }
        }
        return -remaining
    }

    /**
     * Processes the contents of the sell window: every deposited item is added to the shared stock
     * and paid via [sellPayoutOf], so items are paid along the falling price curve rather than at
     * a flat rate. Payouts are given in currency items, preferring larger denominations. Currency
     * items themselves are returned unprocessed. Item meta is preserved in the stock, so variants
     * of the same material are stocked and traded separately. If the stock fails to load, all
     * deposited items are returned unprocessed.
     *
     * @param player The selling player.
     * @param contents The deposited items, possibly containing null slots.
     */
    private fun processDeposit(
        player: Player,
        contents: List<ItemStack?>,
    ) {
        if (!loadStock()) {
            contents.filterNotNull().forEach { give(player, it) }
            return
        }
        var sold = false
        var rejected = false
        contents.filterNotNull().forEach { stack ->
            if (Currency.byMaterial(stack.type) != null) {
                give(player, stack)
                rejected = true
                return@forEach
            }
            stock.getOrPut(keyOf(stack)) { StockEntry(stack.asOne(), 0) }.count += stack.amount
            payOut(player, sellPayoutOf(stack.type, stack.amount))
            demand.merge(stack.type, -stack.amount, Int::plus)
            sold = true
        }
        if (sold) {
            saveStock()
            player.sendActionBar(MM.deserialize(SOLD_MSG))
            player.playSound(PURCHASE_SOUND)
        }
        if (rejected) {
            player.sendActionBar(MM.deserialize(CURRENCY_REJECTED_MSG))
            player.playSound(NO_FUNDS_SOUND)
        }
    }

    /**
     * Returns the current stock (individual items) of a trade entry.
     */
    private fun stockOf(item: WanderingTraderItemData): Int {
        if (!loadStock()) return 0
        return stock[keyOf(item.result)]?.count ?: 0
    }

    /**
     * Returns the current buy price (credit points per item) for a material: [priceFor] at its
     * current net bought-minus-sold demand.
     */
    private fun priceOf(type: Material): Int = priceFor(demand[type] ?: 0)

    /**
     * Returns the buy price (credit points per item) at [netDemand] net bought-minus-sold items,
     * starting at [BASE_PRICE] and shifting by [PRICE_STEP_POINTS] points per
     * [DEMAND_PER_PRICE_STEP] net traded items, clamped between [MIN_PRICE] and [MAX_PRICE].
     */
    private fun priceFor(netDemand: Int): Int =
        (BASE_PRICE + netDemand / DEMAND_PER_PRICE_STEP * PRICE_STEP_POINTS).coerceIn(MIN_PRICE, MAX_PRICE)

    /**
     * Returns the payout (credit points per item) when selling at [price]: [SELL_PRICE_RATIO] of
     * it, at least 1 point.
     */
    private fun sellPriceOf(price: Int): Int = (price * SELL_PRICE_RATIO).toInt().coerceAtLeast(1)

    /**
     * Returns the total payout (credit points) for selling [amount] items of [type] right now:
     * each item is paid at [sellPriceOf] of the falling price curve, so bulk deposits don't clear
     * at the starting price.
     */
    private fun sellPayoutOf(
        type: Material,
        amount: Int,
    ): Int {
        val demandBefore = demand[type] ?: 0
        var payout = 0
        repeat(amount) { payout += sellPriceOf(priceFor(demandBefore - it)) }
        return payout
    }

    /**
     * Pays out [points] credit points to the player in currency items, preferring larger
     * denominations so the fewest items are given. Overflow is dropped at the player's feet.
     */
    private fun payOut(
        player: Player,
        points: Int,
    ) {
        var remaining = points
        Currency.entries.reversed().forEach { currency ->
            val amount = remaining / currency.points
            if (amount > 0) {
                give(player, ItemStack.of(currency.material, amount))
                remaining -= amount * currency.points
            }
        }
    }

    /**
     * Gives an item to the player, dropping overflow at their feet.
     */
    private fun give(
        player: Player,
        stack: ItemStack,
    ) {
        player
            .inventory
            .addItem(stack)
            .values
            .forEach { player.world.dropItemNaturally(player.location, it) }
    }

    /**
     * Returns a stable stock map key for an [ItemStack], ignoring stack size: the Base64 encoding
     * of a serialized single-item copy. Stacks of equal type and meta share a key.
     */
    private fun keyOf(stack: ItemStack): String = Base64.getEncoder().encodeToString(stack.asOne().serializeAsBytes())

    /**
     * Lazily loads the stock and demand from disk. A missing file counts as a successful empty
     * load. On failure the stock is left unloaded, so the next access retries the load.
     *
     * @return true when the stock is ready to use, false when reading the file failed.
     */
    private fun loadStock(): Boolean {
        if (stockLoaded) return true
        if (!stockFile.exists()) {
            stockLoaded = true
            return true
        }
        return runCatching {
            val config = YamlConfiguration()
            config.load(stockFile)
            config.getConfigurationSection("stock")?.getKeys(false)?.forEach { key ->
                config.getItemStack("stock.$key.stack")?.let {
                    stock[keyOf(it)] = StockEntry(it.asOne(), config.getInt("stock.$key.count"))
                }
            }
            config.getConfigurationSection("demand")?.getKeys(false)?.forEach { key ->
                Material.getMaterial(key)?.let { demand[it] = config.getInt("demand.$key") }
            }
        }.onSuccess {
            stockLoaded = true
        }.onFailure {
            instance.logger.warning("Failed to load wandering trader stock: ${it.message}")
        }.isSuccess
    }

    /**
     * Schedules pending stock changes to be persisted, debounced by [SAVE_DELAY_TICKS] ticks. The
     * YAML snapshot is built on the main thread when the task fires; only the disk write runs
     * asynchronously. Does nothing while the stock is not loaded, so a failed load cannot
     * schedule overwriting existing data, or once [shuttingDown] is set, so no task is scheduled
     * after plugin disable begins.
     */
    private fun saveStock() {
        if (!stockLoaded || shuttingDown) return
        if (saveTask != null) return
        saveTask =
            instance.server.scheduler.runTaskLater(
                instance,
                Runnable {
                    saveTask = null
                    writeStock()
                },
                SAVE_DELAY_TICKS,
            )
    }

    /**
     * Builds the YAML snapshot of the current state and writes it to the stock file asynchronously,
     * stamping it with a new version so stale async writes cannot overwrite newer files.
     */
    private fun writeStock() {
        if (!stockLoaded) return
        val version = ++snapshotVersion
        val yaml = buildStockYaml()
        instance.server.scheduler.runTaskAsynchronously(instance, Runnable { writeStockFile(yaml, version) })
    }

    /**
     * Cancels any pending debounced save and persists the current state synchronously.
     */
    private fun flushStock() {
        if (!stockLoaded) return
        saveTask?.cancel()
        saveTask = null
        writeStockFile(buildStockYaml(), ++snapshotVersion)
    }

    /**
     * Serializes the current stock and demand into a YAML string, omitting entries with zero
     * stock or zero demand. Must be called on the main thread.
     */
    private fun buildStockYaml(): String {
        val config = YamlConfiguration()
        var index = 0
        stock.values.filter { it.count > 0 }.forEach {
            config.set("stock.$index.stack", it.template)
            config.set("stock.$index.count", it.count)
            index++
        }
        demand.filterValues { it != 0 }.forEach { (type, amount) -> config.set("demand.${type.name}", amount) }
        return config.saveToString()
    }

    /**
     * Writes [yaml] to the stock file, guarding against interleaved and out-of-order writes:
     * snapshots not newer than the last written one are skipped. The content is written to
     * [stockTempFile] first and then moved over [stockFile], so a crash mid-write can only
     * corrupt the temporary staging file, never the previous intact stock file. Falls back to a
     * plain move on filesystems without atomic-move support.
     *
     * @param yaml The serialized stock and demand configuration.
     * @param version The snapshot version; writes proceed only when newer than the last written.
     */
    private fun writeStockFile(
        yaml: String,
        version: Long,
    ) {
        synchronized(writeLock) {
            if (version <= writtenVersion) return
            writtenVersion = version
            runCatching {
                instance.dataFolder.mkdirs()
                Files.writeString(stockTempFile.toPath(), yaml)
                try {
                    Files.move(
                        stockTempFile.toPath(),
                        stockFile.toPath(),
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(stockTempFile.toPath(), stockFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            }.onFailure {
                stockTempFile.delete()
                instance.logger.warning("Failed to save wandering trader stock: ${it.message}")
            }
        }
    }

    /**
     * A stocked item variant: a normalized single-item [template] preserving full item meta, plus
     * the number of individual items in [count].
     */
    private class StockEntry(
        val template: ItemStack,
        var count: Int,
    )

    /**
     * A currency the trader accepts, ordered by ascending value. Prices are tracked in credit
     * points and converted at [points] per item; payment and payouts use any mix of currencies.
     */
    private enum class Currency(
        val material: Material,
        val points: Int,
    ) {
        COPPER(Material.COPPER_INGOT, 1),
        GOLD(Material.GOLD_INGOT, 2),
        DIAMOND(Material.DIAMOND, 8),
        EMERALD(Material.EMERALD, 16),
        ;

        companion object {
            private val BY_MATERIAL = entries.associateBy { it.material }

            /** Returns the [Currency] for [material], or null when the material is not a currency. */
            fun byMaterial(material: Material): Currency? = BY_MATERIAL[material]
        }
    }
}
