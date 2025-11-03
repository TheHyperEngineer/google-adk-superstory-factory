package com.hyper_engineer.superstoryfactory.agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;

@Component
public class StockTickerAgent {

    private static final Random RANDOM = new Random();

    public BaseAgent getAgent() {
        return LlmAgent.builder()
                .name("stock_ticker_agent")
                .description("Provides the current stock price for a given company ticker symbol. Use this for any questions about stock prices.")
                .instruction("""
                        You are a stock market analyst. When a user asks for the price of a stock,
                        use the `lookup_stock_ticker` tool to get the information.
                        Then, report the result back to the user in a clear, human-readable sentence.
                        For example: "The current price of AAPL is $250.75."
                        If there is an error, report that error to the user.
                        """)
                .model("gemini-2.5-flash")
                .tools(FunctionTool.create(StockTickerAgent.class, "lookupStockTicker"))
                .build();
    }

    @Schema(name = "lookup_stock_ticker", description = "Looks up the current stock price for a given company ticker symbol (e.g., 'GOOG', 'AAPL').")
    public static Map<String, String> lookupStockTicker(
            @Schema(name = "ticker", description = "The stock ticker symbol of the company.")
            String ticker) {
        // This is a mock implementation.
        if (ticker.equalsIgnoreCase("FAIL")) {
            return Map.of("error", "Could not retrieve data for the specified ticker.");
        } else {
            return Map.of(
                    "ticker", ticker.toUpperCase(),
                    "price", String.format("%.2f", 100 + (RANDOM.nextDouble() * 900))
            );
        }
    }
}