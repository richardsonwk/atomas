package atomas;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;

import static atomas.PeriodicTable.DARK_PLUS;
import static atomas.PeriodicTable.PLUS;
import static atomas.PeriodicTable.atom;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * The game board: a ring containing 1 to 18 {@link IAtom}s. The addition of a 19th atom which does not cause a
 * reaction ends the game.
 *
 * <p>
 * The purpose of this class is to store the board state and implement reactions. It does not embody game mechanics;
 * for example, using a {@code neutrino} to duplicate an atom is just an {@linkplain #insert(IAtom, int) insert}, with
 * the <em>external</em> restriction that the inserted atom be one of those already present in the field. Similarly,
 * using a {@code minus} atom is a {@linkplain #remove(int) remove}. This class does not simulate the probabilistic
 * generation of the next atom.
 * </p>
 */
@NotThreadSafe
public final class Field {
    /**
     * @see #hashCode()
     */
    private static final Function<IAtom, Integer> ATOM_TO_INT = atom -> {
        if (atom instanceof Atom) {
            return ((Atom) atom).getAtomicNumber();
        } else if (atom == PLUS) {
            return -1;
        } else {
            return -2;
        }
    };

    /**
     * Internal representation of the atoms on the board.
     */
    private final List<IAtom> mContents = new LinkedList<>();

    /**
     * @see IFieldListener
     */
    private final List<IFieldListener> mListeners = new LinkedList<>();

    /**
     * @param initialContents the initial set of contents
     * @throws IllegalArgumentException if the list is {@code null}, empty, or contains {@code null}
     */
    public Field(final List<Atom> initialContents) {
        if (initialContents == null) {
            throw new IllegalArgumentException("initialContents == null");
        }
        if (initialContents.isEmpty()) {
            throw new IllegalArgumentException("Field requires at least one atom");
        }
        if (initialContents.contains(null)) {
            throw new IllegalArgumentException("initialContents contains null");
        }

        mContents.addAll(initialContents);
    }

    /**
     * @param listener the listener to add if not already present
     * @throws IllegalArgumentException if the argument is {@code null}
     */
    public void addListener(final IFieldListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener == null");
        }

        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    /**
     * @return the number of atoms in the field (always positive)
     */
    public int count() {
        return mContents.size();
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof Field) {
            // Fields A = (1 2 1 3) and B = (3 1 2 1) are identical, i.e., the index doesn't matter but order does.
            return rotations().contains(((Field) other).mContents);
        }

        return false;
    }

    @Override
    public int hashCode() {
        /*
         * Not a great hash, but one consistent with equals(). If this field, A, is the rotation of field B, then
         * sorting A and B will have the same result. That means that if A.equals(B), A.hashCode() == B.hashCode().
         * However, many non-equals() fields will have the same hashCode().
         *
         * With A = (1 2 1 3) and B = (3 1 2 1), A.equals(B) because B is just a rotation of A. Sorting A or B yields
         * (1 1 2 3), so A.hashCode() == B.hashCode(). However, for C = (2 3 1 1), !A.equals(C), but A.hashCode() ==
         * C.hashCode(), since C's sort is also (1 1 2 3).
         */
        return mContents.stream().map(ATOM_TO_INT).sorted().collect(toList()).hashCode();
    }

    /**
     * @param atom the atom to insert
     * @param index where to insert it
     * @throws IllegalArgumentException if the atom is {@code null}
     * @throws IndexOutOfBoundsException if the index is not in [0, {@link #count()}]
     */
    public void insert(final IAtom atom, final int index) {
        if (atom == null) {
            throw new IllegalArgumentException("atom == null");
        }

        mContents.add(index, atom);
        mListeners.forEach(listener -> listener.insert(index, atom));

        ReactionContext context = new ReactionContext(index);

        /*
         * We must deal with any dark plus first. At most one dark plus can exist in the field and only if there's
         * exactly one or two atoms (prior to another being inserted); otherwise the dark plus would have already
         * reacted per this code. Note that even if there's a dark plus in the field, and we're inserting a new dark
         * plus, the outcome will be the same whether we operate on the new dark plus or the old one. It's extremely
         * unlikely (even impossible?) anyone has ever seen two at once anyway.
         */
        if (atom == DARK_PLUS) {
            context = react(context, DarkPlusReaction.INSTANCE);
        } else if (context.getCounterclockwiseAtom() == DARK_PLUS) {
            context = react(new ReactionContext(context.getCounterclockwiseIndex()), DarkPlusReaction.INSTANCE);
        } else if (context.getClockwiseAtom() == DARK_PLUS) {
            context = react(new ReactionContext(context.getClockwiseIndex()), DarkPlusReaction.INSTANCE);
        } else if (atom == PLUS) {
            context = react(context, PlusReaction.INSTANCE);
        }

        reactAtAdjacentPlus(context);
    }

    /**
     * @param index the index to remove
     * @throws IndexOutOfBoundsException if the index is not in [0, {@link #count()})
     * @throws IllegalStateException if there is only one atom in the field
     */
    public void remove(final int index) {
        if (count() == 1) {
            throw new IllegalStateException("The field must not become empty");
        }

        /*
         * Note that if there is a dark plus on the field, it's either the only atom or one of only two. No matter what
         * is removed, the dark plus will not react with anything. So we can effectively ignore dark plus here.
         */

        mContents.remove(index);
        mListeners.forEach(listener -> listener.remove(index));

        ReactionContext context = new ReactionContext(index == count() ? index - 1 : index);

        if (context.getCenterAtom() == PLUS) {
            context = react(context, PlusReaction.INSTANCE);
        }

        reactAtAdjacentPlus(context);
    }

    /**
     * @param listener the listener to remove if present
     * @throws IllegalArgumentException if the argument is {@code null}
     */
    public void removeListener(final IFieldListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener == null");
        }

        mListeners.remove(listener);
    }

    @Override
    public String toString() {
        return mContents.stream().map(IAtom::toString).collect(Collectors.joining(" "));
    }

    /**
     * Updates the {@link Field} for a reaction using this context, notifying listeners.
     *
     * @param context the context in which a reaction occurred (must not be {@code null})
     * @param result the result of the reaction (must not be {@code null})
     * @return a new context for any subsequent reactions (never {@code null})
     */
    private ReactionContext adjustField(final ReactionContext context, final Atom result) {
        assert context != null;
        assert result != null;

        /*
         * The result goes in the center index, but two indices are removed; if those were below the center index,
         * it gets shifted counterclockwise.
         */
        final int resultIndex =
            context.getCenterIndex()
                - (context.getCounterclockwiseIndex() < context.getCenterIndex() ? 1 : 0)
                - (context.getClockwiseIndex() < context.getCenterIndex() ? 1 : 0);

        mContents.remove(Math.max(context.getCounterclockwiseIndex(), context.getClockwiseIndex()));
        mContents.remove(Math.min(context.getCounterclockwiseIndex(), context.getClockwiseIndex()));
        mContents.set(resultIndex, result);

        mListeners.forEach(listener ->
            listener.react(
                context.getCounterclockwiseIndex(),
                context.getCenterIndex(),
                context.getClockwiseIndex(),
                result,
                resultIndex));

        return new ReactionContext(resultIndex);
    }

    /**
     * The core algorithm: collapse atoms together whenever a supplied reaction applies, calculating a
     * reaction-dependent result. If the reaction does not apply, this does nothing.
     *
     * @param context the context that specifies where the reaction occurs (must not be {@code null})
     * @param reaction the reaction that might apply to that context (must not be {@code null})
     * @return the last context used (never {@code null})
     */
    private ReactionContext react(final ReactionContext context, final IReaction reaction) {
        assert context != null;
        assert reaction != null;

        ReactionContext loopContext = context;
        IReaction loopReaction = reaction;
        while (loopContext.getCounterclockwiseIndex() != loopContext.getClockwiseIndex()
            && loopReaction.isApplicable(loopContext)) {

            final Atom result = loopReaction.react(
                loopContext.getCounterclockwiseAtom(),
                loopContext.getCenterAtom(),
                loopContext.getClockwiseAtom());

            loopContext = adjustField(loopContext, result);

            // After the initial reaction, it's *must* be a plus-style reaction, though it may be inapplicable now.
            loopReaction = PlusReaction.INSTANCE;
        }

        return loopContext;
    }

    /**
     * Potentially starts a reaction, if a {@linkplain PeriodicTable#PLUS plus} is next to the center atom of the given
     * context (and of course if the {@link PlusReaction} is otherwise applicable). The counterclockwise direction is
     * preferred.
     *
     * <p>
     * Recursive.
     * </p>
     *
     * @param context the context in which to operate (must not be {@code null})
     */
    private void reactAtAdjacentPlus(final ReactionContext context) {
        assert context != null;

        if (context.getCounterclockwiseAtom() == PLUS) {
            reactAtAdjacentPlus(react(new ReactionContext(context.getCounterclockwiseIndex()), PlusReaction.INSTANCE));
        } else if (context.getClockwiseAtom() == PLUS) {
            reactAtAdjacentPlus(react(new ReactionContext(context.getClockwiseIndex()), PlusReaction.INSTANCE));
        }
    }

    /**
     * For <i>n</i> atoms, there are <i>n</i> possible rotations.
     *
     * @return all possible rotations of this field (never {@code null} and will not contain {@code null})
     */
    private Set<List<IAtom>> rotations() {
        final IntFunction<List<IAtom>> rotate = distance -> {
            final List<IAtom> copy = new LinkedList<>(mContents);
            Collections.rotate(copy, distance);
            return copy;
        };

        return IntStream.range(0, count()).mapToObj(rotate).collect(toSet());
    }

    /**
     * How {@linkplain PeriodicTable#DARK_PLUS dark plus} works.
     */
    @Immutable
    private static class DarkPlusReaction implements IReaction {
        /**
         * A single instance is sufficient.
         */
        public static final DarkPlusReaction INSTANCE = new DarkPlusReaction();

        /**
         * Private constructor to "require" use of {@link #INSTANCE}.
         */
        private DarkPlusReaction() {
            // Nothing to do.
        }

        @Override
        public boolean isApplicable(final ReactionContext context) {
            // Any two atoms can be fused when the center is a dark plus.
            return context.getCenterAtom() == DARK_PLUS;
        }

        @Override
        public Atom react(final IAtom counterclockwise, final IAtom center, final IAtom clockwise) {
            final int resultingAtomicNumber;
            if (counterclockwise == PLUS && clockwise == PLUS) {
                // Beryllium
                resultingAtomicNumber = 4;
            } else if (counterclockwise == PLUS) {
                // Not documented explicitly.
                resultingAtomicNumber = ((Atom) clockwise).getAtomicNumber() + 3;
            } else if (clockwise == PLUS) {
                // Not documented explicitly.
                resultingAtomicNumber = ((Atom) counterclockwise).getAtomicNumber() + 3;
            } else {
                resultingAtomicNumber = Math.max(
                    ((Atom) counterclockwise).getAtomicNumber(),
                    ((Atom) clockwise).getAtomicNumber()) + 3;
            }

            return atom(resultingAtomicNumber);
        }
    }

    /**
     * Determines if a reaction is possible and if so, how {@link IAtom}s combine.
     */
    private static interface IReaction {
        /**
         * One universal applicability fact is checked prior to this call: there are at least 3 atoms in the field.
         *
         * @param context the context in which a reaction may occur (must not be {@code null})
         * @return {@code true} if this reactor will cause a reaction
         */
        public boolean isApplicable(ReactionContext context);

        /**
         * Calculates a new atom from the combination of the three atoms involved in a reaction. Called only {@linkplain
         * #isApplicable(ReactionContext) if applicable}.
         *
         * @param counterclockwise the atom counterclockwise from the center (must not be {@code null})
         * @param center the atom of interest (must not be {@code null})
         * @param clockwise the atom clockwise from the center (must not be {@code null})
         * @return the resulting {@link Atom} (never {@code null})
         */
        public Atom react(IAtom counterclockwise, IAtom center, IAtom clockwise);
    }

    /**
     * How {@linkplain PeriodicTable#PLUS plus} works.
     */
    @Immutable
    private static class PlusReaction implements IReaction {
        /**
         * A single instance is sufficient.
         */
        public static final PlusReaction INSTANCE = new PlusReaction();

        /**
         * Private constructor to "require" use of {@link #INSTANCE}.
         */
        private PlusReaction() {
            // Nothing to do.
        }

        @Override
        public boolean isApplicable(final ReactionContext context) {
            /*
             * Could check adjacent atom equality first, then make sure just one adjacent atom is a regular atom. The
             * center atom must be either plus (when starting a reaction) or a regular Atom (as a reaction continues).
             */
            return context.getCounterclockwiseAtom() != PLUS
                && context.getCounterclockwiseAtom() != DARK_PLUS
                && context.getClockwiseAtom() != PLUS
                && context.getClockwiseAtom() != DARK_PLUS
                && context.getCenterAtom() != DARK_PLUS
                && context.getCounterclockwiseAtom() == context.getClockwiseAtom();
        }

        @Override
        public Atom react(final IAtom counterclockwise, final IAtom center, final IAtom clockwise) {
            // Per applicability. Either adjacent atom would do; we've guaranteed they're the same.
            assert counterclockwise instanceof Atom;
            final int adjacentAtomicNumber = ((Atom) counterclockwise).getAtomicNumber();

            // Per applicability.
            assert center != DARK_PLUS;

            if (center == PLUS) {
                return atom(adjacentAtomicNumber + 1);
            } else {
                final Atom centerAtom = (Atom) center;

                if (adjacentAtomicNumber < centerAtom.getAtomicNumber()) {
                    return atom(centerAtom.getAtomicNumber() + 1);
                } else {
                    return atom(adjacentAtomicNumber + 2);
                }

                // FIXME: WHAT'S THIS DOCUMENTATION FROM THE WEBSITE?
                /*
                 * A forced +1 reaction, in which a Plus Atom is placed at the end of the reaction, making the
                 * fusion merge faster and eliminating +2 reactions.
                 */
            }
        }
    }

    /**
     * Specifies where the reaction occurs.
     */
    @Immutable
    private final class ReactionContext {
        /**
         * @see #getCenterAtom()
         */
        private final IAtom mCenterAtom;

        /**
         * @see #ReactionContext(int)
         */
        private final int mCenterIndex;

        /**
         * @see #getClockwiseAtom()
         */
        private final IAtom mClockwiseAtom;

        /**
         * @see #getClockwiseIndex()
         */
        private final int mClockwiseIndex;

        /**
         * @see #getCounterclockwiseAtom()
         */
        private final IAtom mCounterclockwiseAtom;

        /**
         * @see #getCounterclockwiseIndex()
         */
        private final int mCounterclockwiseIndex;

        /**
         * @param centerIndex see {@link #getCenterIndex()} (must be valid)
         */
        public ReactionContext(final int centerIndex) {
            assert centerIndex >= 0 && centerIndex < count();

            mCenterIndex = centerIndex;
            mCenterAtom = mContents.get(mCenterIndex);

            mCounterclockwiseIndex = (mCenterIndex - 1 + count()) % count();
            mCounterclockwiseAtom = mContents.get(mCounterclockwiseIndex);

            mClockwiseIndex = (mCenterIndex + 1) % count();
            mClockwiseAtom = mContents.get(mClockwiseIndex);
        }

        /**
         * @return the first atom clockwise from the center atom (never {@code null})
         */
        public IAtom getClockwiseAtom() {
            return mClockwiseAtom;
        }

        /**
         * @return the first index clockwise from the center atom (always valid at rule construction)
         */
        public int getClockwiseIndex() {
            return mClockwiseIndex;
        }

        /**
         * @return the first atom counterclockwise from the center atom (never {@code null})
         */
        public IAtom getCounterclockwiseAtom() {
            return mCounterclockwiseAtom;
        }

        /**
         * @return the first index counterclockwise from the center atom (always valid at rule construction)
         */
        public int getCounterclockwiseIndex() {
            return mCounterclockwiseIndex;
        }

        /**
         * @return the atom at the index of interest (never {@code null})
         */
        public IAtom getCenterAtom() {
            return mCenterAtom;
        }

        /**
         * @return the index of interest (always valid at construction)
         */
        public int getCenterIndex() {
            return mCenterIndex;
        }
    }
}
