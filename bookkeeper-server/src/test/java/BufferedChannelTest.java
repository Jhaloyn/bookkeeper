import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import org.apache.bookkeeper.bookie.BufferedChannel;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;

@RunWith(Parameterized.class)
public class BufferedChannelTest {

	@Rule
	public ExpectedException expectedExceptionWrite;
	@Rule
	public ExpectedException expectedExceptionRead;
	private int bufferCapacity;
	private ByteBufAllocator allocator;
	private FileChannel fileChannel;
	private ByteBuf dst;
	private ByteBuf src;
	private long pos;
	private int length;
	private Exception exceptionWrite;
	private Object expectedRead;

	public BufferedChannelTest(int bufferCapacity, ByteBufAllocator allocator, FileChannel fileChannel, ByteBuf dst,
			ByteBuf src, long pos, int length, Exception exceptionWrite, Object expectedRead) {

		expectedExceptionWrite = ExpectedException.none();
		expectedExceptionRead = ExpectedException.none();
		this.bufferCapacity = bufferCapacity;
		this.allocator = allocator;
		this.fileChannel = fileChannel;
		this.dst = dst;
		this.src = src;
		this.pos = pos;
		this.length = length;
		this.exceptionWrite = exceptionWrite;
		this.expectedRead = expectedRead;
	}

	// Creazione casi di test parametrizzati
	@Parameters
	public static Collection<Object[]> data() throws IOException {
		Object[][] data = new Object[][] {
				// capacità WriteBuffer, allocator, file channel, buffer dst, buffer src, pos
				// (dove iniziare la lettura), length (quanto leggere), valore atteso della
				// scrittura, eccezione attesa della lettura

				// length < buffer_capacity-pos, length<dst, pos<buffer_capacity
				{ 1, UnpooledByteBufAllocator.DEFAULT, createFileChannel(), createByteBufData(1), createByteBufData(1),
						0, 0, null, 0 },
				// length > buffer_capacity - pos, length>dst, pos=buffer_capacity
				{ 0, UnpooledByteBufAllocator.DEFAULT, createFileChannel(), createByteBufData(0), createByteBufData(0),
						0, 1, null, new IOException() },
				// pos > buffer_capacity
				{ 1, null, null, null, null, 2, 1, new NullPointerException(), new IOException() }

		};

		return Arrays.asList(data);
	}

	// Le modifiche apportate al codice per eseguire i casi di test progettati sono
	// a riga: 238
	private static FileChannel createFileChannel() throws IOException {

		File newLogFile;

		// "test" è il nome del file, "log" è l'estensione del file
		newLogFile = File.createTempFile("test", "log");
		// quando l'applicazione termina il file viene cancellato
		newLogFile.deleteOnExit();
		return new RandomAccessFile(newLogFile, "rw").getChannel();
	}

	private static ByteBuf createByteBufData(int length) {

		ByteBuf bb = Unpooled.buffer(length);

		if (length == 0) {
			return bb;
		}

		byte[] data = new byte[length];
		Random random = new Random();
		// Si riempie il buffer con dati random
		random.nextBytes(data);
		bb.writeBytes(data);
		return bb;
	}

	@Test
	public void writeAndReadTest() throws IOException {

		if (exceptionWrite != null) {
			expectedExceptionWrite.expect(NullPointerException.class);
		}

		BufferedChannel buffChann = new BufferedChannel(allocator, fileChannel, bufferCapacity);
		buffChann.write(src);

		if (expectedRead instanceof Exception) {
			expectedExceptionRead.expect(Exception.class);
			buffChann.read(dst, pos, length);
		} else {
			int result = buffChann.read(dst, pos, length);
			buffChann.close();
			fileChannel.close();
			assertEquals(expectedRead, result);
		}

	}

}
