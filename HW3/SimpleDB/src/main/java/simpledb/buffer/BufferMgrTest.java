package simpledb.buffer;

import simpledb.server.SimpleDB;
import simpledb.file.*;

public class BufferMgrTest {
   public static void main(String[] args) throws Exception {
      SimpleDB db = new SimpleDB("buffermgrtest", 400, 10, "midIns"); // only 3 buffers
      BufferMgr bm = db.bufferMgr();

      Buffer[] buff = new Buffer[10];
      buff[0] = bm.pin(new BlockId("testfile", 0));
      buff[1] = bm.pin(new BlockId("testfile", 1));
      buff[2] = bm.pin(new BlockId("testfile", 2));
      buff[3] = bm.pin(new BlockId("testfile", 3));
      buff[4] = bm.pin(new BlockId("testfile", 4)); // block 1 repinned
      bm.unpin(buff[2]);
      buff[5] = bm.pin(new BlockId("testfile", 5));
      buff[6] = bm.pin(new BlockId("testfile", 6));
      buff[7] = bm.pin(new BlockId("testfile", 7));
      bm.unpin(buff[0]);
      bm.unpin(buff[3]);
      bm.unpin(buff[6]);
      bm.unpin(buff[1]);
      buff[8] = bm.pin(new BlockId("testfile", 8));
      buff[9] = bm.pin(new BlockId("testfile", 9));

      // bm.unpin(buff[1]);
      // buff[1] = null;
      // buff[3] = bm.pin(new BlockId("testfile", 0)); // block 0 pinned twice
      bm.printStatus();
      // System.out.println(bm.getHitRatio());
      // System.out.println("Available buffers: " + bm.available());
      // try {
      // System.out.println("Attempting to pin block 3...");
      // buff[5] = bm.pin(new BlockId("testfile", 3)); // will not work; no buffers
      // left
      // } catch (BufferAbortException e) {
      // System.out.println("Exception: No available buffers\n");
      // }
      // bm.unpin(buff[2]);
      // buff[2] = null;
      // buff[5] = bm.pin(new BlockId("testfile", 3)); // now this works

      // System.out.println("Final Buffer Allocation:");
      // for (int i = 0; i < buff.length; i++) {
      // Buffer b = buff[i];
      // if (b != null)
      // System.out.println("buff[" + i + "] pinned to block " + b.block());
      // }
   }
}
