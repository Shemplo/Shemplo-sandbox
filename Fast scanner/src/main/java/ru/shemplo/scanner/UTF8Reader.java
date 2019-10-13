package ru.shemplo.scanner;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UTF8Reader extends Reader {
    
    private final InputStream is;
    
    @Override
    public int read () throws IOException {
        final int byte1 = is.read ();
        if (byte1 == -1) { return -1; }
        
        if (((byte1 >>> 7) & 1) == 0) {
            return byte1 & 0b1111111;
        } else if (((byte1 >>> 5) & 0b111) == 0b110) {
            return ((byte1 & 0b11111) << 6) | (is.read () & 0b111111);
        } else if (((byte1 >>> 4) & 0b1111) == 0b1110) {
            return ((byte1 & 0b1111) << 12) | ((is.read () & 0b111111) << 6) 
                 | (is.read () & 0b111111);
        } else if (((byte1 >>> 3) & 0b11111) == 0b11110) {
            return ((byte1 & 0b111) << 18) | ((is.read () & 0b111111) << 12) 
                 | ((is.read () & 0b111111) << 6) | (is.read () & 0b111111);
        }
        
        String message = "Failed to parse UTF-8 character";
        throw new IOException (message);
    }

    @Override
    public void close () throws IOException {
        is.close ();
    }

    @Override
    public int read (char [] cbuf, int off, int len) throws IOException {
        throw new UnsupportedOperationException ();
    }
    
}
