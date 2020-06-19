import static org.junit.Assert.assertEquals;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collection;

import javax.management.InvalidAttributeValueException;

import org.apache.bookkeeper.client.BKException.BKDigestMatchException;
import org.apache.bookkeeper.proto.DataFormats.LedgerMetadataFormat.DigestType;
import org.apache.bookkeeper.proto.checksum.DigestManager;
import org.apache.bookkeeper.util.ByteBufList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

//DigestManager class takes an entry, attaches a digest to it and packages it with relevant data so that it can be shipped to the bookie. On the return side, it also gets a packet, checks that the digest matches, and extracts the original entry for the packet

@RunWith(Parameterized.class)
public class TestDigestManager {

	@Rule
	public ExpectedException expectedException;
	private long ledgerId;
	boolean useV2Protocol;
	private long lac;
	private DigestType digestType;
	private long entryId;
	private long length;
	private ByteBufAllocator allocator;
	private ByteBuf data; // byteBuf per il ComputeDigest
	private ByteBuf dataLacReceived; // byteBuf per il verifyLac
	private ByteBuf dataReceived; // byteBuf per il verifyData
	private ByteBuf dataLastConfirmedReceived; // byteBuf per il verifyLastConfirmed
	private Object expectedDataCompute;
	private Object expectedLacVerify;
	private Object expectedDataVerify;
	private Object expectedLastConfirmedVerify;

	public TestDigestManager(long ledgerId, boolean useV2Protocol, long lac, DigestType digestType, long entryId,
			long length, ByteBufAllocator allocator, ByteBuf data, ByteBuf dataLacReceived, ByteBuf dataReceived,
			ByteBuf dataLastConfirmedReceived, Object expectedDataCompute, Object expectedLacVerify,
			Object expectedDataVerify, Object expectedLastConfirmedVerify) {

		this.ledgerId = ledgerId;
		this.useV2Protocol = useV2Protocol;
		this.lac = lac;
		this.digestType = digestType;
		this.entryId = entryId;
		this.length = length;
		this.allocator = allocator;
		this.data = data;
		this.dataLacReceived = dataLacReceived;
		this.dataReceived = dataReceived;
		this.dataLastConfirmedReceived = dataLastConfirmedReceived;
		this.expectedDataCompute = expectedDataCompute;
		this.expectedLacVerify = expectedLacVerify;
		this.expectedDataVerify = expectedDataVerify;
		this.expectedLastConfirmedVerify = expectedLastConfirmedVerify;
		this.expectedException = ExpectedException.none();
	}

	// Creazione casi di test parametrizzati
	@Parameters
	public static Collection<Object[]> data() throws Exception {
		Object[][] data = new Object[][] {

				// ledgerId, useV2Protocol, lac, digestType, entryId, length, allocator, data,
				// dataLacReceived, dataReceived, dataLastConfirmedReceived,
				// expectedDataCompute, expectedLacVerify, expectedDataVerify,
				// expectedLastConfirmedVerify

				// ----------------Category Partition e Boundary Values---------------------
				{ -1L, true, -1, DigestType.HMAC, -2, -1, ByteBufAllocator.DEFAULT, createByteBufData(0),
						createByteBufComputeLacData(-1L, ByteBufAllocator.DEFAULT, DigestType.HMAC, true, -1),
						createByteBufComputeData(-1L, ByteBufAllocator.DEFAULT, DigestType.HMAC, true, -1, 0, -2, 0),
						createByteBufComputeData(-1L, ByteBufAllocator.DEFAULT, DigestType.HMAC, true, -1, 0, -2, 0),
						createByteBufData(0), -1L, createByteBufData(0), -1L },

				{ 0L, false, 0, DigestType.CRC32, 0, 0, ByteBufAllocator.DEFAULT, createByteBufData(1),
						createByteBufComputeLacData(0L, ByteBufAllocator.DEFAULT, DigestType.CRC32, false, 0),
						createByteBufComputeData(0L, ByteBufAllocator.DEFAULT, DigestType.CRC32, false, 0, 0, 0, 1),
						createByteBufComputeData(0L, ByteBufAllocator.DEFAULT, DigestType.CRC32, false, 0, 0, 0, 1),
						createByteBufData(1), 0L, createByteBufData(1), 0L },

				{ 1L, false, 1, DigestType.CRC32C, 2, 1, ByteBufAllocator.DEFAULT, createByteBufData(2),
						createByteBufComputeLacData(1L, ByteBufAllocator.DEFAULT, DigestType.CRC32C, false, 1),
						createByteBufComputeData(1L, ByteBufAllocator.DEFAULT, DigestType.CRC32C, false, 1, 1, 2, 2),
						createByteBufComputeData(1L, ByteBufAllocator.DEFAULT, DigestType.CRC32C, false, 1, 1, 2, 2),
						createByteBufData(2), 1L, createByteBufData(2), 1L },

				{ 0L, false, 0, DigestType.DUMMY, 0, 0, ByteBufAllocator.DEFAULT, createByteBufData(0),
						createByteBufData(0), createByteBufData(0), createByteBufData(0), createByteBufData(0),
						new BKDigestMatchException(), new BKDigestMatchException(), new BKDigestMatchException() },

				{ 0L, false, 0, null, 0, 0, null, null, null, null, null, new NullPointerException(),
						new NullPointerException(), new NullPointerException(), new NullPointerException() },

				// ---------------------------Coverage----------------------
				// linee 230 e 190
				{ 0L, false, 0, DigestType.CRC32, 0, 0, ByteBufAllocator.DEFAULT, createByteBufData(1),
						createByteBufComputeLacData(1L, ByteBufAllocator.DEFAULT, DigestType.CRC32, false, 0),
						createByteBufComputeData(1L, ByteBufAllocator.DEFAULT, DigestType.CRC32, false, 0, 0, 0, 1),
						createByteBufComputeData(0L, ByteBufAllocator.DEFAULT, DigestType.CRC32, false, 0, 0, 0, 1),
						createByteBufData(1), new BKDigestMatchException(), new BKDigestMatchException(), 0L },

				// linea 196
				{ 0L, false, 0, DigestType.CRC32, 0, 0, ByteBufAllocator.DEFAULT, createByteBufData(1),
						createByteBufComputeLacData(0L, ByteBufAllocator.DEFAULT, DigestType.CRC32, false, 0),
						createByteBufComputeData(0L, ByteBufAllocator.DEFAULT, DigestType.CRC32, false, 0, 0, 1, 1),
						createByteBufComputeData(0L, ByteBufAllocator.DEFAULT, DigestType.CRC32, false, 0, 0, 0, 1),
						createByteBufData(1), 0L, new BKDigestMatchException(), 0L },

				// linee 220 e 179
				{ 0L, false, 0, DigestType.CRC32, 0, 0, ByteBufAllocator.DEFAULT, createByteBufData(1),
						createByteBufComputeLacData(0L, ByteBufAllocator.DEFAULT, DigestType.HMAC, false, 0),
						createByteBufComputeData(0L, ByteBufAllocator.DEFAULT, DigestType.HMAC, false, 0, 0, 0, 1),
						createByteBufComputeData(0L, ByteBufAllocator.DEFAULT, DigestType.CRC32, false, 0, 0, 0, 1),
						createByteBufData(1), new BKDigestMatchException(), new BKDigestMatchException(), 0L }

		};

		// long ledgerId, allocator, digestType,
		// useV2Protocol, int lac, int length, int entryId, int capacity

		return Arrays.asList(data);

	}

	@Test
	public void computeDigestAndPackageForSending() throws GeneralSecurityException {

		if (expectedDataCompute instanceof NullPointerException) {
			expectedException.expect(NullPointerException.class);
		}

		DigestManager digestManager = DigestManager.instantiate(ledgerId, new byte[6], digestType, allocator,
				useV2Protocol);

		ByteBufList result = digestManager.computeDigestAndPackageForSending(entryId, lac, length, data);
		assertEquals(expectedDataCompute, result.getBuffer(1));
	}

	@Test
	public void verifyDigestAndReturnLacTest()
			throws BKDigestMatchException, GeneralSecurityException, InvalidAttributeValueException {

		if (expectedLacVerify instanceof BKDigestMatchException) {
			expectedException.expect(BKDigestMatchException.class);
		}
		if (expectedLacVerify instanceof NullPointerException) {
			expectedException.expect(NullPointerException.class);
		}

		DigestManager digestManager = DigestManager.instantiate(ledgerId, new byte[6], digestType, allocator,
				useV2Protocol);

		long result = digestManager.verifyDigestAndReturnLac(dataLacReceived);
		assertEquals(expectedLacVerify, result);
	}

	// Ritorna in output il buffer di dati passati in input al compute
	@Test
	public void verifyDigestAndReturnData()
			throws BKDigestMatchException, GeneralSecurityException, InvalidAttributeValueException {

		if (expectedDataVerify instanceof BKDigestMatchException) {
			expectedException.expect(BKDigestMatchException.class);
		}
		if (expectedDataVerify instanceof NullPointerException) {
			expectedException.expect(NullPointerException.class);
		}

		DigestManager digestManager = DigestManager.instantiate(ledgerId, new byte[6], digestType, allocator,
				useV2Protocol);

		ByteBuf result = digestManager.verifyDigestAndReturnData(entryId, dataReceived);
		assertEquals(expectedDataVerify, result);
	}

	@Test
	public void verifyDigestAndReturnLastConfirmed()
			throws BKDigestMatchException, GeneralSecurityException, InvalidAttributeValueException {

		if (expectedLastConfirmedVerify instanceof BKDigestMatchException) {
			expectedException.expect(BKDigestMatchException.class);
		}
		if (expectedLastConfirmedVerify instanceof NullPointerException) {
			expectedException.expect(NullPointerException.class);
		}

		DigestManager digestManager = DigestManager.instantiate(ledgerId, new byte[6], digestType, allocator,
				useV2Protocol);
		long result = digestManager.verifyDigestAndReturnLastConfirmed(dataLastConfirmedReceived).getLastAddConfirmed();
		assertEquals(expectedLastConfirmedVerify, result);
	}

	// Genera un byteBuf senza usare computeDigest
	private static ByteBuf createByteBufData(int capacity) {

		ByteBuf byteBuff = Unpooled.buffer(capacity);

		for (int i = 0; i < capacity; i++) {
			byteBuff.writeBytes("a".getBytes());
		}
		return byteBuff;
	}

	// Genera un byteBuf usando computeDigestForLac
	private static ByteBuf createByteBufComputeLacData(long ledgerId, ByteBufAllocator allocator, DigestType digestType,
			boolean useV2Protocol, int lac) throws GeneralSecurityException {

		DigestManager digestManager = DigestManager.instantiate(ledgerId, new byte[6], digestType, allocator,
				useV2Protocol);

		ByteBufList dataPackagedList = digestManager.computeDigestAndPackageForSendingLac(lac);

		return dataPackagedList.getBuffer(0);
	}

	// Genera un byteBuf usando computeDigest
	private static ByteBuf createByteBufComputeData(long ledgerId, ByteBufAllocator allocator, DigestType digestType,
			boolean useV2Protocol, int lac, int length, int entryId, int capacity) throws GeneralSecurityException {

		DigestManager digestManager = DigestManager.instantiate(ledgerId, new byte[6], digestType, allocator,
				useV2Protocol);

		ByteBuf byteBuf = createByteBufData(capacity);
		ByteBufList dataPackagedList = digestManager.computeDigestAndPackageForSending(entryId, lac, length, byteBuf);

		return ByteBufList.coalesce(dataPackagedList);
	}

}