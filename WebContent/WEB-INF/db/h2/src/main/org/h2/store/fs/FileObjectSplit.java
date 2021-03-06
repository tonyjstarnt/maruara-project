/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.store.fs;

import java.io.EOFException;
import java.io.IOException;
import org.h2.message.DbException;
import org.h2.util.IOUtils;

/**
 * A file that may be split into multiple smaller files.
 */
public class FileObjectSplit implements FileObject {

    private final String name;
    private final String mode;
    private final long maxLength;
    private FileObject[] list;
    private long filePointer;
    private long length;

    FileObjectSplit(String name, String mode, FileObject[] list, long length, long maxLength) {
        this.name = name;
        this.mode = mode;
        this.list = list;
        this.length = length;
        this.maxLength = maxLength;
    }

    public void close() throws IOException {
        for (FileObject f : list) {
            f.close();
        }
    }

    public long getFilePointer() {
        return filePointer;
    }

    public long length() {
        return length;
    }

    private int read(byte[] b, int off, int len) throws IOException {
        long offset = filePointer % maxLength;
        int l = (int) Math.min(len, maxLength - offset);
        FileObject fo = getFileObject();
        fo.seek(offset);
        fo.readFully(b, off, l);
        filePointer += l;
        return l;
    }

    public void readFully(byte[] b, int off, int len) throws IOException {
        if (filePointer + len > length) {
            throw new EOFException();
        }
        while (true) {
            int l = read(b, off, len);
            len -= l;
            if (len <= 0) {
                return;
            }
            off += l;
        }
    }

    public void seek(long pos) {
        filePointer = pos;
    }

    private FileObject getFileObject() throws IOException {
        int id = (int) (filePointer / maxLength);
        while (id >= list.length) {
            int i = list.length;
            FileObject[] newList = new FileObject[i + 1];
            System.arraycopy(list, 0, newList, 0, i);
            String fileName = FileSystemSplit.getFileName(name, i);
            newList[i] = FileSystem.getInstance(fileName).openFileObject(fileName, mode);
            list = newList;
        }
        return list[id];
    }

    public void setFileLength(long newLength) throws IOException {
        filePointer = Math.min(filePointer, newLength);
        int newFileCount = 1 + (int) (newLength / maxLength);
        if (newFileCount == list.length) {
            long size = newLength - maxLength * (newFileCount - 1);
            list[list.length - 1].setFileLength(size);
        } else {
            FileObject[] newList = new FileObject[newFileCount];
            int max = Math.max(newFileCount, list.length);
            long remaining = newLength;
            // delete backwards, so that truncating is somewhat transactional
            for (int i = list.length - 1; i >= newFileCount; i--) {
                // verify the file is writable
                list[i].setFileLength(0);
                list[i].close();
                try {
                    IOUtils.delete(list[i].getName());
                } catch (DbException e) {
                    throw DbException.convertToIOException(e);
                }
            }
            for (int i = 0; i < max; i++) {
                long fileSize = Math.min(remaining, maxLength);
                remaining -= fileSize;
                if (i >= newFileCount) {
                    // already closed and deleted
                } else if (i >= list.length) {
                    String fileName = FileSystemSplit.getFileName(name, i);
                    FileObject o = FileSystem.getInstance(fileName).openFileObject(fileName, mode);
                    o.setFileLength(fileSize);
                    newList[i] = o;
                } else {
                    FileObject o = list[i];
                    if (o.length() != fileSize) {
                        o.setFileLength(fileSize);
                    }
                    newList[i] = list[i];
                }
            }
            list = newList;
        }
        this.length = newLength;
    }

    public void sync() throws IOException {
        for (FileObject f : list) {
            f.sync();
        }
    }

    public void write(byte[] b, int off, int len) throws IOException {
        while (true) {
            int l = writePart(b, off, len);
            len -= l;
            if (len <= 0) {
                return;
            }
            off += l;
        }
    }

    private int writePart(byte[] b, int off, int len) throws IOException {
        long offset = filePointer % maxLength;
        int l = (int) Math.min(len, maxLength - offset);
        FileObject fo = getFileObject();
        fo.seek(offset);
        fo.write(b, off, l);
        filePointer += l;
        length = Math.max(length, filePointer);
        return l;
    }

    public String getName() {
        return FileSystemSplit.PREFIX + name;
    }

    public boolean tryLock() {
        return list[0].tryLock();
    }

    public void releaseLock() {
        list[0].releaseLock();
    }

}
