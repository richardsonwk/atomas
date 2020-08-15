package atomas;

/**
 * Visitor pattern interface for {@link IAtom}.
 */
public interface IAtomVisitor {
    /**
     * @param atom the atom (must not be {@code null})
     * @see Atom
     */
    void visitAtom(Atom atom);

    /**
     * @see PeriodicTable#DARK_PLUS
     */
    void visitDarkPlus();

    /**
     * @see PeriodicTable#PLUS
     */
    void visitPlus();
}
