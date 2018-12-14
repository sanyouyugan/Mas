package com.qiudaoyu.monitor.log.dynamiclog.pool;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 创建时间: 2018/9/21
 * 类描述:固定个数的对象池
 *
 * @author 秋刀鱼
 * @version 1.0
 * @param <T>
 */
public final class ObjectPool<T extends ObjectPool.RecyclableObject> {
    public static int RESET_NUM = 1000;
    private T[] mTable;
    private AtomicInteger mOrderNumber;
    private RecyclableFactory factory;

    /**
     * @param inputArray 长度为2的n次方
     * @param factory    T的工厂
     */
    public ObjectPool(T[] inputArray, RecyclableFactory factory) {
        mOrderNumber = new AtomicInteger(0);
        mTable = inputArray;
        if (mTable == null) {
            throw new NullPointerException("The input array is null.");
        }
        if (factory == null) {
            throw new NullPointerException("The factory is null.");
        }
        this.factory = factory;
        int length = inputArray.length;
        if ((length & length - 1) != 0) {
            throw new RuntimeException("The length of input array is not 2^n.");
        }
    }

    public void recycle(T object) {
        object.isIdle.set(true);
    }

    public T obtain() {
        return obtain(0);
    }

    private T obtain(int retryTime) {
        int index = mOrderNumber.getAndIncrement();
        if (index > RESET_NUM) {
            mOrderNumber.compareAndSet(index, 0);
            if (index > RESET_NUM * 2) {
                mOrderNumber.set(0);
            }
        }

        int num = index & (mTable.length - 1);

        T target = mTable[num];

        if (target.isIdle.compareAndSet(true, false)) {
            return target;
        } else {
            //尝试3次不成功就分配新的
            if (retryTime < 3) {
                return obtain(retryTime++);
            } else {
                return (T) factory.createNew();
            }
        }
    }

    public void clear() {

    }

    public interface RecyclableFactory<T extends ObjectPool.RecyclableObject> {
        T createNew();
    }

    public abstract static class RecyclableObject {
        AtomicBoolean isIdle = new AtomicBoolean(true);

    }


}