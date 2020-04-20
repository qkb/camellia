package com.netease.nim.camellia.redis.toolkit.lock;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.util.SafeEncoder;

import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * 基于redis的分布式锁
 * Created by caojiajun on 2020/4/9.
 */
public class CamelliaRedisLock {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaRedisLock.class);

    private CamelliaRedisTemplate template;//redis客户端
    private byte[] lockKey;//锁key，用于标识一个锁
    private long acquireTimeoutMillis;//获取锁的等待时间
    private long expireTimeoutMillis;//锁的过期时间
    private long tryLockIntervalMillis;//两次尝试获取锁时的间隔
    private String lockId;//锁唯一标识，用于标识谁获取的锁
    private boolean lockOk = false;//锁是否获取到了
    private long expireTimestamp = -1;//锁的过期时间戳

    private CamelliaRedisLock(CamelliaRedisTemplate template, byte[] lockKey, String lockId, long acquireTimeoutMillis, long expireTimeoutMillis, long tryLockIntervalMillis) {
        this.template = template;
        this.lockKey = lockKey;
        this.lockId = lockId;
        this.acquireTimeoutMillis = acquireTimeoutMillis;
        this.expireTimeoutMillis = expireTimeoutMillis;
        this.tryLockIntervalMillis = tryLockIntervalMillis;
    }

    /**
     * 获取一个锁对象
     * @param template redis客户端
     * @param lockKey 锁key
     * @param acquireTimeoutMillis 获取锁的超时时间
     * @param expireTimeoutMillis 锁的过期时间
     * @return 锁对象
     */
    public static CamelliaRedisLock newLock(CamelliaRedisTemplate template, String lockKey,
                                            long acquireTimeoutMillis, long expireTimeoutMillis) {
        return new CamelliaRedisLock(template, SafeEncoder.encode(lockKey), UUID.randomUUID().toString(), acquireTimeoutMillis, expireTimeoutMillis, 5);
    }

    /**
     * 获取一个锁对象
     * @param template redis客户端
     * @param lockKey 锁key
     * @param acquireTimeoutMillis 获取锁的超时时间
     * @param expireTimeoutMillis 锁的过期时间
     * @return 锁对象
     */
    public static CamelliaRedisLock newLock(CamelliaRedisTemplate template, byte[] lockKey,
                                            long acquireTimeoutMillis, long expireTimeoutMillis) {
        return new CamelliaRedisLock(template, lockKey, UUID.randomUUID().toString(), acquireTimeoutMillis, expireTimeoutMillis, 5);
    }

    /**
     * 获取一个锁对象
     * @param template redis客户端
     * @param lockKey 锁key
     * @param lockId 锁id
     * @param acquireTimeoutMillis 获取锁的超时时间
     * @param expireTimeoutMillis 锁的过期时间
     * @return 锁对象
     */
    public static CamelliaRedisLock newLock(CamelliaRedisTemplate template, String lockKey, String lockId,
                                            long acquireTimeoutMillis, long expireTimeoutMillis) {
        return new CamelliaRedisLock(template, SafeEncoder.encode(lockKey), lockId, acquireTimeoutMillis, expireTimeoutMillis, 5);
    }

    /**
     * 获取一个锁对象
     * @param template redis客户端
     * @param lockKey 锁key
     * @param lockId 锁id
     * @param acquireTimeoutMillis 获取锁的超时时间
     * @param expireTimeoutMillis 锁的过期时间
     * @return 锁对象
     */
    public static CamelliaRedisLock newLock(CamelliaRedisTemplate template, byte[] lockKey, String lockId,
                                            long acquireTimeoutMillis, long expireTimeoutMillis) {
        return new CamelliaRedisLock(template, lockKey, lockId, acquireTimeoutMillis, expireTimeoutMillis, 5);
    }

    /**
     * 获取一个锁对象
     * @param template redis客户端
     * @param lockKey 锁key
     * @param lockId 锁id
     * @param acquireTimeoutMillis 获取锁的超时时间
     * @param expireTimeoutMillis 锁的过期时间
     * @param tryLockIntervalMillis 两次尝试获取锁时的间隔
     * @return 锁对象
     */
    public static CamelliaRedisLock newLock(CamelliaRedisTemplate template, String lockKey, String lockId,
                                            long acquireTimeoutMillis, long expireTimeoutMillis, long tryLockIntervalMillis) {
        return new CamelliaRedisLock(template, SafeEncoder.encode(lockKey), lockId, acquireTimeoutMillis, expireTimeoutMillis, tryLockIntervalMillis);
    }

    /**
     * 获取一个锁对象
     * @param template redis客户端
     * @param lockKey 锁key
     * @param lockId 锁id
     * @param acquireTimeoutMillis 获取锁的超时时间
     * @param expireTimeoutMillis 锁的过期时间
     * @param tryLockIntervalMillis 两次尝试获取锁时的间隔
     * @return 锁对象
     */
    public static CamelliaRedisLock newLock(CamelliaRedisTemplate template, byte[] lockKey, String lockId,
                                            long acquireTimeoutMillis, long expireTimeoutMillis, long tryLockIntervalMillis) {
        return new CamelliaRedisLock(template, lockKey, lockId, acquireTimeoutMillis, expireTimeoutMillis, tryLockIntervalMillis);
    }

    /**
     * 尝试获取锁，若获取不到，则立即返回
     */
    public boolean tryLock() {
        try {
            long timestamp = System.currentTimeMillis() + expireTimeoutMillis;
            String set = template.set(lockKey, SafeEncoder.encode(lockId), SafeEncoder.encode("NX"), SafeEncoder.encode("PX"), expireTimeoutMillis);
            boolean ok = set != null && set.equalsIgnoreCase("ok");
            if (ok) {
                this.lockOk = true;
                this.expireTimestamp = timestamp;
            }
            return ok;
        } catch (Exception e) {
            logger.error("tryLock error, lockKey = {}, lockId = {}", lockKey, lockId, e);
            return false;
        }
    }

    /**
     * 锁是否获取到了
     * @return 成功/失败
     */
    public boolean isLockOk() {
        if (!lockOk) return false;
        if (System.currentTimeMillis() < this.expireTimestamp) {
            return true;
        }
        byte[] value = template.get(lockKey);
        return value != null && SafeEncoder.encode(value).equals(lockId);
    }

    /**
     * 尝试获取锁，若没有，则会等待重试，直到acquireTimeoutMillis超时
     * @return 成功/失败
     */
    public boolean lock() {
        long start = System.currentTimeMillis();
        while (true) {
            boolean lockOk = tryLock();
            if (lockOk) {
                return true;
            }
            try {
                Thread.sleep(tryLockIntervalMillis);
            } catch (InterruptedException e) {
                logger.error("sleep error", e);
            }
            if (System.currentTimeMillis() - start > acquireTimeoutMillis) {
                return false;
            }
        }
    }

    /**
     * 尝试对锁进行renew，只能renew自己获取到的锁
     * @return 成功/失败
     */
    public boolean renew() {
        if (!lockOk) {
            return false;
        }
        try {
            byte[] value = template.get(lockKey);
            if (value != null && SafeEncoder.encode(value).equals(this.lockId)) {
                long timestamp = System.currentTimeMillis() + expireTimeoutMillis;
                template.pexpire(lockKey, expireTimeoutMillis);
                this.expireTimestamp = timestamp;
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("renew error, lockKey = {}, lockId = {}", lockKey, lockId, e);
            return false;
        }
    }

    /**
     * 释放锁，只能释放自己获取到的锁
     * @return 成功/失败
     */
    public boolean release() {
        try {
            if (!lockOk) return false;
            byte[] value = template.get(lockKey);
            if (value != null && SafeEncoder.encode(value).equals(this.lockId)) {
                template.del(lockKey);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("release error, lockKey = {}, lockId = {}", lockKey, lockId, e);
            return false;
        }
    }

    /**
     * 清空锁相关的key，会释放不是自己获取到的锁
     * @return 成功/失败
     */
    public boolean clear() {
        try {
            return template.del(lockKey) > 0;
        } catch (Exception e) {
            logger.error("clear error, lockKey = {}, lockId = {}", lockKey, lockId, e);
            return false;
        }
    }

    /**
     * 尝试获取锁，并且执行一个任务，锁获取失败时会等待直到超时失败
     * @param runnable 任务
     * @return 成功/失败
     */
    public boolean lockAndRun(Runnable runnable) {
        boolean lock = lock();
        if (!lock) return false;
        try {
            runnable.run();
            return true;
        } finally {
            release();
        }
    }

    /**
     * 尝试获取一个锁，并且执行一个带返回值的任务，锁获取失败时会等待直到超时失败
     * @param callable 任务
     * @param <T> 任务返回值类型
     * @return 任务返回值
     * @throws Exception 异常
     */
    public <T> LockTaskResult<T> lockAndRun(Callable<T> callable) throws Exception {
        boolean lock = lock();
        if (!lock) return new LockTaskResult<>(false, null);
        try {
            T result = callable.call();
            return new LockTaskResult<>(true, result);
        } finally {
            release();
        }
    }

    /**
     * 尝试获取锁，并且执行一个任务，锁获取失败时会立即返回
     * @param runnable 任务
     * @return 成功/失败
     */
    public boolean tryLockAndRun(Runnable runnable) {
        boolean lock = tryLock();
        if (!lock) return false;
        try {
            runnable.run();
            return true;
        } finally {
            release();
        }
    }

    /**
     * 尝试获取一个锁，并且执行一个带返回值的任务，锁获取失败时会立即返回
     * @param callable 任务
     * @param <T> 任务返回值类型
     * @return 任务返回值
     * @throws Exception 异常
     */
    public <T> LockTaskResult<T> tryLockAndRun(Callable<T> callable) throws Exception {
        boolean lock = tryLock();
        if (!lock) return new LockTaskResult<>(false, null);
        try {
            T result = callable.call();
            return new LockTaskResult<>(true, result);
        } finally {
            release();
        }
    }
}