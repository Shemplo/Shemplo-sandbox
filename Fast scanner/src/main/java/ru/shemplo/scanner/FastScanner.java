package ru.shemplo.scanner;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

import lombok.Setter;

public final class FastScanner implements IFastScanner {
    
    @Setter (onMethod_ = { @Override })
    private Predicate <Integer> skipCharacter = Character::isWhitespace;
    
    private final Reader reader;
    
    public FastScanner (InputStream is, Charset charset) throws IOException {
        if (charset == null || !StandardCharsets.UTF_8.equals (charset)) {
            var reader = new InputStreamReader (is, charset);
            this.reader = new BufferedReader (reader);
        } else {
            this.reader = new UTF8Reader (is);
        }
    }
    
    public FastScanner (String string) {
        this.reader = new StringReader (string);
    }
    
    @Override
    public void close () {
        try   { reader.close (); } 
        catch (IOException ioe) {
            throw new RuntimeException (ioe);
        }
    }
    
    private int linesSkipped = 0, previousCharacter;
    private String token = "";
    
    private void _readToken () throws IOException {
        if (token.length () > 0) { return; }
        linesSkipped = previousCharacter == '\n' ? 1 : 0;
        
        StringBuilder tokenSB = new StringBuilder ();
        boolean isToken = false;
        int charachter = -1;
        
        while ((charachter = reader.read ()) != -1) {
            if (skipCharacter.test (charachter)) {
                if (charachter == '\n' && !isToken) {
                    linesSkipped++;
                }
                
                previousCharacter = charachter;
                if (isToken) { break; } // token is read
            } else {
                tokenSB.append ((char) charachter);
                isToken = true;
            }
            
            previousCharacter = charachter;
        }
        
        token = tokenSB.toString ();
    }

    @Override
    public boolean hasNext () throws IOException {
        _readToken ();
        return token.length () > 0;
    }

    @Override
    public boolean hasNextInLine () throws IOException {
        return hasNext () && linesSkipped == 0;
    }

    @Override
    public boolean hasNextInt (int radix) throws IOException {
        if (!hasNext ()) { return false; }
        try {
            Integer.parseInt (token, radix);
        } catch (NumberFormatException nfe) {
            return false;
        }
        
        return true;
    }

    @Override
    public boolean hasNextLong (int radix) throws IOException {
        if (!hasNext ()) { return false; }
        try {
            Long.parseLong (token, radix);
        } catch (NumberFormatException nfe) {
            try {
                Long.parseUnsignedLong (token, radix);
            } catch (NumberFormatException nfe2) {                
                return false;
            }
        }
        
        return true;
    }

    @Override
    public String next () throws IOException {
        if (hasNext ()) {
            String out = token;
            token = "";
            return out;
        } else {
            throw new IOException ("EOF reached");
        }
    }
    
    @Override
    public void skipLine () throws IOException {
        if (linesSkipped > 0) {
            linesSkipped--;
        } else {            
            int character = -1;
            while ((character = reader.read ()) != -1) {
                if (character == '\n') { break; }
            }
        }
        
        
        previousCharacter = -1;
    }
    
}
