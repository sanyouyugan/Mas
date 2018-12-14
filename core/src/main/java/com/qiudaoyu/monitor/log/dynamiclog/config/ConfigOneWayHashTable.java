package com.qiudaoyu.monitor.log.dynamiclog.config;

/**
 * 创建时间: 2018/9/18
 * 类描述: OneWay-HashTable
 * https://blog.csdn.net/v_JULY_v/article/details/6256463
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class ConfigOneWayHashTable {
    private final static int HASH_OFFSET = 0;
    private final static int HASH_A = 1;
    private final static int HASH_B = 2;
    private volatile int tableLength;
    private int[] cryptTable = new int[0x500];
    private HaseNode[] hashIndexTable;

    /**
     * @param nTableLength
     */
    public ConfigOneWayHashTable(int nTableLength) {
        InitCryptTable();
        tableLength = nTableLength;
        //初始化hash表
        hashIndexTable = new HaseNode[nTableLength];
        for (int i = 0; i < nTableLength; i++) {
            hashIndexTable[i].nHashA = -1;
            hashIndexTable[i].nHashB = -1;
            hashIndexTable[i].bExists = false;
        }

    }

    public ConfigOneWayHashTable() {


    }

    public synchronized void setHashIndexTable(HaseNode[] hashIndexTable) {
        this.hashIndexTable = hashIndexTable;
        tableLength = hashIndexTable.length;
    }


    /**
     * 函数名：InitCryptTable
     * 功  能：对哈希索引表预处理
     * 返回值：无
     */
    private void InitCryptTable() {
        int seed = 0x00100001;
        int index1;
        int index2;
        int i;
        for (index1 = 0; index1 < 0x100; index1++) {
            for (index2 = index1, i = 0; i < 5; i++, index2 += 0x100) {
                int temp1, temp2;
                seed = (seed * 125 + 3) % 0x2AAAAB;
                temp1 = (seed & 0xFFFF) << 0x10;
                seed = (seed * 125 + 3) % 0x2AAAAB;
                temp2 = (seed & 0xFFFF);
                cryptTable[index2] = (temp1 | temp2);
            }
        }
    }

    /**
     * 函数名：HashString
     * 功  能：求取哈希值
     * 返回值：返回hash值
     *
     * @param lpszString
     * @param dwHashType
     * @return
     */
    public int HashString(String lpszString, int dwHashType) {
        int seed1 = 0x7FED7FED, seed2 = 0xEEEEEEEE;
        for (int i = 0; i < lpszString.length(); i++) {
            char upperChar = Character.toUpperCase(lpszString.charAt(i));
            seed1 = cryptTable[(dwHashType << 8) + upperChar] ^ (seed1 + seed2);
            seed2 = upperChar + seed1 + seed2 + (seed2 << 5) + 3;
        }
        return seed1;
    }

    /**
     * 函数名：Hashed
     * 功  能：检测一个字符串是否被hash过
     * 返回值：如果存在，返回位置；否则，返回-1
     *
     * @param lpszString
     * @return
     */
    public HaseNode Hashed(String lpszString) {
        //不同的字符串三次hash还会碰撞的几率无限接近于不可能
        int nHash = HashString(lpszString, HASH_OFFSET);
        int nHashA = HashString(lpszString, HASH_A);
        int nHashB = HashString(lpszString, HASH_B);


        int nHashStart = nHash % tableLength;
        int nHashPos = nHashStart;
        while (hashIndexTable[nHashPos].bExists) {
            if (hashIndexTable[nHashPos].nHashA == nHashA && hashIndexTable[nHashPos].nHashB == nHashB) {
                return hashIndexTable[nHashPos];
            } else {
                nHashPos = (nHashPos + 1) % tableLength;
            }
            if (nHashPos == nHashStart) {
                break;
            }

        }
        //没有找到
        return null;
    }

    /**
     * @param nHash
     * @param nHashA
     * @param nHashB
     * @return
     */
    public HaseNode Hashed(int nHash, int nHashA, int nHashB) {
        //不同的字符串三次hash还会碰撞的几率无限接近于不可能
        int nHashStart = nHash % tableLength;
        int nHashPos = nHashStart;
        while (hashIndexTable[nHashPos].bExists) {
            if (hashIndexTable[nHashPos].nHashA == nHashA && hashIndexTable[nHashPos].nHashB == nHashB) {
                return hashIndexTable[nHashPos];
            } else {
                nHashPos = (nHashPos + 1) % tableLength;
            }
            if (nHashPos == nHashStart) {
                break;
            }

        }
        //没有找到
        return null;
    }

    /**
     * 函数名：Hash
     * 功  能：hash一个字符串
     * 返回值：成功，返回true；失败，返回false
     *
     * @param lpszString
     * @return
     */
    HaseNode Hash(String lpszString, Object data) {
        int nHash = HashString(lpszString, HASH_OFFSET);
        int nHashA = HashString(lpszString, HASH_A);
        int nHashB = HashString(lpszString, HASH_B);
        int nHashStart = nHash % tableLength;
        int nHashPos = nHashStart;
        while (hashIndexTable[nHashPos].bExists) {
            nHashPos = (nHashPos + 1) % tableLength;
            //一个轮回
            if (nHashPos == nHashStart) {
                //hash表中没有空余的位置了,无法完成hash
                return null;
            }
        }
        hashIndexTable[nHashPos].bExists = true;
        hashIndexTable[nHashPos].nHashA = nHashA;
        hashIndexTable[nHashPos].nHashB = nHashB;
        hashIndexTable[nHashPos].data = data;
        return hashIndexTable[nHashPos];
    }


    public static class HaseNode {
        int nHashA;
        int nHashB;
        boolean bExists;
        Object data;
    }
}
