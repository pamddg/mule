/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.lang.System.lineSeparator;
import static java.lang.System.nanoTime;
import static java.util.Collections.synchronizedSet;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.mule.runtime.core.api.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Proxy for a {@link ScheduledExecutorService} that adds tracking of the source of the dispatched tasks.
 *
 * @since 4.0
 */
public class DefaultScheduler extends AbstractExecutorService implements Scheduler {

  private final ExecutorService executor;
  private final ScheduledExecutorService scheduledExecutor;

  private Set<SchedulerRunnableDecorator<?>> currentTasks = synchronizedSet(new HashSet<>());

  private volatile boolean shutdown = false;

  /**
   * Wait condition to support awaitTermination
   */
  private final CountDownLatch terminationLatch = new CountDownLatch(1);

  /**
   * @param executor the actual executor that will run the dispatched tasks.
   * @param scheduledExecutor the executor that will handle the delayed/periodic tasks. This will not execute the actual tasks,
   *        but will dispatch it to the {@code executor} at the appropriate time.
   */
  public DefaultScheduler(ExecutorService executor, ScheduledExecutorService scheduledExecutor) {
    this.executor = executor;
    this.scheduledExecutor = scheduledExecutor;
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    checkShutdown();
    requireNonNull(command);

    final RunnableFuture<?> task = newTaskFor(command, null);
    return scheduledExecutor.schedule(() -> executor.execute(task), delay, unit);
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    checkShutdown();
    requireNonNull(callable);

    final RunnableFuture<V> task = newTaskFor(callable);
    return (ScheduledFuture<V>) scheduledExecutor.schedule(() -> executor.execute(task), delay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    checkShutdown();
    requireNonNull(command);

    final RunnableFuture<?> task = newTaskFor(command, null);
    return scheduledExecutor.scheduleAtFixedRate(() -> executor.execute(task), initialDelay, period, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    checkShutdown();
    requireNonNull(command);

    final RunnableFuture<?> task = newTaskFor(command, null);
    return scheduledExecutor.scheduleWithFixedDelay(() -> executor.execute(task), initialDelay, delay, unit);
  }

  @Override
  public void shutdown() {
    this.shutdown = true;
    tryTerminate();
  }

  @Override
  public List<Runnable> shutdownNow() {
    shutdown();

    List<Runnable> tasks;
    try {
      synchronized (currentTasks) {
        tasks = new ArrayList<>(currentTasks.size());
        for (SchedulerRunnableDecorator<?> task : currentTasks) {
          task.stop();
          if (!task.isStarted()) {
            tasks.add(task);
          }
        }
        currentTasks.clear();

        return tasks;
      }
    } finally {
      tryTerminate();
    }
  }

  @Override
  public boolean isShutdown() {
    return shutdown || executor.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return shutdown && currentTasks.isEmpty();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    final long stopNanos = nanoTime() + unit.toNanos(timeout);
    while (nanoTime() <= stopNanos) {
      if (isTerminated()) {
        return true;
      }
      // Do this in a while just in case that the termination occurred right before this next line is executed.
      if (terminationLatch.await(50, MILLISECONDS)) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
    final SchedulerRunnableDecorator<T> decorated = new SchedulerRunnableDecorator<>(super.newTaskFor(callable), this);
    currentTasks.add(decorated);
    return decorated;
  }

  @Override
  protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
    final SchedulerRunnableDecorator<T> decorated = new SchedulerRunnableDecorator<>(super.newTaskFor(runnable, value), this);
    currentTasks.add(decorated);
    return decorated;
  }

  @Override
  public void execute(Runnable command) {
    checkShutdown();
    requireNonNull(command);

    executor.execute(command);
  }

  protected void checkShutdown() {
    if (isShutdown()) {
      throw new RejectedExecutionException(this.toString() + " already shutdown");
    }
  }

  protected void taskFinished(BaseSchedulerTaskDecorator task) {
    currentTasks.remove(task);
    tryTerminate();
  }

  private void tryTerminate() {
    if (isTerminated()) {
      terminationLatch.countDown();
    }
  }

  @Override
  public String toString() {
    return super.toString() + "{" + lineSeparator()
        + "  executor: " + executor.toString() + lineSeparator()
        + "  shutdown: " + shutdown + lineSeparator()
        + "}";
  }
}
