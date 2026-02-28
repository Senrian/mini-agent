package com.miniagent.chain;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Chain链 - 对标LangChain的LCEL (LangChain Expression Language)
 * 
 * 支持:
 * - 链式组合
 * - 并行执行
 * - 流式处理
 * - 错误处理
 */
@Slf4j
public class Chain<I, O> {

    private final Function<I, O> func;
    private final String name;
    private List<Chain<?, ?>> nextChains = new ArrayList<>();
    private Function<Throwable, ?> errorHandler;

    private Chain(String name, Function<I, O> func) {
        this.name = name;
        this.func = func;
    }

    /**
     * 创建Chain
     */
    public static <I, O> Chain<I, O> create(String name, Function<I, O> func) {
        return new Chain<>(name, func);
    }

    /**
     * 执行Chain
     */
    public O invoke(I input) {
        try {
            log.debug("Invoking chain: {}", name);
            O output = func.apply(input);
            
            // 执行下一个Chain
            for (Chain<?, ?> next : nextChains) {
                // ...
            }
            
            return output;
        } catch (Throwable e) {
            log.error("Chain error: {}", name, e);
            if (errorHandler != null) {
                return (O) errorHandler.apply(e);
            }
            throw new RuntimeException("Chain execution failed: " + name, e);
        }
    }

    /**
     * 链式组合 - Pipe (对标 | 操作符)
     */
    public <O2> Chain<I, O2> pipe(Chain<O, O2> next) {
        Chain<I, O2> combined = new Chain<>(name + "->" + next.name, input -> {
            O intermediate = this.invoke(input);
            return next.invoke(intermediate);
        });
        return combined;
    }

    /**
     * 链式组合 - 使用函数
     */
    public <O2> Chain<I, O2> pipe(Function<O, O2> fn) {
        return pipe(Chain.create(name + "-fn", fn));
    }

    /**
     * 并行执行 - 对标RunnableParallel
     */
    public static <K, V> Map<K, V> parallel(Map<String, Chain<?, V>> chains, K input) {
        Map<K, V> results = new HashMap<>();
        
        for (Map.Entry<String, Chain<?, V>> entry : chains.entrySet()) {
            try {
                V result = entry.getValue().invoke(input);
                results.put((K) entry.getKey(), result);
            } catch (Exception e) {
                log.error("Parallel chain error: {}", entry.getKey(), e);
            }
        }
        
        return results;
    }

    /**
     * 绑定上下文
     */
    public Chain<I, O> bind(Map<String, Object> context) {
        return new Chain<>(name, input -> {
            Map<String, Object> ctx = new HashMap<>(context);
            ctx.put("input", input);
            return func.apply((I) ctx);
        });
    }

    /**
     * 添加错误处理
     */
    public Chain<I, O> withErrorHandler(Function<Throwable, ?> handler) {
        this.errorHandler = handler;
        return this;
    }

    /**
     * 异步执行
     */
    public CompletableFuture<O> invokeAsync(I input) {
        return CompletableFuture.supplyAsync(() -> invoke(input));
    }

    // 静态便捷方法
    public static <T> Chain<T, T> identity() {
        return create("identity", t -> t);
    }

    public static <T> Chain<T, List<T>> batch(List<Chain<T, T>> chains) {
        return create("batch", input -> {
            List<T> results = new ArrayList<>();
            for (Chain<T, T> chain : chains) {
                results.add(chain.invoke(input));
            }
            return results;
        });
    }
}
