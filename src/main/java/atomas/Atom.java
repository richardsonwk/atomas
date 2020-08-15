package atomas;

import java.awt.Color;

import net.jcip.annotations.Immutable;

/**
 * An element of the {@link PeriodicTable}; not necessarily a real one. As noted by {@link IAtom}, instances are
 * compared referentially.
 */
@Immutable
public final class Atom implements IAtom {
    /**
     * @see #getAtomicNumber()
     */
    private final int mAtomicNumber;

    /**
     * @see #getColor()
     */
    private final Color mColor;

    /**
     * @see #getName()
     */
    private final String mName;

    /**
     * @see #getSymbol()
     */
    private final String mSymbol;

    /**
     * <strong>For use only by {@link PeriodicTable}.</strong>
     *
     * @param atomicNumber see {@link #getAtomicNumber()}
     * @param symbol see {@link #getSymbol()}
     * @param name see {@link #getName()}
     * @param color see {@link #getColor()}
     * @throws IllegalArgumentException if the atomic number is less than 1
     * @throws IllegalArgumentException if the symbol is {@code null} or only whitespace
     * @throws IllegalArgumentException if the color is {@code null}
     * @see PeriodicTable#atom(int)
     */
    Atom(final int atomicNumber, final String symbol, final String name, final Color color) {
        if (atomicNumber < 1) {
            throw new IllegalArgumentException("atomicNumber " + atomicNumber + " < 1");
        }
        if (symbol == null) {
            throw new IllegalArgumentException("symbol == null");
        }
        if (symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("symbol is only whitespace");
        }
        if (name == null) {
            throw new IllegalArgumentException("name == null");
        }
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("name is only whitespace");
        }
        if (color == null) {
            throw new IllegalArgumentException("color == null");
        }

        mAtomicNumber = atomicNumber;
        mSymbol = symbol.trim();
        mName = name.trim();
        mColor = color;
    }

    /**
     * @return the atom's atomic number (always at least 1)
     */
    public int getAtomicNumber() {
        return mAtomicNumber;
    }

    /**
     * @return the atom's color (never {@code null})
     */
    public Color getColor() {
        return mColor;
    }

    /**
     * @return the atom's name (never {@code null} or empty; always trimmed)
     */
    public String getName() {
        return mName;
    }

    /**
     * @return the atom's symbol (never {@code null} or empty; always trimmed)
     */
    public String getSymbol() {
        return mSymbol;
    }

    @Override
    public String toString() {
        return mSymbol + '[' + mAtomicNumber + ']';
    }
}
