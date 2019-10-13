package ru.shemplo.scanner;

import java.io.Closeable;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Interface for homework about "fast" scanner.
 *
 * All mentioned methods (as this gist too) is just a recomendation for students.
 * No exact following of it is required but it might make life easier in future.
 * 
 * But there are global requirements for implementation of "fast" scanner:
 * <ul>
 *     <li>Instance of "fast" scanner has to be constructed with any InputStream or Reader.</li>
 *     <li>Reading or any other manipulations with character more than once is prohibited.</li>
 *     <li>No raw expanding buffers of characters sould be used.</li>
 *     <li>
 *         Implemented logic must be strictly bounded with purposes of this type 
 *         (f.e. `sorting` is not purpose of this type).
 *     </li>
 *     <li>Rational approach should be used everywhere.</li>
 * </ul>
 */
public interface IFastScanner extends Closeable {
    
    /**
     * Check if next token (sequence of non whitespace characters) is available.
     * The negative answer should be interpreted as EOF signal.
     *
     * This method can be called several times in a row but the answer should stay
     * the same until some of `next...()` method will be called
     *
     * @return <b>true</b> if one more token is available; <b>false</b> otherwise
     * @throws IOException in case of problems or reading from InputStream or Reader
     */
    boolean hasNext () throws IOException;
    
    /**
     * Check if next token is available before the EOF or \r\n (Windows) or \n (Unix).
     * The negative answer should be interpreted as EOL signal
     *
     * @return <b>true</b> if one more token is available; <b>false</b> otherwise
     * @throws IOException in case of problems or reading from InputStream or Reader
     */
    boolean hasNextInLine () throws IOException;
    
    default
    boolean hasNextInt () throws IOException {
        return hasNextInt (10);
    }
    
    boolean hasNextInt (int radix) throws IOException;
    
    boolean hasNextIntInLine (int radix) throws IOException;
    
    boolean hasNextLong (int radix) throws IOException;
    
    boolean hasNextLongInLine (int radix) throws IOException;
    
    // and so on ...
    
    /**
     * @return next available token (doesn't matter if it is in new or non-new line)
     * @throws IOException in case of problems or reading from InputStream or Reader
     */
    String next () throws IOException;
    
    String nextLine () throws IOException;
    
    // The logic of next methods should be the same and use for detecting type of token
    // only read sequence of characters.
    //
    // All trics with `try { Interger.parseInt (this.sequnce); } catch { return false; }`
    // are prohibited!
    
    default
    int nextInt () throws IOException {
        return nextInt (10);
    }
    
    int nextInt (int radix) throws IOException;
    
    default
    long nextLong () throws IOException {
        return nextLong (10);
    }
    
    long nextLong (int radix) throws IOException;
    
    default
    long nextUnsignedLong () throws IOException {
        return nextUnsignedLong (10);
    }
    
    long nextUnsignedLong (int radix) throws IOException;
    
    // optional
    
    /**
     * This method set up special criteria for methods {#hasNextWord ()} and {#hasNextWordInLine ()}
     * that defines whether read sequence of characters is word or not.
     *
     * @argument regexpPattern compiled regular expression
     */
    void setWordPattern (Pattern regexpPattern);
    
    /**
     * The same as {#hasNext ()} but in additional check if token accepts word criteria defined
     * from {#setWordPredicate (Pattern)}. In case if no criteria (or null value passed) no
     * additional checks should be done
     */
    boolean hasNextWord () throws IOException;
    
    boolean hasNextWordInLine () throws IOException;
    
    String nextWord () throws IOException;
    
}