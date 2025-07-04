package com.motycka.edu.order

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private val logger = KotlinLogging.logger {}

private const val ORDER_NOT_FOUND = "Order not found"
private const val INVALID_ID = "Invalid ID format"

fun Route.orderRoutes(
    orderService: OrderService,
    basePath: String
) {
        route("$basePath/orders") {

            post {
                val request = call.receive<OrderRequest>()
                val orderResponse = orderService.createOrder(request)
                call.respond(HttpStatusCode.Created, orderResponse)
            }


            get {
                val orders = orderService.findAllOrders()
                call.respond(HttpStatusCode.OK,  orders )
            }


            get("{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, INVALID_ID)
                    return@get
                }
                val order = orderService.findOrderById(OrderId(id))
                if (order != null) {
                    call.respond(HttpStatusCode.OK, order)
                } else {
                    call.respond(HttpStatusCode.NotFound, ORDER_NOT_FOUND)
                }
            }


            put("{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, INVALID_ID)
                    return@put
                }
                val request = call.receive<OrderUpdateRequest>()
                val updatedOrder = orderService.updateOrderStatus(OrderId(id), request)
                if (updatedOrder != null) {
                    call.respond(HttpStatusCode.OK, updatedOrder)
                } else {
                    call.respond(HttpStatusCode.NotFound, ORDER_NOT_FOUND)
                }
            }
        }


}
