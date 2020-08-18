package atomas;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

import static atomas.PeriodicTable.DARK_PLUS;
import static atomas.PeriodicTable.PLUS;
import static atomas.PeriodicTable.atom;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * JUnit 4.x test cases for {@link Field}.
 */
public class FieldTest {
    @Test(expected = IllegalArgumentException.class)
    public void testAddListenerNullRestriction() {
        field(1, 2, 3).addListener(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorEmptyRestriction() {
        new Field(emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullAtomRestriction() {
        new Field(asList(atom(1), null, atom(2)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullRestriction() {
        new Field(null);
    }

    @Test
    public void testEquality() {
        final BiConsumer<Field, Field> test = (field1, field2) -> {
            Assert.assertEquals(field1, field2);
            Assert.assertEquals(field1.hashCode(), field2.hashCode());

            // Exercise toString().
            System.out.println(field1.toString() + " = " + field2.toString());
        };

        // Simplest cases.
        test.accept(field(1), field(1));
        test.accept(field(1, 2), field(2, 1));

        final Field field1 = field(1, 2, 3, 1);
        final Field field2 = field(1, 1, 2, 3);
        final Field field3 = field(3, 1, 1, 2);
        final Field field4 = field(2, 3, 1, 1);

        test.accept(field1, field1);
        test.accept(field1, field2);
        test.accept(field1, field3);
        test.accept(field1, field4);

        test.accept(field2, field1);
        test.accept(field2, field2);
        test.accept(field2, field3);
        test.accept(field2, field4);

        test.accept(field3, field1);
        test.accept(field3, field2);
        test.accept(field3, field3);
        test.accept(field3, field4);

        test.accept(field4, field1);
        test.accept(field4, field2);
        test.accept(field4, field3);
        test.accept(field4, field4);

        // 10 tests.
        final Random random = new Random(System.currentTimeMillis());
        IntStream.range(0, 10).forEach(iteration -> {
            // 10-20 atoms, from anywhere in PeriodicTable.
            final List<Atom> atoms =
                random.ints(random.nextInt(10) + 10, 1, PeriodicTable.max().getAtomicNumber() + 1)
                    .mapToObj(PeriodicTable::atom)
                    .collect(toList());

            final Field field = new Field(atoms);

            // 10 random rotations and comparisons.
            IntStream.range(0, 10).forEach(rotationNumber -> {
                Collections.rotate(atoms, random.nextInt());
                test.accept(field, new Field(atoms));
            });
        });

        // Tests for a field that includes plus

        final Field fieldWithPlus1 = field(1, 2, 3);
        fieldWithPlus1.insert(PLUS, 1);

        final Field fieldWithPlus2 = field(3, 1, 2);
        fieldWithPlus2.insert(PLUS, 2);

        test.accept(fieldWithPlus1, fieldWithPlus2);

        // Tests for a field that includes dark plus

        final Field fieldWithDarkPlus1 = field(17);
        fieldWithDarkPlus1.insert(DARK_PLUS, 1);

        final Field fieldWithDarkPlus2 = field(17);
        fieldWithDarkPlus2.insert(DARK_PLUS, 0);

        test.accept(fieldWithDarkPlus1, fieldWithDarkPlus2);
    }

    @Test
    public void testInequality() {
        Assert.assertNotEquals(field(1), field(2));
        Assert.assertNotEquals(field(1, 2), field(1, 3));
        Assert.assertNotEquals(field(1, 2, 3), field(1, 3, 2));
        Assert.assertNotEquals(field(1), "wrong type");
    }

    @Test
    public void testInsertAtom() {
        // FIXME IMPLEMENT, INCLUDING INSERT NEXT TO A PLUS THAT SHOULD THEN WORK (CCW AND CW)
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testInsertBadIndexRestriction1() {
        field(1, 2, 3).insert(atom(1), -1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testInsertBadIndexRestriction2() {
        field(1, 2, 3).insert(atom(1), 4);
    }

    @Test
    public void testInsertDarkPlus() {
        final Field field = field(1, 2, 3, 4);

        final IFieldListener listener = mock(IFieldListener.class);
        field.addListener(listener);

        // 1 2 (+) 3 4
        field.insert(DARK_PLUS, 2);

        Assert.assertEquals(field(1, 6, 4), field);
        verify(listener).insert(2, DARK_PLUS);
        verify(listener).react(1, 2, 3, atom(6), 1);

        // FIXME OH SO MUCH MORE
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertNullAtomRestriction() {
        field(1, 2, 3).insert(null, 0);
    }

    @Test
    public void testInsertPlus() {
        final Field field = field(1, 2, 3, 1);

        final IFieldListener listener = mock(IFieldListener.class);
        field.addListener(listener);

        // + 1 2 3 1
        field.insert(PLUS, 0);

        Assert.assertEquals(field(2, 2, 3), field);
        verify(listener).insert(0, PLUS);
        verify(listener).react(4, 0, 1, atom(2), 0);

        // FIXME OH SO MUCH MORE
    }

    @Test
    public void testRemove() {
        // FIXME IMPLEMENT, INCLUDING CASE WHERE REMOVAL CAUSES A PLUS TO WORK (CCW AND CW)
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRemoveBadIndexRestriction1() {
        field(1, 2, 3).remove(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRemoveBadIndexRestriction2() {
        field(1, 2, 3).remove(3);
    }

    @Test(expected = IllegalStateException.class)
    public void testRemoveNonEmptyRestriction() {
        field(1).remove(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveListenerNullRestriction() {
        field(1, 2, 3).removeListener(null);
    }

    /**
     * @param atomicNumbers the atomic numbers (must not be {@code null} or contain numbers less than 1 or otherwise
     *     outside the periodic table)
     * @return a field with the given {@link Atom}s (never {@code null})
     */
    private static Field field(final int... atomicNumbers) {
        return new Field(Arrays.stream(atomicNumbers).mapToObj(PeriodicTable::atom).collect(toList()));
    }
}
