/**
 * Copyright (c) 2013-2019 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.redisson;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;

import org.redisson.api.RFuture;
import org.redisson.api.RLock;
import org.redisson.client.RedisException;
import org.redisson.client.codec.LongCodec;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.client.protocol.RedisCommand.ValueType;
import org.redisson.client.protocol.RedisCommands;
import org.redisson.client.protocol.RedisStrictCommand;
import org.redisson.client.protocol.convertor.IntegerReplayConvertor;
import org.redisson.command.CommandAsyncExecutor;
import org.redisson.misc.RPromise;
import org.redisson.misc.RedissonPromise;
import org.redisson.pubsub.LockPubSub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.internal.PlatformDependent;

/**
 * Distributed implementation of {@link java.util.concurrent.locks.Lock}
 * Implements reentrant lock.<br>
 * Lock will be removed automatically if client disconnects.
 * <p>
 * Implements a <b>non-fair</b> locking so doesn't guarantees an acquire order.
 *
 * @author Nikita Koksharov
 *
 */
public class RedissonLock extends RedissonExpirable implements RLock {

    public static class ExpirationEntry {
        
        private long threadId;
        private Timeout timeout;
        
        public ExpirationEntry(long threadId, Timeout timeout) {
            super();
            this.threadId = threadId;
            this.timeout = timeout;
        }
        
        public long getThreadId() {
            return threadId;
        }
        
        public Timeout getTimeout() {
            return timeout;
        }
        
    }
    
    private static final Logger log = LoggerFactory.getLogger(RedissonLock.class);
    
    private static final ConcurrentMap<String, ExpirationEntry> expirationRenewalMap = PlatformDependent.newConcurrentHashMap();
    protected long internalLockLeaseTime;

    final UUID id;
    final String entryName;

    protected static final LockPubSub PUBSUB = new LockPubSub();

    final CommandAsyncExecutor commandExecutor;

    public RedissonLock(CommandAsyncExecutor commandExecutor, String name) {
        super(commandExecutor, name);
        this.commandExecutor = commandExecutor;
        this.id = commandExecutor.getConnectionManager().getId();
        this.internalLockLeaseTime = commandExecutor.getConnectionManager().getCfg().getLockWatchdogTimeout();
        this.entryName = id + ":" + name;
    }

    protected String getEntryName() {
        return entryName;
    }

    String getChannelName() {
        return prefixName("redisson_lock__channel", getName());
    }

    protected String getLockName(long threadId) {
        return id + ":" + threadId;
    }

    @Override
    public void lock() {
        try {
            lockInterruptibly();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void lock(long leaseTime, TimeUnit unit) {
        try {
            lockInterruptibly(leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 可中断的获取锁
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {
        lockInterruptibly(-1, null);
    }

    @Override
    public void lockInterruptibly(long leaseTime, TimeUnit unit) throws InterruptedException {
        long threadId = Thread.currentThread().getId();
        // 尝试获取锁
        Long ttl = tryAcquire(leaseTime, unit, threadId);
        // lock acquired
        // 获取到了锁，直接返回
        if (ttl == null) {
            return;
        }

        // 如果没有获取到锁，则基于Redis的发布订阅机制订阅一下，等锁被释放了，则会回调这些订阅的客户端
        RFuture<RedissonLockEntry> future = subscribe(threadId);
        commandExecutor.syncSubscription(future);

        try {
            while (true) {
                // 尝试获取锁
                ttl = tryAcquire(leaseTime, unit, threadId);
                // lock acquired
                // 获取到了锁则直接返回
                if (ttl == null) {
                    break;
                }

                // waiting for message
                // 如果没有获取到锁，则看看该锁的过期时间是否大于0
                if (ttl >= 0) {
                    // getEntry(threadId).getLatch()获取的是一个信号量（Semaphore），这里通过AQS的方式获取锁
                    getEntry(threadId).getLatch().tryAcquire(ttl, TimeUnit.MILLISECONDS);
                } else {
                    // 如果不是大于0，则说明该锁已经被释放了，再次发起加锁请求
                    getEntry(threadId).getLatch().acquire();
                }
            }
        } finally {
            // 最后取消订阅
            unsubscribe(future, threadId);
        }
//        get(lockAsync(leaseTime, unit));
    }

    /**
     * 尝试获取锁，如果没有获取到锁则返回该锁还剩余多少毫秒过期，如果获取到了锁，则返回空
     */
    private Long tryAcquire(long leaseTime, TimeUnit unit, long threadId) {
        // tryAcquireAsync方法返回一个RFuture<Long>类型，get方法主要就是取得RFuture中的数值
        // 该数值就是该锁还剩余的过期时间（如果为空，则表示已经获取到锁了，反之则表示该锁还剩多久过期）
        return get(tryAcquireAsync(leaseTime, unit, threadId));
    }
    
    private RFuture<Boolean> tryAcquireOnceAsync(long leaseTime, TimeUnit unit, final long threadId) {
        if (leaseTime != -1) {
            return tryLockInnerAsync(leaseTime, unit, threadId, RedisCommands.EVAL_NULL_BOOLEAN);
        }
        RFuture<Boolean> ttlRemainingFuture = tryLockInnerAsync(commandExecutor.getConnectionManager().getCfg().getLockWatchdogTimeout(), TimeUnit.MILLISECONDS, threadId, RedisCommands.EVAL_NULL_BOOLEAN);
        ttlRemainingFuture.addListener(new FutureListener<Boolean>() {
            @Override
            public void operationComplete(Future<Boolean> future) throws Exception {
                if (!future.isSuccess()) {
                    return;
                }

                Boolean ttlRemaining = future.getNow();
                // lock acquired
                if (ttlRemaining) {
                    scheduleExpirationRenewal(threadId);
                }
            }
        });
        return ttlRemainingFuture;
    }

    private <T> RFuture<Long> tryAcquireAsync(long leaseTime, TimeUnit unit, final long threadId) {
        // 1、如果有设置锁过期时间
        if (leaseTime != -1) {
            // 调用tryLockInnerAsync，通过lua脚本去加锁
            return tryLockInnerAsync(leaseTime, unit, threadId, RedisCommands.EVAL_LONG);
        }
        // 2、如果获取锁时没有传递锁过期时间，则这里会给个默认过期时间30s（通过执行lua脚本去获取锁）
        RFuture<Long> ttlRemainingFuture = tryLockInnerAsync(commandExecutor.getConnectionManager().getCfg().getLockWatchdogTimeout(), TimeUnit.MILLISECONDS, threadId, RedisCommands.EVAL_LONG);
        // 3、给获取锁的操作添加一个监听，当获取锁的操作返回时（不管成功还是失败），立即调用监听方法
        ttlRemainingFuture.addListener(new FutureListener<Long>() {
            // 当获取锁的操作执行结束时，该方法被吊起
            @Override
            public void operationComplete(Future<Long> future) throws Exception {
                // 3.1、如果获取锁失败，则直接返回
                if (!future.isSuccess()) {
                    return;
                }

                Long ttlRemaining = future.getNow();
                // lock acquired
                // ttlRemaining == null 则说明获取锁成功
                if (ttlRemaining == null) {
                    // 3.2、如果获取锁成功了，则开启定时任务去定时延长锁过期时间（看门狗）
                    scheduleExpirationRenewal(threadId);
                }
            }
        });
        return ttlRemainingFuture;
    }

    @Override
    public boolean tryLock() {
        return get(tryLockAsync());
    }

    private void scheduleExpirationRenewal(final long threadId) {
        if (expirationRenewalMap.containsKey(getEntryName())) {
            return;
        }

        Timeout task = commandExecutor.getConnectionManager().newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                
                RFuture<Boolean> future = renewExpirationAsync(threadId);
                
                future.addListener(new FutureListener<Boolean>() {
                    @Override
                    public void operationComplete(Future<Boolean> future) throws Exception {
                        expirationRenewalMap.remove(getEntryName());
                        if (!future.isSuccess()) {
                            log.error("Can't update lock " + getName() + " expiration", future.cause());
                            return;
                        }
                        
                        if (future.getNow()) {
                            // reschedule itself
                            scheduleExpirationRenewal(threadId);
                        }
                    }
                });
            }

        }, internalLockLeaseTime / 3, TimeUnit.MILLISECONDS);

        if (expirationRenewalMap.putIfAbsent(getEntryName(), new ExpirationEntry(threadId, task)) != null) {
            task.cancel();
        }
    }

    protected RFuture<Boolean> renewExpirationAsync(long threadId) {
        return commandExecutor.evalWriteAsync(getName(), LongCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN,
                "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
                    "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                    "return 1; " +
                "end; " +
                "return 0;",
            Collections.<Object>singletonList(getName()), 
            internalLockLeaseTime, getLockName(threadId));
    }

    void cancelExpirationRenewal(Long threadId) {
        ExpirationEntry task = expirationRenewalMap.get(getEntryName());
        if (task != null && (threadId == null || task.getThreadId() == threadId)) {
            expirationRenewalMap.remove(getEntryName());
            task.getTimeout().cancel();
        }
    }

    <T> RFuture<T> tryLockInnerAsync(long leaseTime, TimeUnit unit, long threadId, RedisStrictCommand<T> command) {
        // 将锁过期时间转换为毫秒
        internalLockLeaseTime = unit.toMillis(leaseTime);

        // 通过lua脚本去获取锁（可重入锁）
        // pttl命令和ttl命令类似，只是他是以毫秒为单位返回剩余过期时间，ttl是以秒为单位
        return commandExecutor.evalWriteAsync(getName(), LongCodec.INSTANCE, command,
                  "if (redis.call('exists', KEYS[1]) == 0) then " +
                      "redis.call('hset', KEYS[1], ARGV[2], 1); " +
                      "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                      "return nil; " +
                  "end; " +
                  "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
                      "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
                      "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                      "return nil; " +
                  "end; " +
                  "return redis.call('pttl', KEYS[1]);",
                    Collections.<Object>singletonList(getName()), internalLockLeaseTime, getLockName(threadId));
    }
    
    private void acquireFailed(long threadId) {
        get(acquireFailedAsync(threadId));
    }
    
    protected RFuture<Void> acquireFailedAsync(long threadId) {
        return RedissonPromise.newSucceededFuture(null);
    }

    /**
     * 带等待时间的获取锁
     * @param waitTime 等待时间
     * @param leaseTime 锁过期时间
     * @param unit 时间单位
     * @return boolean
     * @author Mr_wenpan@163.com 2021/7/25 9:15 下午
     */
    @Override
    public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
        // 1、将等待时间转换为毫秒、、获取当前时间、获取当前线程ID
        long time = unit.toMillis(waitTime);
        long current = System.currentTimeMillis();
        final long threadId = Thread.currentThread().getId();
        // 2、尝试申请锁，返回还剩余的锁过期时间
        Long ttl = tryAcquire(leaseTime, unit, threadId);
        // 3、ttl==null 表示获取锁成功则直接返回true
        // lock acquired
        if (ttl == null) {
            return true;
        }

        // 4、获取还需要等待的时间，且根据还需等待的时间（time）判断是否获取锁失败
        time -= (System.currentTimeMillis() - current);
        // 如果还需要等待的时间为0，则说明获取锁已经失败了
        // 申请锁的耗时如果大于等于最大等待时间，则申请锁失败
        if (time <= 0) {
            acquireFailed(threadId);
            return false;
        }

        // 重新获取当前时间
        current = System.currentTimeMillis();
        // 5、上面第一次尝试获取锁失败，且还没有超出最大等待时间的基础上，基于Redis的发布订阅机制,订阅锁释放事件
        final RFuture<RedissonLockEntry> subscribeFuture = subscribe(threadId);
        /*
         * 6、基于Redis的发布订阅机制,订阅锁释放事件，并通过await方法阻塞等待锁释放，有效的解决了无效的锁申请浪费资源的问题：
         * 基于信号量，当锁被其它资源占用时，当前线程通过 Redis 的 channel 订阅锁的释放事件，一旦锁释放会发消息通知待等待的线程进行竞争
         * 当 this.await返回false，说明等待时间已经超出获取锁最大等待时间，取消订阅并返回获取锁失败
         * 当 this.await返回true，进入下面的循环再次尝试获取锁
         *
         * await是通过CountDownLatch + 监听器机制来实现的，具体看方法内部注释
         */
        if (!await(subscribeFuture, time, TimeUnit.MILLISECONDS)) {
            // 在等待时间耗完的情况下，取消对该锁的订阅
            if (!subscribeFuture.cancel(false)) {
                subscribeFuture.addListener(new FutureListener<RedissonLockEntry>() {
                    @Override
                    public void operationComplete(Future<RedissonLockEntry> future) throws Exception {
                        if (subscribeFuture.isSuccess()) {
                            unsubscribe(subscribeFuture, threadId);
                        }
                    }
                });
            }
            // 获取锁失败
            acquireFailed(threadId);
            return false;
        }

        // 7、如果在等待时间内订阅的锁已经被释放了，则会执行这里
        try {
            // 获取还需要等待的时间
            time -= (System.currentTimeMillis() - current);
            // 如果还需要等待的时间小于0，则说明已经超过最大等待时间，获取锁失败
            if (time <= 0) {
                acquireFailed(threadId);
                return false;
            }

            // 8、能运行到这里则说明：
            // 1、当前还在最大等待时间内
            // 2、并且等待的锁已经被释放（即对该锁的订阅事件已经被吊起过），在这里可以再次尝试获取锁
            // 这是一个死循环，循环退出条件有两个：
            // ①、在最大等待时间内成功获取锁，返回true
            // ②、超出了最大等待时间，但仍然没有成功获取到锁，返回false
            while (true) {
                // 获取当前时间
                long currentTime = System.currentTimeMillis();
                // 8.1、再次尝试申请锁，返回还剩余的锁过期时间
                ttl = tryAcquire(leaseTime, unit, threadId);
                // 8.2、ttl==null 表示获取锁成功则直接返回true
                // lock acquired
                if (ttl == null) {
                    return true;
                }

                // 再次计算还需要等待多时时间
                time -= (System.currentTimeMillis() - currentTime);
                // 8.3、如果还需要等待的时间小于0，则说明已经超过最大等待时间，获取锁失败
                if (time <= 0) {
                    acquireFailed(threadId);
                    return false;
                }

                // waiting for message
                // 更新一下当前时间，因为上面的操作可能会耗时，进而导致下面根据currentTime计算的time不准确
                currentTime = System.currentTimeMillis();
                if (ttl >= 0 && ttl < time) {
                    // 8.4、如果剩余时间(ttl)小于waittime ,就在 ttl 时间内，从Entry的信号量（Semaphore）获取一个许可(除非被中断或者一直没有可用的许可)。
                    getEntry(threadId).getLatch().tryAcquire(ttl, TimeUnit.MILLISECONDS);
                } else {
                    // 8.5、如果该锁剩余过期时间(ttl)大于waittime，则就在waittime 时间范围内等待可以通过信号量（Semaphore）
                    getEntry(threadId).getLatch().tryAcquire(time, TimeUnit.MILLISECONDS);
                }

                // 更新剩余的等待时间(最大等待时间-已经消耗的阻塞时间)
                time -= (System.currentTimeMillis() - currentTime);
                // 8.6、如果还需要等待的时间小于0，则说明已经超过最大等待时间，获取锁失败
                if (time <= 0) {
                    acquireFailed(threadId);
                    return false;
                }
            }
        } finally {
            // 9、无论是否获得锁,都要取消订阅解锁消息
            unsubscribe(subscribeFuture, threadId);
        }
//        return get(tryLockAsync(waitTime, leaseTime, unit));
    }

    protected RedissonLockEntry getEntry(long threadId) {
        return PUBSUB.getEntry(getEntryName());
    }

    protected RFuture<RedissonLockEntry> subscribe(long threadId) {
        return PUBSUB.subscribe(getEntryName(), getChannelName(), commandExecutor.getConnectionManager().getSubscribeService());
    }

    protected void unsubscribe(RFuture<RedissonLockEntry> future, long threadId) {
        PUBSUB.unsubscribe(future.getNow(), getEntryName(), getChannelName(), commandExecutor.getConnectionManager().getSubscribeService());
    }

    @Override
    public boolean tryLock(long waitTime, TimeUnit unit) throws InterruptedException {
        return tryLock(waitTime, -1, unit);
    }

    @Override
    public void unlock() {
        try {
            get(unlockAsync(Thread.currentThread().getId()));
        } catch (RedisException e) {
            if (e.getCause() instanceof IllegalMonitorStateException) {
                throw (IllegalMonitorStateException)e.getCause();
            } else {
                throw e;
            }
        }
        
//        Future<Void> future = unlockAsync();
//        future.awaitUninterruptibly();
//        if (future.isSuccess()) {
//            return;
//        }
//        if (future.cause() instanceof IllegalMonitorStateException) {
//            throw (IllegalMonitorStateException)future.cause();
//        }
//        throw commandExecutor.convertException(future);
    }

    @Override
    public Condition newCondition() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean forceUnlock() {
        return get(forceUnlockAsync());
    }

    @Override
    public RFuture<Boolean> forceUnlockAsync() {
        cancelExpirationRenewal(null);
        return commandExecutor.evalWriteAsync(getName(), LongCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN,
                "if (redis.call('del', KEYS[1]) == 1) then "
                + "redis.call('publish', KEYS[2], ARGV[1]); "
                + "return 1 "
                + "else "
                + "return 0 "
                + "end",
                Arrays.<Object>asList(getName(), getChannelName()), LockPubSub.unlockMessage);
    }

    @Override
    public boolean isLocked() {
        return isExists();
    }

    @Override
    public RFuture<Boolean> isExistsAsync() {
        return commandExecutor.writeAsync(getName(), codec, RedisCommands.EXISTS, getName());
    }

    @Override
    public boolean isHeldByCurrentThread() {
        return isHeldByThread(Thread.currentThread().getId());
    }

    @Override
    public boolean isHeldByThread(long threadId) {
        final RFuture<Boolean> future = commandExecutor.writeAsync(getName(), LongCodec.INSTANCE, RedisCommands.HEXISTS, getName(), getLockName(threadId));
        return get(future);
    }

    private static final RedisCommand<Integer> HGET = new RedisCommand<Integer>("HGET", ValueType.MAP_VALUE, new IntegerReplayConvertor(0));
    
    public RFuture<Integer> getHoldCountAsync() {
        return commandExecutor.writeAsync(getName(), LongCodec.INSTANCE, HGET, getName(), getLockName(Thread.currentThread().getId()));
    }
    
    @Override
    public int getHoldCount() {
        return get(getHoldCountAsync());
    }

    @Override
    public RFuture<Boolean> deleteAsync() {
        return forceUnlockAsync();
    }

    @Override
    public RFuture<Void> unlockAsync() {
        long threadId = Thread.currentThread().getId();
        return unlockAsync(threadId);
    }

    protected RFuture<Boolean> unlockInnerAsync(long threadId) {
        return commandExecutor.evalWriteAsync(getName(), LongCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN,
                "if (redis.call('exists', KEYS[1]) == 0) then " +
                    "redis.call('publish', KEYS[2], ARGV[1]); " +
                    "return 1; " +
                "end;" +
                "if (redis.call('hexists', KEYS[1], ARGV[3]) == 0) then " +
                    "return nil;" +
                "end; " +
                "local counter = redis.call('hincrby', KEYS[1], ARGV[3], -1); " +
                "if (counter > 0) then " +
                    "redis.call('pexpire', KEYS[1], ARGV[2]); " +
                    "return 0; " +
                "else " +
                    "redis.call('del', KEYS[1]); " +
                    "redis.call('publish', KEYS[2], ARGV[1]); " +
                    "return 1; "+
                "end; " +
                "return nil;",
                Arrays.<Object>asList(getName(), getChannelName()), LockPubSub.unlockMessage, internalLockLeaseTime, getLockName(threadId));

    }

    /**
     * RFuture可以理解为对Future的一个增强，netty中的实现
     */
    @Override
    public RFuture<Void> unlockAsync(final long threadId) {
        final RPromise<Void> result = new RedissonPromise<Void>();
        // 具体的释放锁的lua脚本（释放锁的动作在这里完成）
        RFuture<Boolean> future = unlockInnerAsync(threadId);

        // 添加一个监听器，一旦释放锁的操作完成（无论失败或成功），都会吊起监听器的operationComplete方法
        future.addListener(new FutureListener<Boolean>() {
            @Override
            public void operationComplete(Future<Boolean> future) throws Exception {
                // 如果释放锁失败了
                if (!future.isSuccess()) {
                    cancelExpirationRenewal(threadId);
                    result.tryFailure(future.cause());
                    return;
                }

                Boolean opStatus = future.getNow();
                if (opStatus == null) {
                    IllegalMonitorStateException cause = new IllegalMonitorStateException("attempt to unlock lock, not locked by current thread by node id: "
                            + id + " thread-id: " + threadId);
                    result.tryFailure(cause);
                    return;
                }
                if (opStatus) {
                    cancelExpirationRenewal(null);
                }

                // 释放锁成功
                result.trySuccess(null);
            }
        });

        return result;
    }

    @Override
    public RFuture<Void> lockAsync() {
        return lockAsync(-1, null);
    }

    @Override
    public RFuture<Void> lockAsync(long leaseTime, TimeUnit unit) {
        final long currentThreadId = Thread.currentThread().getId();
        return lockAsync(leaseTime, unit, currentThreadId);
    }

    @Override
    public RFuture<Void> lockAsync(long currentThreadId) {
        return lockAsync(-1, null, currentThreadId);
    }
    
    @Override
    public RFuture<Void> lockAsync(final long leaseTime, final TimeUnit unit, final long currentThreadId) {
        final RPromise<Void> result = new RedissonPromise<Void>();
        RFuture<Long> ttlFuture = tryAcquireAsync(leaseTime, unit, currentThreadId);
        ttlFuture.addListener(new FutureListener<Long>() {
            @Override
            public void operationComplete(Future<Long> future) throws Exception {
                if (!future.isSuccess()) {
                    result.tryFailure(future.cause());
                    return;
                }

                Long ttl = future.getNow();

                // lock acquired
                if (ttl == null) {
                    if (!result.trySuccess(null)) {
                        unlockAsync(currentThreadId);
                    }
                    return;
                }

                final RFuture<RedissonLockEntry> subscribeFuture = subscribe(currentThreadId);
                subscribeFuture.addListener(new FutureListener<RedissonLockEntry>() {
                    @Override
                    public void operationComplete(Future<RedissonLockEntry> future) throws Exception {
                        if (!future.isSuccess()) {
                            result.tryFailure(future.cause());
                            return;
                        }

                        lockAsync(leaseTime, unit, subscribeFuture, result, currentThreadId);
                    }

                });
            }
        });

        return result;
    }

    private void lockAsync(final long leaseTime, final TimeUnit unit,
            final RFuture<RedissonLockEntry> subscribeFuture, final RPromise<Void> result, final long currentThreadId) {
        RFuture<Long> ttlFuture = tryAcquireAsync(leaseTime, unit, currentThreadId);
        ttlFuture.addListener(new FutureListener<Long>() {
            @Override
            public void operationComplete(Future<Long> future) throws Exception {
                if (!future.isSuccess()) {
                    unsubscribe(subscribeFuture, currentThreadId);
                    result.tryFailure(future.cause());
                    return;
                }

                Long ttl = future.getNow();
                // lock acquired
                if (ttl == null) {
                    unsubscribe(subscribeFuture, currentThreadId);
                    if (!result.trySuccess(null)) {
                        unlockAsync(currentThreadId);
                    }
                    return;
                }

                final RedissonLockEntry entry = getEntry(currentThreadId);
                if (entry.getLatch().tryAcquire()) {
                    lockAsync(leaseTime, unit, subscribeFuture, result, currentThreadId);
                } else {
                    // waiting for message
                    final AtomicReference<Timeout> futureRef = new AtomicReference<Timeout>();
                    final Runnable listener = new Runnable() {
                        @Override
                        public void run() {
                            if (futureRef.get() != null) {
                                futureRef.get().cancel();
                            }
                            lockAsync(leaseTime, unit, subscribeFuture, result, currentThreadId);
                        }
                    };

                    entry.addListener(listener);

                    if (ttl >= 0) {
                        Timeout scheduledFuture = commandExecutor.getConnectionManager().newTimeout(new TimerTask() {
                            @Override
                            public void run(Timeout timeout) throws Exception {
                                if (entry.removeListener(listener)) {
                                    lockAsync(leaseTime, unit, subscribeFuture, result, currentThreadId);
                                }
                            }
                        }, ttl, TimeUnit.MILLISECONDS);
                        futureRef.set(scheduledFuture);
                    }
                }
            }
        });
    }

    @Override
    public RFuture<Boolean> tryLockAsync() {
        return tryLockAsync(Thread.currentThread().getId());
    }

    @Override
    public RFuture<Boolean> tryLockAsync(long threadId) {
        return tryAcquireOnceAsync(-1, null, threadId);
    }

    @Override
    public RFuture<Boolean> tryLockAsync(long waitTime, TimeUnit unit) {
        return tryLockAsync(waitTime, -1, unit);
    }

    @Override
    public RFuture<Boolean> tryLockAsync(long waitTime, long leaseTime, TimeUnit unit) {
        long currentThreadId = Thread.currentThread().getId();
        return tryLockAsync(waitTime, leaseTime, unit, currentThreadId);
    }

    @Override
    public RFuture<Boolean> tryLockAsync(final long waitTime, final long leaseTime, final TimeUnit unit,
            final long currentThreadId) {
        final RPromise<Boolean> result = new RedissonPromise<Boolean>();

        final AtomicLong time = new AtomicLong(unit.toMillis(waitTime));
        final long currentTime = System.currentTimeMillis();
        RFuture<Long> ttlFuture = tryAcquireAsync(leaseTime, unit, currentThreadId);
        ttlFuture.addListener(new FutureListener<Long>() {
            @Override
            public void operationComplete(Future<Long> future) throws Exception {
                if (!future.isSuccess()) {
                    result.tryFailure(future.cause());
                    return;
                }

                Long ttl = future.getNow();

                // lock acquired
                if (ttl == null) {
                    if (!result.trySuccess(true)) {
                        unlockAsync(currentThreadId);
                    }
                    return;
                }

                long elapsed = System.currentTimeMillis() - currentTime;
                time.addAndGet(-elapsed);
                
                if (time.get() <= 0) {
                    trySuccessFalse(currentThreadId, result);
                    return;
                }
                
                final long current = System.currentTimeMillis();
                final AtomicReference<Timeout> futureRef = new AtomicReference<Timeout>();
                final RFuture<RedissonLockEntry> subscribeFuture = subscribe(currentThreadId);
                subscribeFuture.addListener(new FutureListener<RedissonLockEntry>() {
                    @Override
                    public void operationComplete(Future<RedissonLockEntry> future) throws Exception {
                        if (!future.isSuccess()) {
                            result.tryFailure(future.cause());
                            return;
                        }

                        if (futureRef.get() != null) {
                            futureRef.get().cancel();
                        }

                        long elapsed = System.currentTimeMillis() - current;
                        time.addAndGet(-elapsed);
                        
                        tryLockAsync(time, leaseTime, unit, subscribeFuture, result, currentThreadId);
                    }
                });
                if (!subscribeFuture.isDone()) {
                    Timeout scheduledFuture = commandExecutor.getConnectionManager().newTimeout(new TimerTask() {
                        @Override
                        public void run(Timeout timeout) throws Exception {
                            if (!subscribeFuture.isDone()) {
                                subscribeFuture.cancel(false);
                                trySuccessFalse(currentThreadId, result);
                            }
                        }
                    }, time.get(), TimeUnit.MILLISECONDS);
                    futureRef.set(scheduledFuture);
                }
            }

        });


        return result;
    }

    private void trySuccessFalse(final long currentThreadId, final RPromise<Boolean> result) {
        acquireFailedAsync(currentThreadId).addListener(new FutureListener<Void>() {
            @Override
            public void operationComplete(Future<Void> future) throws Exception {
                if (future.isSuccess()) {
                    result.trySuccess(false);
                } else {
                    result.tryFailure(future.cause());
                }
            }
        });
    }

    private void tryLockAsync(final AtomicLong time, final long leaseTime, final TimeUnit unit,
            final RFuture<RedissonLockEntry> subscribeFuture, final RPromise<Boolean> result, final long currentThreadId) {
        if (result.isDone()) {
            unsubscribe(subscribeFuture, currentThreadId);
            return;
        }
        
        if (time.get() <= 0) {
            unsubscribe(subscribeFuture, currentThreadId);
            trySuccessFalse(currentThreadId, result);
            return;
        }
        
        final long current = System.currentTimeMillis();
        RFuture<Long> ttlFuture = tryAcquireAsync(leaseTime, unit, currentThreadId);
        ttlFuture.addListener(new FutureListener<Long>() {
            @Override
            public void operationComplete(Future<Long> future) throws Exception {
                if (!future.isSuccess()) {
                    unsubscribe(subscribeFuture, currentThreadId);
                    result.tryFailure(future.cause());
                    return;
                }

                Long ttl = future.getNow();
                // lock acquired
                if (ttl == null) {
                    unsubscribe(subscribeFuture, currentThreadId);
                    if (!result.trySuccess(true)) {
                        unlockAsync(currentThreadId);
                    }
                    return;
                }
                
                long elapsed = System.currentTimeMillis() - current;
                time.addAndGet(-elapsed);
                
                if (time.get() <= 0) {
                    unsubscribe(subscribeFuture, currentThreadId);
                    trySuccessFalse(currentThreadId, result);
                    return;
                }

                // waiting for message
                final long current = System.currentTimeMillis();
                final RedissonLockEntry entry = getEntry(currentThreadId);
                if (entry.getLatch().tryAcquire()) {
                    tryLockAsync(time, leaseTime, unit, subscribeFuture, result, currentThreadId);
                } else {
                    final AtomicBoolean executed = new AtomicBoolean();
                    final AtomicReference<Timeout> futureRef = new AtomicReference<Timeout>();
                    final Runnable listener = new Runnable() {
                        @Override
                        public void run() {
                            executed.set(true);
                            if (futureRef.get() != null) {
                                futureRef.get().cancel();
                            }

                            long elapsed = System.currentTimeMillis() - current;
                            time.addAndGet(-elapsed);
                            
                            tryLockAsync(time, leaseTime, unit, subscribeFuture, result, currentThreadId);
                        }
                    };
                    entry.addListener(listener);

                    long t = time.get();
                    if (ttl >= 0 && ttl < time.get()) {
                        t = ttl;
                    }
                    if (!executed.get()) {
                        Timeout scheduledFuture = commandExecutor.getConnectionManager().newTimeout(new TimerTask() {
                            @Override
                            public void run(Timeout timeout) throws Exception {
                                if (entry.removeListener(listener)) {
                                    long elapsed = System.currentTimeMillis() - current;
                                    time.addAndGet(-elapsed);
                                    
                                    tryLockAsync(time, leaseTime, unit, subscribeFuture, result, currentThreadId);
                                }
                            }
                        }, t, TimeUnit.MILLISECONDS);
                        futureRef.set(scheduledFuture);
                    }
                }
            }
        });
    }


}
;
