import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import org.apache.bookkeeper.bookie.BufferedChannel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;

@RunWith(Parameterized.class)
public class BufferedChannelTest {

	@Rule
	public ExpectedException expectedException;
	private int bufferCapacity;
	private ByteBuf dst;
	private ByteBuf src;
	private long pos;
	private int length;
	private long unpersistedBytesBound;
	private Object expectedWrite;
	private Object expectedRead;
	private BufferedChannel buffChann;

	public BufferedChannelTest(int bufferCapacity, ByteBuf dst, ByteBuf src, long pos, int length,
			long unpersistedBytesBound, Object expectedWrite, Object expectedRead) {

		expectedException = ExpectedException.none();
		this.bufferCapacity = bufferCapacity;
		this.dst = dst;
		this.src = src;
		this.pos = pos;
		this.length = length;
		this.unpersistedBytesBound = unpersistedBytesBound;
		this.expectedWrite = expectedWrite;
		this.expectedRead = expectedRead;
	}

	// Creazione casi di test parametrizzati
	@Parameters
	public static Collection<Object[]> data() throws IOException {
		Object[][] data = new Object[][] {
				// capacità WriteBuffer, buffer dst, buffer src, pos
				// (dove iniziare la lettura), length (quanto leggere), unpersistedBytesBound,
				// valore atteso della scrittura, eccezione attesa della lettura

				// ---------------Category Partition e Boundary Values----------

				// length < buffer_capacity-pos, length<dst, pos<buffer_capacity
				{ 1, createByteBufDataEmpty(1), createByteBufData(1), 0, 0, 1, 0, 0 },
//				// length > buffer_capacity - pos, length>dst, pos=buffer_capacity
				{ 0, createByteBufDataEmpty(0), createByteBufData(0), 0, 1, 1, 0, new IOException() },
//				// pos > buffer_capacity
				{ 1, null, null, 2, 1, 0, new NullPointerException(), new NullPointerException() }, };

		return Arrays.asList(data);
	}

	@Before
	public void setUpBufferedChannel() throws IOException {
		buffChann = new BufferedChannel(UnpooledByteBufAllocator.DEFAULT, createFileChannel(), bufferCapacity);
	}

	@Test
	public void writeTest() throws IOException {

		if (expectedWrite instanceof NullPointerException) {
			expectedException.expect(NullPointerException.class);
		}

		buffChann.write(src);
		assertEquals(expectedWrite, buffChann.getNumOfBytesInWriteBuffer());
	}

	// In questo test viene prima invocata la write in modo da leggere nella read()
	// ciò che viene scritto nel WriteBuffer e ne FileChannel a partire dalla
	// write()
	@Test
	public void readTest() throws IOException {

		if (expectedWrite instanceof NullPointerException) {
			expectedException.expect(NullPointerException.class);
		}

		buffChann.write(src);

		if (expectedRead instanceof Exception) {
			expectedException.expect(Exception.class);
			buffChann.read(dst, pos, length);
		} else {
			int result = buffChann.read(dst, pos, length);
			buffChann.close();
			assertEquals(expectedRead, result);
		}

	}

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

	private static ByteBuf createByteBufDataEmpty(int length) {

		ByteBuf bb = Unpooled.buffer(length);
		if (length == 0) {
			return bb;
		}
		byte[] data = new byte[length];
		return bb;
	}

}
