package atomas;

/**
 * Listener for changes to the {@link Field}. <strong>Must not throw.</strong>
 */
public interface IFieldListener {
    /**
     * @param index where the atom was inserted (must be in [0, {@link Field#count()}] <em>prior to the insert</em>)
     * @param atom the atom that was inserted (must not be {@code null})
     */
    void insert(int index, IAtom atom);

    /**
     * @param ccwIndex the index counterclockwise from the center index (must be in [0, {@link Field#count()})
     *     <em>prior to the reaction</em>)
     * @param centerIndex the index which reacted with the indices on either side (must be in [0, {@link
     *     Field#count()}) <em>prior to the reaction</em>)
     * @param cwIndex the index clockwise from the center index (must be in [0, {@link Field#count()}) <em>prior to
     *     the reaction</em>)
     * @param result the resulting atom (must not be {@code null})
     * @param resultIndex where the result is (must be in [0, {@link Field#count()}) <em>after the reaction</em>)
     */
    void react(int ccwIndex, int centerIndex, int cwIndex, IAtom result, int resultIndex);

    /**
     * @param index the index of the removed atom (must be in [0, {@link Field#count()}) <em>prior</em> to the
     *     remove)
     */
    void remove(int index);
}
