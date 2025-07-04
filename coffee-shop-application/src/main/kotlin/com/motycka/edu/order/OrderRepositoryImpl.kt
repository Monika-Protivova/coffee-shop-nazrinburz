package com.motycka.edu.order

import org.jetbrains.exposed.sql.transactions.transaction


class OrderRepositoryImpl: OrderRepository{
    override fun selectAll(): List<OrderDTO>=transaction {
        OrderDAO.all().map { it.toDTO() }
    }

    override fun selectById(id: OrderId): OrderDTO? = transaction {
        OrderDAO.findById(id.value)?.toDTO()
    }

    override fun create(order: OrderDTO): OrderDTO = transaction {
        OrderDAO.new {
            this.customerId = order.customerId
            this.status = order.status
        }.toDTO()
    }

    override fun update(order: OrderDTO): OrderDTO = transaction {
        val orderIdValue = order.id?.value ?: throw IllegalArgumentException("Order ID is required for an update")

        val orderDAO = OrderDAO.findById(orderIdValue)
            ?: throw NoSuchElementException("Order with id $orderIdValue not found")

        orderDAO.apply {
            this.status = order.status
        }.toDTO()
    }
}