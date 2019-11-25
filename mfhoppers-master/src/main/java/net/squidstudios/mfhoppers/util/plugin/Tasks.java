package net.squidstudios.mfhoppers.util.plugin;

import net.squidstudios.mfhoppers.MFHoppers;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;


public class Tasks {
    private MFHoppers MFHoppers;

    public static Tasks getInstance() {
        return instance;
    }

    private static Tasks instance;
    public Tasks(MFHoppers pl){

        this.MFHoppers = pl;
        instance = this;

    }
    public BukkitTask runTask(Runnable runnable){


       return Bukkit.getScheduler().runTask(MFHoppers,runnable);

    }
    public BukkitTask runTaskAsync(Runnable runnable){

        return Bukkit.getScheduler().runTaskAsynchronously(MFHoppers, runnable);

    }
    public BukkitTask runTaskTimerAsync(Runnable runnable, int delay){

        return Bukkit.getScheduler().runTaskTimerAsynchronously(MFHoppers, runnable, delay,delay);

    }
    public <T> void runTask(Callable<T> callable, Consumer<Future<T>> consumer) {
        FutureTask<T> task = new FutureTask<>(callable);

        new BukkitRunnable() {

            @Override
            public void run() {
                try {
                    task.run();
                    new BukkitRunnable() {

                        @Override
                        public void run() {
                            try {
                                consumer.accept(task);

                            } catch (Exception ex){
                                ex.printStackTrace();
                            }
                        }
                    }.runTaskAsynchronously(MFHoppers);
                } catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }.runTask(MFHoppers);
    }
    public BukkitTask runTaskLater(Runnable runnable, int delay){

        return Bukkit.getScheduler().runTaskLater(MFHoppers, runnable,delay);

    }
}
