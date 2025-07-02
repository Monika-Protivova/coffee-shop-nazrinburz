package com.motycka.edu.order

import com.motycka.edu.menu.MenuItemDTO
import java.math.BigDecimal

object PriceCalculator {

    fun calculatePrice(menuItems: List<MenuItemDTO>, discountInPercent: Double, orderItems: List<OrderItemDTO> = emptyList()): Double {
        val priceMap= menuItems.associateBy { it.id }

        val originalPrice = orderItems.sumOf { orderItem ->
            val price = priceMap[orderItem.menuItemId] ?.price?: 0.0
            price * orderItem.quantity
        }

        val validDiscount = discountInPercent.coerceIn(0.0, 100.0)
        val discountMultiplier = 1.0 - (validDiscount / 100.0)

        val finalPrice = originalPrice * discountMultiplier

        return BigDecimal(finalPrice).setScale(2, RoundingMode.HALF_UP).toDouble()
}
