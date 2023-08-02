package rvparse;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

/**
 * An event input stream lets an application to read {@link RVEvent} from an
 * underlying input stream in a portable way.
 *
 */
public class RVEventReader {

	private static final LZ4FastDecompressor FAST_DECOMPRESSOR = LZ4Factory.fastestInstance().fastDecompressor();

	private final LZ4BlockInputStream in;

	private final ByteBuffer byteBuffer = ByteBuffer.allocate(RVEvent.SIZEOF);

	private RVEvent lastReadEvent;

	public static final int COMPRESS_BLOCK_SIZE = 8 * 1024 * 1024; // 8MB

	public RVEventReader(Path path) throws IOException {
		lastReadEvent = new RVEvent();
		// public RVEventReader(Path path) {
		in = new LZ4BlockInputStream(new BufferedInputStream(new FileInputStream(path.toFile()), COMPRESS_BLOCK_SIZE),
				FAST_DECOMPRESSOR);
		// readEvent();
		/*
		 * try{ while (true){ System.out.println(readEvent().toString()); } }
		 * catch (EOFException e){}
		 */
	}

	public RVEvent readEvent() throws IOException {
		int bytes;
		int off = 0;
		int len = RVEvent.SIZEOF;
		while ((bytes = in.read(byteBuffer.array(), off, len)) != len) {
			if (bytes == -1) {
				lastReadEvent = null;
				throw new EOFException();
			}
			off += bytes;
			len -= bytes;
		}
		/*
		lastReadEvent = new RVEvent(byteBuffer.getLong(), byteBuffer.getLong(), byteBuffer.getInt(),
				byteBuffer.getLong(), byteBuffer.getLong(), RVEventType.values()[byteBuffer.get()]);
		*/
		lastReadEvent.updateRVEvent(byteBuffer.getLong(), byteBuffer.getLong(), byteBuffer.getInt(),
				byteBuffer.getLong(), byteBuffer.getLong(), RVEventType.values()[byteBuffer.get()]);
		byteBuffer.clear();
		return lastReadEvent;
	}

	public RVEvent lastReadEvent() {
		return lastReadEvent;
	}

	public void close() throws IOException {
		in.close();
	}

}
