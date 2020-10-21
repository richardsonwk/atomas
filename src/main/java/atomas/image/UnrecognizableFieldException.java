package atomas.image;

import java.nio.file.Path;

/**
 * Specialized exception for {@link FieldImage#read(Path)}.
 */
public class UnrecognizableFieldException extends Exception {
    /**
     * @see Exception#Exception(String)
     */
    public UnrecognizableFieldException(final String message) {
        super(message);
    }
}
