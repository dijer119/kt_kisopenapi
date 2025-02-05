package io.github.devngho.kisopenapi.requests

import io.github.devngho.kisopenapi.KisOpenApi
import io.github.devngho.kisopenapi.requests.util.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class OrderSell(override val client: KisOpenApi):
    DataRequest<OrderBuy.OrderData, OrderBuy.OrderResponse> {
    private val url = if (client.isDemo) "https://openapivts.koreainvestment.com:29443/uapi/domestic-stock/v1/trading/order-cash"
    else               "https://openapi.koreainvestment.com:9443/uapi/domestic-stock/v1/trading/order-cash"

    override suspend fun call(data: OrderBuy.OrderData): OrderBuy.OrderResponse {
        if (data.corp == null) data.corp = client.corp

        val res = client.httpClient.post(url) {
            auth(client)
            tradeId(if(client.isDemo) "VTTC0801U" else "TTTC0801U")
            stock(data.stockCode)
            data.corp?.let { corporation(it) }
            setBody(
                OrderBuy.OrderDataJson(
                    client.account!![0],
                    client.account!![1],
                    data.stockCode,
                    data.orderType,
                    data.count,
                    data.price
                )
            )
        }
        return res.body<OrderBuy.OrderResponse>().apply {
            if (this.errorCode != null) throw RequestError(this.errorDescription)

            res.headers.forEach { s, strings ->
                when(s) {
                    "tr_id" -> this.tradeId = strings[0]
                    "tr_cont" -> this.tradeContinuous = strings[0]
                    "gt_uid" -> this.globalTradeID = strings[0]
                }
            }

            if (this.tradeContinuous == "F" || this.tradeContinuous == "M") {
                this.next = {
                    call(data.copy(tradeContinuous = "N"))
                }
            }
        }
    }
}