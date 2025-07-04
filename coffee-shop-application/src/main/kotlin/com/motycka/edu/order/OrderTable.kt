package com.motycka.edu.order

import com.motycka.edu.customer.CustomerDAO
import com.motycka.edu.customer.CustomerTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable


object OrderTable : LongIdTable("orders") {
    val customerId = reference("customer_id", CustomerTable)
    val status = enumerationByName("status", 50, OrderStatus::class)
}


class OrderDAO(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<OrderDAO>(OrderTable)

    var customer by CustomerDAO referencedOn OrderTable.customerId
    var status by OrderTable.status

    // The relationship to OrderItemDAO is still needed to calculate the price later.
    val items by OrderItemDAO.referrersOn(OrderItemTable.orderId)

    fun toDTO(): OrderDTO {
        return OrderDTO(
            id = OrderId(this.id.value),
            customerId = this.customer.id.value,
            status = this.status
        )
    }
}