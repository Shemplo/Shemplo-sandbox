package ru.shemplo.scanner;

import java.io.IOException;
import java.io.InputStream;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FastScanner {
    
    private static final int BUFFER_SIZE = 1 << 13;
    
    private final byte [] buffer = new byte [BUFFER_SIZE];
    private boolean isEOS = false; // is End of Stream
    private int bufferLength = 0, pointer = 0;
    
    private String token = "";
    
    private final InputStream input;
    
    private void tryReadNextToken () throws IOException {
        if (token.length () > 0) { return; } // token is already read
        
        StringBuilder sb = new StringBuilder ();
        token = "";
        
        int codePoint = 0, bytes = 0;
        boolean wasToken = false;
        
        while (!isEOS) {
            if (pointer >= bufferLength) {
                bufferLength = input.read (buffer);
                isEOS = bufferLength == -1;
                pointer = 0;
            }
            
            if (bytes == 0) {
                if (codePoint != 0) {
                    if (Character.isWhitespace (codePoint)) {
                        if (wasToken) { break; }
                    } else {
                        sb.append (Character.toString (codePoint));
                        wasToken = true;
                    }
                }
                
                if (isEOS) { break; }
                byte header = buffer [pointer];
                if (((header >>> 7) & 0b1) == 0) {
                    codePoint = header & 0b1111111;
                    bytes = 0;
                } else if (((header >>> 5) & 0b111) == 0b110) {
                    codePoint = header & 0b111111;
                    bytes = 1;
                } else if (((header >>> 4) & 0b1111) == 0b1110) {
                    codePoint = header & 0b1111;
                    bytes = 2;
                } else if (((header >>> 3) & 0b11111) == 0b11110) {
                    codePoint = header & 0b111;
                    bytes = 3;
                } else {
                    String message = "Failed to parse UTF-8 character";
                    throw new IllegalStateException (message);
                }
                
                pointer++;
                continue;
            }
            
            codePoint = (codePoint << 6) | (buffer [pointer] & 0b111111);
            pointer++;
            bytes--;
        }
        
        token = sb.toString ();
    }
    
    public boolean hasNext () throws IOException {
        tryReadNextToken ();
        return token.length () > 0;
    }
    
    public String next () throws IOException {
        tryReadNextToken ();
        String out = token;
        token = "";
        return out;
    }
    
    public boolean hasNextInt () throws IOException {
        if (!hasNext ()) { return false; }
        try {
            Integer.parseInt (token);
        } catch (NumberFormatException nfe) {
            return false;
        }
        
        return true;
    }
    
    public int nextInt () throws IOException {
        return Integer.parseInt (next ());
    }
    
}
