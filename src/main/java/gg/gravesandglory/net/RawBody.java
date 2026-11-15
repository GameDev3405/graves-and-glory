package gg.gravesandglory.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.function.Function;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * The 1.21.1 payload body rule (port core lane, 2026-11-15): every Graves and Glory play payload carries its body as the same bytes
 * 1.20.1 wrote into its PacketByteBuf. The codec writes the body unchanged and reads every remaining byte of the payload, so the wire
 * layout of each channel is identical to 1.20.1 (ggbot and the harness keep their readers) and the send and receive sites keep their
 * PacketByteBuf writes and reads. A payload is immutable: it copies the bytes on creation and hands out a fresh reader per call.
 */
public final class RawBody {
    private RawBody() {}

    /** The readable bytes of a buffer the old code built, copied; the buffer's reader index is left where it was. */
    public static byte[] copy(ByteBuf buf) {
        byte[] out = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), out);
        return out;
    }

    /** A new reader over a body, positioned at its first byte. */
    public static PacketByteBuf reader(byte[] body) {
        return new PacketByteBuf(Unpooled.wrappedBuffer(body));
    }

    /** A codec that writes the body as is and reads every byte left in the payload. */
    public static <T extends CustomPayload> PacketCodec<PacketByteBuf, T> codec(Function<byte[], T> make, Function<T, byte[]> body) {
        return PacketCodec.of((value, buf) -> buf.writeBytes(body.apply(value)), buf -> {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return make.apply(bytes);
        });
    }
}
