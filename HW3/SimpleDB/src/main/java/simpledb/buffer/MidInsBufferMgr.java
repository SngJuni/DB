package simpledb.buffer;

import java.util.*;

import simpledb.file.*;
import simpledb.log.LogMgr;

public class MidInsBufferMgr implements BufferMgr {

   private LinkedList<Buffer> freeBufferList;
   private LinkedList<Buffer> lruBufferList;
   private Map<BlockId, Buffer> bufferMap;
   private static final long MAX_TIME = 10000;
   private int refernce_cnt;
   private int hit_cnt;

   /**
    * Constructor: Creates a buffer manager having the specified
    * number of buffer slots.
    * This constructor depends on a {@link FileMgr} and
    * {@link simpledb.log.LogMgr LogMgr} object.
    * 
    * @param numbuffs the number of buffer slots to allocate
    */
   public MidInsBufferMgr(FileMgr fm, LogMgr lm, int numbuffs) {
      /* Write your code */
      freeBufferList = new LinkedList<>();
      lruBufferList = new LinkedList<>();
      bufferMap = new HashMap<>();

      for (int i = 0; i < numbuffs; i++) {
         Buffer buff = new Buffer(fm, lm, i);
         freeBufferList.add(buff);
      }
   }

   public synchronized int available() {
      /* Write your code */
      int count = 0;
      for (Buffer buff : lruBufferList) {
         if (!buff.isPinned()) {
            count++;
         }
      }
      return freeBufferList.size() + count;
   }

   public synchronized void flushAll(int txnum) {
      /* Write your code */
      for (Buffer buff : lruBufferList) {
         if (buff.modifyingTx() == txnum) {
            buff.flush();
            if (!buff.isPinned()) {
               freeBufferList.add(buff);
            }
         }
      }
   }

   public synchronized Buffer pin(BlockId blk) {
      /* Write your code */
      try {
         long timestamp = System.currentTimeMillis();
         Buffer buff = tryToPin(blk);
         while (buff == null && !waitingTooLong(timestamp)) {
            wait(MAX_TIME);
            buff = tryToPin(blk);
         }
         if (buff == null) {
            throw new BufferAbortException();
         }
         return buff;
      } catch (InterruptedException e) {
         throw new BufferAbortException();
      }
   }

   private boolean waitingTooLong(long starttime) {
      return System.currentTimeMillis() - starttime > MAX_TIME;
   }

   private Buffer tryToPin(BlockId blk) {
      Buffer buff = bufferMap.get(blk);
      if (buff != null) { // hit
         lruBufferList.remove(buff);
         lruBufferList.addFirst(buff);
         hit_cnt++;
         return buff;
      }

      refernce_cnt++;
      int midIdx = (lruBufferList.size() * 5 / 8);

      if (!freeBufferList.isEmpty()) { // when remaining the free buffer
         buff = freeBufferList.remove(0);
         buff.assignToBlock(blk);
         buff.pin();
         bufferMap.put(blk, buff);
         if (lruBufferList.size() <= 3) { // head
            lruBufferList.addFirst(buff);
            return buff;
         } else { // 5/8 point
            lruBufferList.add(midIdx, buff);
            return buff;
         }
      } else {
         ListIterator<Buffer> iterator = lruBufferList.listIterator(lruBufferList.size());
         while (iterator.hasPrevious()) { // when searching the unpinned buffer
            Buffer unpinnedbuff = iterator.previous();
            if (!unpinnedbuff.isPinned()) {
               unpinnedbuff.flush();
               bufferMap.remove(unpinnedbuff.block());
               buff = unpinnedbuff;
               buff.assignToBlock(blk);
               buff.pin();
               bufferMap.put(blk, buff);
               iterator.remove();
               lruBufferList.add(midIdx, buff);

               return buff;
            }
         }
      }

      return null; // no remaing buffer, no unpinned buffer
   }

   public synchronized void unpin(Buffer buff) {
      /* Write your code */
      buff.unpin();
      if (!buff.isPinned()) {
         notifyAll();
      }
   }

   public void printStatus() {
      /* Write your code */
      System.out.println("LRU list:");
      for (Buffer buff : lruBufferList) {
         String status = buff.isPinned() ? "pinned" : "unpinned";
         System.out.println("Buffer " + buff.getID() + ": " + buff.block().toString() + " " + status);
      }
   }

   public float getHitRatio() {
      /* Write your code */
      if (refernce_cnt == 0)
         return (float) 0.0;
      return (float) hit_cnt / refernce_cnt * 100;
   }

}
