package net.squidstudios.mfhoppers.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

public class OFuture<O> extends CompletableFuture<O> {

    private O object;
    private BiConsumer<? super O, ? super Throwable> action;

    @Override
    public boolean complete(O value) {
        this.object = value;

        if (action != null)
            action.accept(value, null);
        return true;
    }

    @Override
    public CompletableFuture<O> whenComplete(BiConsumer<? super O, ? super Throwable> action) {
        this.action = action;
        if (object != null)
            action.accept(object, null);

        return this;
    }

    @Override
    public O get() {
        return object;
    }
}
