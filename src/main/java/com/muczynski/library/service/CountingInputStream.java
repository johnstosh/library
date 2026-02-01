/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream wrapper that counts bytes read. Used to track byte position
 * in the ZIP stream for resumable chunked uploads.
 */
class CountingInputStream extends FilterInputStream {
    private long count;

    CountingInputStream(InputStream in) {
        super(in);
    }

    long getCount() {
        return count;
    }

    @Override
    public int read() throws IOException {
        int result = super.read();
        if (result != -1) {
            count++;
        }
        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = super.read(b, off, len);
        if (result > 0) {
            count += result;
        }
        return result;
    }

    @Override
    public long skip(long n) throws IOException {
        long result = super.skip(n);
        count += result;
        return result;
    }
}
