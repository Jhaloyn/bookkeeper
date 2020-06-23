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
public class TestBufferedChannel {

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

	public TestBufferedChannel(int bufferCapacity, ByteBuf dst, ByteBuf src, long pos, int length,
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

//				// length < buffer_capacity-pos, length<dst, pos<buffer_capacity
//				{ 1, createByteBufDataEmpty(1), createByteBufData(1), 0, 0, 1, 0, 0 },
//				// length > buffer_capacity - pos, length > dst, pos=buffer_capacity
//				{ 0, createByteBufDataEmpty(0), createByteBufData(0), 0, 1, 1, 0, new IOException("Read past EOF") },
//				// length = buffer_capacity - pos, length = dst, pos < buffer_capacity
//				{ 1, createByteBufDataEmpty(1), createByteBufData(1), 0, 1, 0, 0, 1 },
//				// pos > buffer_capacity
//				{ 2, null, null, 2, 1, 0, new NullPointerException(), new NullPointerException() },

				// ------------------------------Coverage strutturale---------------------------
//				{ 5, createByteBufDataEmpty(10), createByteBufData(10), 0, 5, 1, 0, 5 }, // da linea 128
//				{ 10, createByteBufDataEmpty(10), createByteBufData(5), 0, 5, 0, 5, 5 }, // linee 122,127, da 258
//
//				// -------------------------Mutation coverage---------------------------------
//				{ 5, createByteBufDataEmpty(10), createByteBufData(7), 0, 5, 0, 2, 5 }, // mutazione linea 116
//				{ 5, createByteBufDataEmpty(10), createByteBufData(7), 0, 5, 1, 0, 5 }, // mutazioni linee 129,130
//				{ 5, createByteBufDataEmpty(10), createByteBufData(7), 0, 5, 7, 0, 5 }, // mutazione linea 129
//				// mutazioni linee 247, 248
//				{ 5, createByteBufDataEmpty(10), createByteBufData(7), 5, 5, 0, 2, new IOException("Read past EOF") },
//				// mutazione linea 290
//				{ 5, createByteBufDataEmpty(10), createByteBufData(7), 3, 5, 0, 2, new IOException("Read past EOF") },
//				{ 5, createByteBufDataEmpty(10), createByteBufData(4), 1, 3, 0, 4, 3 }, // mutazione linea 287

				// LOOP test per //mutante 266
				// { 7, createByteBufDataEmpty(5), createByteBufData(10), 0, 10, 0, 3, 10 }

				// length < buffer_capacity-pos, length<dst, pos<buffer_capacity
				{ 1, createByteBufDataEmpty(1), createByteBufData(1), 0, 0, 0, 0, 0 },
				// length > buffer_capacity - pos, length > dst, pos=buffer_capacity
				{ 0, createByteBufDataEmpty(0), createByteBufData(0), 0, 1, 0, 0, new IOException("Read past EOF") },
				// length = buffer_capacity - pos, length = dst, pos < buffer_capacity
				{ 1, createByteBufDataEmpty(1), createByteBufData(1), 0, 1, 0, 0, 1 },
				// pos > buffer_capacity
				{ 2, null, null, 3, 1, 0, new NullPointerException(), new NullPointerException() }, };

		return Arrays.asList(data);

	}

	@Before
	public void setUpBufferedChannel() throws IOException {
		buffChann = new BufferedChannel(UnpooledByteBufAllocator.DEFAULT, createFileChannel(), bufferCapacity,
				unpersistedBytesBound);
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
	// ciò che viene scritto nel WriteBuffer e nel FileChannel a partire dalla
	// write()
	@Test
	public void readTest() throws IOException {

		if (expectedWrite instanceof NullPointerException) {
			expectedException.expect(NullPointerException.class);
		}

		buffChann.write(src);

		if (expectedRead instanceof IOException) {
			expectedException.expect(IOException.class);
		}

		int result = buffChann.read(dst, pos, length);
		buffChann.close();
		assertEquals(expectedRead, result);
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
