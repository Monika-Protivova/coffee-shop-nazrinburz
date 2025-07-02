package com.motycka.edu.order

import com.motycka.edu.customer.InternalCustomerService
import com.motycka.edu.db.DatabaseFactory.dbQuery
import com.motycka.edu.menu.MenuRepository
import com.motycka.edu.user.UserId
import com.motycka.edu.menu.MenuItemDTO
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val menuRepository: MenuRepository,
    private val customerService: InternalCustomerService
) {

    suspend fun createOrder(request: OrderRequest): OrderResponse {
        logger.info { "Creating new order for customerId: ${request.customerId}" }

        val menuItemIds = request.items.map { it.menuItemId }
        val menuItems = dbQuery { menuRepository.selectMenuItems(ids = menuItemIds) }
        if (menuItems.size != menuItemIds.size) {
            throw IllegalArgumentException("One or more menu items not found.")
        }

        val discountPercent = dbQuery { customerService.getDiscountPercent(UserId(request.customerId)) }
        val totalPrice = PriceCalculator.calculatePrice(menuItems, discountPercent, request.items)

        val newOrderDto = OrderDTO(id = null, customerId = request.customerId, status = OrderStatus.PENDING)
        val createdOrder = dbQuery { orderRepository.create(newOrderDto) }
        val orderId = createdOrder.id ?: throw IllegalStateException("Created order must have an ID")

        val orderItemsToCreate = request.items.map {
            OrderItemDTO(id = null, orderId = orderId, menuItemId = MenuItemId(it.menuItemId), quantity = it.quantity)
        }
        dbQuery { orderItemRepository.createOrderItems(orderItemsToCreate) }

        return findOrderById(orderId) ?: throw IllegalStateException("Could not find newly created order.")
    }

    suspend fun findOrderById(id: OrderId): OrderResponse? {
        logger.info { "Finding order with id: ${id.value}" }
        val orderDto = dbQuery { orderRepository.selectById(id) } ?: return null
        return buildFullOrderResponse(orderDto)
    }

    suspend fun findAllOrders(): List<OrderResponse> {
        logger.info { "Finding all orders" }
        val allOrderDtos = dbQuery { orderRepository.selectAll() }
        return allOrderDtos.mapNotNull { buildFullOrderResponse(it) }
    }

    suspend fun updateOrderStatus(id: OrderId, request: OrderUpdateRequest): OrderResponse? {
        logger.info { "Updating status for order id: ${id.value} to ${request.status}" }
        val orderDto = dbQuery { orderRepository.selectById(id) } ?: return null
        val updatedDto = orderDto.copy(status = request.status)
        dbQuery { orderRepository.update(updatedDto) }

        return findOrderById(id)
    }


    private suspend fun buildFullOrderResponse(orderDto: OrderDTO): OrderResponse? {
        val orderId = orderDto.id ?: return null

        val orderItems = dbQuery { orderItemRepository.selectByOrderId(orderId) }
        val menuItemIds = orderItems.map { it.menuItemId.value }
        val menuItems = dbQuery { menuRepository.selectMenuItems(ids = menuItemIds) }


        if (menuItems.size != menuItemIds.distinct().size) {
            logger.error { "Could not find all menu items for order ${orderId.value}" }
            return null
        }

        val discountPercent = dbQuery { customerService.getDiscountPercent(UserId(orderDto.customerId)) }
        val requestItems = orderItems.map { OrderRequest.Item(it.menuItemId.value, it.quantity) }
        val totalPrice = PriceCalculator.calculatePrice(menuItems, discountPercent, requestItems)

        val menuItemsById = menuItems.associateBy { it.id!!.value }
        val orderItemResponses = orderItems.map { orderItem ->
            val menuItem = menuItemsById[orderItem.menuItemId.value]!!
            OrderItemResponse(menuItem = menuItem, quantity = orderItem.quantity)
        }

        return OrderResponse(
            id = orderId,
            customerId = orderDto.customerId,
            items = orderItemResponses,
            totalPrice = totalPrice,
            status = orderDto.status,
            isPaid = false
        )
    }
}