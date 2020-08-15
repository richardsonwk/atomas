package atomas;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;

import static atomas.PeriodicTable.DARK_PLUS;
import static atomas.PeriodicTable.PLUS;
import static atomas.PeriodicTable.atom;

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
     * Internal representation of the atoms on the board.
     */
    private final List<IAtom> mContents = new LinkedList<>();

    /**
     * @see #Field(List, IFieldListener)
     */
    private final IFieldListener mListener;

    /**
     * @param initialContents the initial set of contents
     * @param listener a listener to notify of changes to the field // FIXME: WHY NOT USUAL ADD LISTENER?
     * @throws IllegalArgumentException if the list is {@code null}, empty, or contains {@code null}
     * @throws IllegalArgumentException if the listener is {@code null}
     */
    public Field(final List<IAtom> initialContents, final IFieldListener listener) {
        if (initialContents == null) {
            throw new IllegalArgumentException("initialContents == null");
        }
        if (initialContents.isEmpty()) {
            throw new IllegalArgumentException("Field requires at least one atom");
        }
        if (initialContents.contains(null)) {
            throw new IllegalArgumentException("initialContents contains null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener == null");
        }

        mContents.addAll(initialContents);
        mListener = listener;
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
            return mContents.equals(((Field) other).mContents);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mContents);
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
        mListener.insert(index, atom);

        final ReactionContext context = new ReactionContext(index);

        /*
         * We must deal with any dark plus first. At most one dark plus can exist in the field and only if there's
         * exactly one or two atoms (prior to another being inserted); otherwise the dark plus would have already
         * reacted per this code. Note that even if there's a dark plus in the field, and we're inserting a new dark
         * plus, the outcome will be the same whether we operate on the new dark plus or the old one. It's extremely
         * unlikely (even impossible?) anyone has ever seen two at once anyway.
         */
        if (atom == DARK_PLUS) {
            react(context, new DarkPlusReaction());
        } else if (context.getCounterclockwiseAtom() == DARK_PLUS) {
            react(new ReactionContext(context.getCounterclockwiseIndex()), new DarkPlusReaction());
        } else if (context.getClockwiseAtom() == DARK_PLUS) {
            react(new ReactionContext(context.getClockwiseIndex()), new DarkPlusReaction());
        } else if (atom == PLUS) {
            react(context, new PlusReaction());
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
            throw new IllegalStateException("The field must contain at least one atom");
        }

        /*
         * Note that if there is a dark plus on the field, it's either the only atom or one of only two. No matter what
         * is removed, the dark plus will not react with anything. So we can effectively ignore dark plus here.
         */

        mContents.remove(index);
        mListener.remove(index);

        final ReactionContext context = new ReactionContext(index == count() ? index - 1 : index);

        if (context.getCenterAtom() == PLUS) {
            react(context, new PlusReaction());
        }

        reactAtAdjacentPlus(context);
    }

    @Override
    public String toString() {
        return mContents.stream().map(IAtom::toString).collect(Collectors.joining(" "));
    }

    /**
     * How {@linkplain PeriodicTable#DARK_PLUS dark plus} works.
     */
    @Immutable
    private static class DarkPlusReaction implements IReaction {
        /**
         * @see DarkPlusReaction
         */
        public DarkPlusReaction() {
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
         * @see PlusReaction
         */
        public PlusReaction() {
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

        mListener.react(
            context.getCounterclockwiseIndex(),
            context.getCenterIndex(),
            context.getClockwiseIndex(),
            result,
            resultIndex);

        return new ReactionContext(resultIndex);
    }

    /**
     * The core algorithm: collapse atoms together whenever a supplied reaction applies, calculating a
     * reaction-dependent result. If the reaction does not apply, this does nothing.
     *
     * @param context the context that specifies where the reaction occurs (must not be {@code null})
     * @param reaction the reaction that might apply to that context (must not be {@code null})
     */
    private void react(final ReactionContext context, final IReaction reaction) {
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
            loopReaction = new PlusReaction();
        }
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
            react(new ReactionContext(context.getCounterclockwiseIndex()), new PlusReaction());
            reactAtAdjacentPlus(context);
        } else if (context.getClockwiseAtom() == PLUS) {
            react(new ReactionContext(context.getClockwiseIndex()), new PlusReaction());
            reactAtAdjacentPlus(context);
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
