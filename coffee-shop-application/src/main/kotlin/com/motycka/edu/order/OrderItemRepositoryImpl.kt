package com.motycka.edu.order

import org.jetbrains.exposed.sql.transactions.transaction

class OrderItemRepositoryImpl: OrderItemRepository{
    override fun selectByOrderId(orderId: OrderId): List<OrderItemDTO> = transaction {
        OrderItemDAO.find { OrderItemTable.orderId eq orderId.value }.map { it.toDTO() }
    }

    override fun createOrderItems(orderItems: List<OrderItemDTO>) = transaction {
        orderItems.forEach { orderItem ->
            OrderItemDAO.new {
                this.orderId = orderItem.orderId.value
                this.productId = orderItem.menuItemId.value
                this.quantity = orderItem.quantity
            }
        }
    }
}