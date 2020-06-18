import static org.junit.Assert.assertEquals;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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

	private long ledgerId;
	private byte[] passwd;
	boolean useV2Protocol;
	private long lac;
	private DigestType digestType;
	private ByteBuf data;
	private long entryId;
	private long length;
	private ByteBufAllocator allocator;
	private Object expectedLac;
	private Object expectedData;
	private Object expectedLastConfirmed;
	@Rule
	public ExpectedException expectedExceptionLac;
//	@Rule
//	public ExpectedException expectedExceptionData;

	public TestDigestManager(long ledgerId, byte[] passwd, boolean useV2Protocol, long lac, DigestType digestType,
			ByteBuf data, long entryId, long length, ByteBufAllocator allocator, Object expectedLac,
			Object expectedData, Object expectedLastConfirmed) {

		this.ledgerId = ledgerId;
		this.passwd = passwd;
		this.useV2Protocol = useV2Protocol;
		this.lac = lac;
		this.digestType = digestType;
		this.data = data;
		this.entryId = entryId;
		this.length = length;
		this.allocator = allocator;
		this.expectedLac = expectedLac;
		this.expectedData = expectedData;
		this.expectedLastConfirmed = expectedLastConfirmed;
		this.expectedExceptionLac = ExpectedException.none();
		// this.expectedExceptionData = ExpectedException.none();
	}

	// Creazione casi di test parametrizzati
	@Parameters
	public static Collection<Object[]> data() throws Exception {
		Object[][] data = new Object[][] {
				// ledgerId, passwd, V2Protocol, lac, digestType, bufData, entryID, length,
				// allocator, valore atteso Lac, valore atteso buffer, valore atteso last
				// confirmed

				// Test che non dovrebbe passare xke LedgerId <0 e entryId=lac
//				{ -1, new byte[] { (byte) 1 }, true, 0L, DigestType.HMAC, createByteBufData(1).get(0), 0L, 1L,
//						ByteBufAllocator.DEFAULT, 0L, createByteBufData(1).get(1), 0L },

				// Test che non dovrebbe passare xke Lac<0
				{ 0, new byte[] { (byte) 1 }, false, -1L, DigestType.CRC32, createByteBufData(1).get(0), 0, 0,
						ByteBufAllocator.DEFAULT, -1L, createByteBufData(1).get(1), -1L },
//				// Test che non dovrebbe passare xke length<0
				{ 0, new byte[6], false, 0L, DigestType.CRC32C, createByteBufData(0).get(0), 1, -1,
						ByteBufAllocator.DEFAULT, 0L, createByteBufData(0).get(1), 0L },
//				// Test che non dovrebbe passare xke entryID<lac
				{ 0, new byte[6], false, 0L, DigestType.DUMMY, createByteBufData(0).get(0), -1, 1,
						ByteBufAllocator.DEFAULT, 0L, createByteBufData(0).get(1), 0L },
				{ 0, null, false, 0L, null, null, 1, 1, null, new NullPointerException(), new NullPointerException(),
						new NullPointerException() }

		};

		return Arrays.asList(data);
	}

	// TODO fare un metodo per creare buffer uguali

	@Test
	public void verifyDigestAndReturnLacTest()
			throws BKDigestMatchException, GeneralSecurityException, InvalidAttributeValueException {
//
//		if (expectedLac instanceof BKDigestMatchException) {
//			expectedExceptionLac.expect(BKDigestMatchException.class);
//		} else if (expectedLac instanceof GeneralSecurityException) {
//			expectedExceptionLac.expect(GeneralSecurityException.class);
//		} else
		if (expectedLac instanceof NullPointerException) {
			expectedExceptionLac.expect(NullPointerException.class);
		}

		DigestManager digestManager = DigestManager.instantiate(ledgerId, passwd, digestType, allocator, useV2Protocol);

		ByteBufList listBuf = digestManager.computeDigestAndPackageForSendingLac(lac);
		long result = digestManager.verifyDigestAndReturnLac(listBuf.getBuffer(0));
		assertEquals(expectedLac, result);
	}

	@Test
	public void verifyDigestAndReturnData()
			throws BKDigestMatchException, GeneralSecurityException, InvalidAttributeValueException {

		// TODO controllare se è pox lasciare una sola ExpectedException variable

//		if (expectedData instanceof BKDigestMatchException) {
//			expectedExceptionLac.expect(BKDigestMatchException.class);
//		} else if (expectedData instanceof GeneralSecurityException) {
//			expectedExceptionLac.expect(GeneralSecurityException.class);
//		} else 
		if (expectedData instanceof NullPointerException) {
			expectedExceptionLac.expect(NullPointerException.class);
		}

		DigestManager digestManager = DigestManager.instantiate(ledgerId, passwd, digestType, allocator, useV2Protocol);

		ByteBufList listBuf = digestManager.computeDigestAndPackageForSending(entryId, lac, length, data);
		ByteBuf result = digestManager.verifyDigestAndReturnData(entryId, ByteBufList.coalesce(listBuf));
		// assertEquals(ByteBufList.coalesce(listBuf).readerIndex(16 + 20), result);

		assertEquals(expectedData, result);
	}

	@Test
	public void verifyDigestAndReturnLastConfirmed()
			throws BKDigestMatchException, GeneralSecurityException, InvalidAttributeValueException {

//		if (expectedLastConfirmed instanceof BKDigestMatchException) {
//			expectedExceptionLac.expect(BKDigestMatchException.class);
//		} else if (expectedLastConfirmed instanceof GeneralSecurityException) {
//			expectedExceptionLac.expect(GeneralSecurityException.class);
//		} else 
		if (expectedLastConfirmed instanceof NullPointerException) {
			expectedExceptionLac.expect(NullPointerException.class);
		}

		DigestManager digestManager = DigestManager.instantiate(ledgerId, passwd, digestType, allocator, useV2Protocol);

		ByteBufList listBuf = digestManager.computeDigestAndPackageForSending(entryId, lac, length, data);
		long result = digestManager.verifyDigestAndReturnLastConfirmed(ByteBufList.coalesce(listBuf))
				.getLastAddConfirmed();
		assertEquals(expectedLastConfirmed, result);
	}

	private static List<ByteBuf> createByteBufData(int length) {

		ArrayList<ByteBuf> listByteBuff = new ArrayList<ByteBuf>();

		if (length == 0) {
			listByteBuff.add(Unpooled.buffer(0));
			listByteBuff.add(Unpooled.buffer(0));
			return listByteBuff;
		} else {

			byte[] data = "Ciao".getBytes();
			// Random random = new Random();
			// Si riempie il buffer con dati random
			// random.nextBytes(data);
			ByteBuf bb = Unpooled.buffer("Ciao".length());
			bb.writeBytes(data);
			listByteBuff.add(bb);
			listByteBuff.add(bb);
		}
		return listByteBuff;
	}

//	private static ByteBuf createAndFillBuffer(boolean useV2Protocol, DigestType digestType, ByteBufAllocator allocator,
//			long ledgerId, long lac) throws Exception {
//
//		int macCodeLength;
//
//		switch (digestType) {
//		case HMAC:
//			macCodeLength = 20;
//			break;
//		case CRC32:
//			macCodeLength = 8;
//			break;
//		case CRC32C:
//			macCodeLength = 4;
//		case DUMMY:
//			macCodeLength = 0;
//		default:
//			throw new Exception("Digest Type non valido");
//		}
//		ByteBuf headersBuffer;
//		if (useV2Protocol) {
//			headersBuffer = allocator.buffer(16 + macCodeLength);
//		} else {
//			headersBuffer = Unpooled.buffer(16 + macCodeLength);
//		}
//		headersBuffer.writeLong(ledgerId);
//		headersBuffer.writeLong(lac);
//		return headersBuffer;
//	}

}