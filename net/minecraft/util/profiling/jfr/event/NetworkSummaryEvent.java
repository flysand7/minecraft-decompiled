package net.minecraft.util.profiling.jfr.event;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import jdk.jfr.Category;
import jdk.jfr.DataAmount;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Period;
import jdk.jfr.StackTrace;
import net.minecraft.obfuscate.DontObfuscate;

@Name("minecraft.NetworkSummary")
@Label("Network Summary")
@Category({"Minecraft", "Network"})
@StackTrace(false)
@Period("10 s")
@DontObfuscate
public class NetworkSummaryEvent extends Event {
   public static final String EVENT_NAME = "minecraft.NetworkSummary";
   public static final EventType TYPE = EventType.getEventType(NetworkSummaryEvent.class);
   @Name("remoteAddress")
   @Label("Remote Address")
   public final String remoteAddress;
   @Name("sentBytes")
   @Label("Sent Bytes")
   @DataAmount
   public long sentBytes;
   @Name("sentPackets")
   @Label("Sent Packets")
   public int sentPackets;
   @Name("receivedBytes")
   @Label("Received Bytes")
   @DataAmount
   public long receivedBytes;
   @Name("receivedPackets")
   @Label("Received Packets")
   public int receivedPackets;

   public NetworkSummaryEvent(String var1) {
      this.remoteAddress = var1;
   }

   public static final class SumAggregation {
      private final AtomicLong sentBytes = new AtomicLong();
      private final AtomicInteger sentPackets = new AtomicInteger();
      private final AtomicLong receivedBytes = new AtomicLong();
      private final AtomicInteger receivedPackets = new AtomicInteger();
      private final NetworkSummaryEvent event;

      public SumAggregation(String var1) {
         this.event = new NetworkSummaryEvent(var1);
         this.event.begin();
      }

      public void trackSentPacket(int var1) {
         this.sentPackets.incrementAndGet();
         this.sentBytes.addAndGet((long)var1);
      }

      public void trackReceivedPacket(int var1) {
         this.receivedPackets.incrementAndGet();
         this.receivedBytes.addAndGet((long)var1);
      }

      public void commitEvent() {
         this.event.sentBytes = this.sentBytes.get();
         this.event.sentPackets = this.sentPackets.get();
         this.event.receivedBytes = this.receivedBytes.get();
         this.event.receivedPackets = this.receivedPackets.get();
         this.event.commit();
      }
   }

   public static final class Fields {
      public static final String REMOTE_ADDRESS = "remoteAddress";
      public static final String SENT_BYTES = "sentBytes";
      private static final String SENT_PACKETS = "sentPackets";
      public static final String RECEIVED_BYTES = "receivedBytes";
      private static final String RECEIVED_PACKETS = "receivedPackets";

      private Fields() {
      }
   }
}
