package simpledb.buffer;

import java.util.*;

import simpledb.file.*;
import simpledb.log.LogMgr;

public class LRUBufferMgr implements BufferMgr {

   private List<Buffer> unpinnedBufferList;
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
   public LRUBufferMgr(FileMgr fm, LogMgr lm, int numbuffs) {
      /* Write your code */
      unpinnedBufferList = new ArrayList<>(numbuffs);
      bufferMap = new HashMap<>(numbuffs);

      for (int i = 0; i < numbuffs; i++) {
         Buffer buff = new Buffer(fm, lm, i);
         unpinnedBufferList.add(buff);
      }
   }

   public synchronized int available() {
      /* Write your code */
      return unpinnedBufferList.size();
   }

   public synchronized void flushAll(int txnum) {
      /* Write your code */
      for (Buffer buff : bufferMap.values())
         if (buff.modifyingTx() == txnum)
            buff.flush();
   }

   public synchronized void unpin(Buffer buff) {
      /* Write your code */
      buff.unpin();
      if (!buff.isPinned()) {
         unpinnedBufferList.add(buff);
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
         if (buff == null)
            throw new BufferAbortException();
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
      if (buff == null) {
         buff = chooseUnpinnedBuffer();
         if (buff == null) {
            return null;
         }
         bufferMap.put(blk, buff);
         unpinnedBufferList.remove(buff);
         buff.assignToBlock(blk);
      } else {
         buff.assignToBlock(blk);
         hit_cnt++;
      }
      if (!buff.isPinned()) {
         buff.assignToBlock(blk);
      }
      buff.pin();
      refernce_cnt++;

      return buff;
   }

   public Buffer chooseUnpinnedBuffer() {
      if (!unpinnedBufferList.isEmpty()) {
         return unpinnedBufferList.get(0);
      }
      return null;
   }

   public void printStatus() {
      /* Write your code */
      System.out.println("Allocated Buffers:");
      for (Map.Entry<BlockId, Buffer> entry : bufferMap.entrySet()) {
         BlockId blk = entry.getKey();
         Buffer buff = entry.getValue();
         String status = buff.isPinned() ? "pinned" : "unpinned";
         System.out.println("Buffer " + buff.getID() + ": " + blk.toString() + " " + status);
      }
      System.out.print("Unpinned Buffers in LRU order: ");
      for (Buffer buff : unpinnedBufferList) {
         System.out.print(buff.getID() + " ");
      }
      System.out.println("");
   }

   public float getHitRatio() {
      /* Write your code */
      if (refernce_cnt == 0)
         return (float) 0.0;
      return (float) hit_cnt / refernce_cnt * 100;
   }
}
