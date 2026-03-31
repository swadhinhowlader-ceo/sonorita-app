package com.sonorita.assistant.business

import com.sonorita.assistant.ai.AIEngine
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

class AIBusinessPartner(private val aiEngine: AIEngine) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    data class MarketAnalysis(
        val asset: String,
        val price: Double,
        val change24h: Double,
        val prediction: String,
        val confidence: Float
    )

    data class Portfolio(
        val holdings: MutableList<Holding> = mutableListOf()
    )

    data class Holding(
        val asset: String,
        val quantity: Double,
        val buyPrice: Double,
        val currentPrice: Double = 0.0
    ) {
        val profitLoss: Double get() = (currentPrice - buyPrice) * quantity
        val profitLossPercent: Double get() = ((currentPrice - buyPrice) / buyPrice) * 100
    }

    private val portfolio = Portfolio()
    private val priceAlerts = mutableListOf<PriceAlert>()

    data class PriceAlert(
        val asset: String,
        val targetPrice: Double,
        val direction: String, // "above" or "below"
        val isActive: Boolean = true
    )

    suspend fun getMarketAnalysis(asset: String): MarketAnalysis {
        return try {
            val response = aiEngine.query(
                "Analyze the current market for $asset. " +
                "Give a brief prediction for today. " +
                "Include: current price estimate, 24h trend, and your confidence level (0-1). " +
                "Format: Price: [number] Change: [number]% Prediction: [text] Confidence: [number]",
                emptyList()
            )

            val prediction = aiEngine.query(
                "Based on current market conditions, what's your prediction for $asset in the next 24 hours? " +
                "Be concise. Bullish or Bearish? Why?",
                emptyList()
            )

            MarketAnalysis(
                asset = asset,
                price = 0.0, // Would need real API
                change24h = 0.0,
                prediction = prediction.content,
                confidence = 0.6f
            )
        } catch (e: Exception) {
            MarketAnalysis(asset, 0.0, 0.0, "Analysis failed: ${e.message}", 0f)
        }
    }

    fun addToPortfolio(asset: String, quantity: Double, buyPrice: Double) {
        portfolio.holdings.add(Holding(asset, quantity, buyPrice))
    }

    fun getPortfolioSummary(): String {
        if (portfolio.holdings.isEmpty()) return "Portfolio empty. 'portfolio add [asset] [qty] [price]' bolo."

        val totalValue = portfolio.holdings.sumOf { it.quantity * it.currentPrice }
        val totalPL = portfolio.holdings.sumOf { it.profitLoss }

        return buildString {
            appendLine("💼 Portfolio Summary:")
            portfolio.holdings.forEach { holding ->
                val pl = if (holding.profitLoss >= 0) "📈" else "📉"
                appendLine("$pl ${holding.asset}: ${holding.quantity} @ ${holding.buyPrice} → ${holding.currentPrice} (${String.format("%.1f", holding.profitLossPercent)}%)")
            }
            appendLine()
            appendLine("💰 Total Value: $totalValue")
            appendLine("📊 Total P/L: ${if (totalPL >= 0) "+" else ""}$totalPL")
        }
    }

    fun setPriceAlert(asset: String, targetPrice: Double, direction: String) {
        priceAlerts.add(PriceAlert(asset, targetPrice, direction))
    }

    fun checkAlerts(currentPrices: Map<String, Double>): List<String> {
        val triggered = mutableListOf<String>()

        priceAlerts.filter { it.isActive }.forEach { alert ->
            val price = currentPrices[alert.asset] ?: return@forEach
            val hit = when (alert.direction) {
                "above" -> price >= alert.targetPrice
                "below" -> price <= alert.targetPrice
                else -> false
            }

            if (hit) {
                triggered.add("🔔 ${alert.asset} is now ${alert.direction} ${alert.targetPrice}! Current: $price")
            }
        }

        return triggered
    }

    suspend fun getStockAnalysis(symbol: String): String {
        return try {
            val response = aiEngine.query(
                "Analyze stock: $symbol. Give brief: current trend, key factors, recommendation (buy/hold/sell). " +
                "Be concise and practical. Consider recent news and technical indicators.",
                emptyList()
            )
            "📊 $symbol Analysis:\n${response.content}"
        } catch (e: Exception) {
            "Analysis failed: ${e.message}"
        }
    }

    suspend fun getBusinessIdea(topic: String): String {
        return try {
            val response = aiEngine.query(
                "Generate 3 innovative business ideas related to: $topic. " +
                "For each: describe the idea, target market, revenue model, and first 3 steps to start. " +
                "Be practical and creative.",
                emptyList()
            )
            "💡 Business Ideas for '$topic':\n${response.content}"
        } catch (e: Exception) {
            "Idea generation failed: ${e.message}"
        }
    }

    suspend fun analyzeCompetitor(company: String): String {
        return try {
            val response = aiEngine.query(
                "Analyze the competitive landscape for: $company. " +
                "Who are their main competitors? What are their strengths and weaknesses? " +
                "What opportunities exist?",
                emptyList()
            )
            "🔍 Competitor Analysis for '$company':\n${response.content}"
        } catch (e: Exception) {
            "Competitor analysis failed: ${e.message}"
        }
    }
}
