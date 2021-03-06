/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.dev.fs;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import org.h2.store.fs.FileObject;
import org.h2.util.IOUtils;

/**
 * The file is read from a stream. When reading from start to end, the same
 * input stream is re-used, however when reading from end to start, a new input
 * stream is opened for each request.
 */
public class FileObjectZip2 implements FileObject {

    private static final byte[] SKIP_BUFFER = new byte[1024];

    private final String fullName;
    private final String name;
    private final long length;
    private long pos;
    private InputStream in;
    private long inPos;
    private boolean skipUsingRead;

    FileObjectZip2(String fullName, String name, ZipInputStream in, long length) {
        this.fullName = fullName;
        this.name = name;
        this.length = length;
        this.in = in;
    }

    public void close() {
        try {
            in.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public long getFilePointer() {
        return pos;
    }

    public long length() {
        return length;
    }

    public void readFully(byte[] b, int off, int len) throws IOException {
        if (inPos > pos) {
            if (in != null) {
                in.close();
            }
            in = null;
        }
        if (in == null) {
            in = IOUtils.openFileInputStream(fullName);
            inPos = 0;
        }
        if (inPos < pos) {
            long skip = pos - inPos;
            if (!skipUsingRead) {
                try {
                    IOUtils.skipFully(in, skip);
                } catch (NullPointerException e) {
                    // workaround for Android
                    skipUsingRead = true;
                }
            }
            if (skipUsingRead) {
                while (skip > 0) {
                    int s = (int) Math.min(SKIP_BUFFER.length, skip);
                    s = in.read(SKIP_BUFFER, 0, s);
                    skip -= s;
                }
            }
            inPos = pos;
        }
        int l = IOUtils.readFully(in, b, off, len);
        if (l != len) {
            throw new EOFException();
        }
        pos += len;
        inPos += len;
    }

    public void seek(long newPos) {
        this.pos = newPos;
    }

    public void setFileLength(long newLength) throws IOException {
        throw new IOException("File is read-only");
    }

    public void sync() {
        // nothing to do
    }

    public void write(byte[] b, int off, int len) throws IOException {
        throw new IOException("File is read-only");
    }

    public String getName() {
        return name;
    }

    public boolean tryLock() {
        return false;
    }

    public void releaseLock() {
        // ignore
    }

}
