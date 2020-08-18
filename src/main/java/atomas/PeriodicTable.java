package atomas;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import net.jcip.annotations.Immutable;

import static java.util.stream.Collectors.toList;

/**
 * The source of all known {@link Atom}s, including {@link #PLUS} and {@link #DARK_PLUS}.
 */
@Immutable
public final class PeriodicTable {
    /**
     * The single instance of the "dark plus".
     */
    public static final IAtom DARK_PLUS = new IAtom() {
        @Override
        public String toString() {
            return "(+)";
        }
    };

    /**
     * The single instance of the "red plus".
     */
    public static final IAtom PLUS = new IAtom() {
        @Override
        public String toString() {
            return "+";
        }
    };

    /**
     * Internal representation.
     */
    private final List<Atom> mAtoms = new LinkedList<>();

    /**
     * Private constructor for use only by the singleton instance.
     */
    private PeriodicTable() {
        try (
            final BufferedReader reader =
                new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("periodic-table.csv")))) {

            mAtoms.addAll(reader.lines().map(PeriodicTable::toAtom).collect(toList()));
        } catch (final IOException exception) {
            // A build/packaging/coding error.
        }
    }

    /**
     * @param atomicNumber the atomic number of the desired {@link Atom}
     * @return the {@link Atom} (never {@code null})
     * @throws IllegalArgumentException if the atomic number is less than 1
     * @throws IndexOutOfBoundsException if the atomic number is too large
     */
    public static Atom atom(final int atomicNumber) {
        if (atomicNumber < 1) {
            throw new IllegalArgumentException("atomicNumber " + atomicNumber + " < 1");
        }

        return LazySingletonInitializer.INSTANCE.mAtoms.get(atomicNumber - 1);
    }

    /**
     * @return the atom with the largest atomic number in the periodic table (never {@code null})
     */
    public static Atom max() {
        return LazySingletonInitializer.INSTANCE.mAtoms.stream().max(Comparator.comparing(Atom::getAtomicNumber)).get();
    }

    /**
     * [atomic number],[symbol],[name],#[hex rgb color]
     *
     * @param csvLine a comma-separated series of values as described above (must be in the expected format)
     * @return a new {@link Atom} (never {@code null})
     * @throws NumberFormatException if there is a problem with the atomic number or color
     * @throws IllegalArgumentException if the atomic number is out-of-range
     * @throws IllegalArgumentException if the symbol or name is empty
     */
    private static Atom toAtom(final String csvLine) {
        assert csvLine != null;
        assert !csvLine.isEmpty();

        final String[] fields = csvLine.split(",");
        assert fields.length == 4;

        return new Atom(Integer.parseInt(fields[0]), fields[1], fields[2], Color.decode(fields[3]));
    }

    /**
     * Lazily-initialized singleton pattern class.
     */
    @Immutable
    private static final class LazySingletonInitializer {
        /**
         * The instance.
         */
        private static final PeriodicTable INSTANCE = new PeriodicTable();
    }
}
