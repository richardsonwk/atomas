package atomas;

import org.junit.Assert;
import org.junit.Test;

import static atomas.PeriodicTable.DARK_PLUS;
import static atomas.PeriodicTable.PLUS;
import static atomas.PeriodicTable.atom;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * JUnit 4.x test cases for {@link Field}.
 */
public class FieldTest {
    @Test
    public void test() {
        final IFieldListener listener = mock(IFieldListener.class);

        // 1 2 3 1
        final Field field = new Field(asList(atom(1), atom(2), atom(3), atom(1)), listener);
        System.out.println(field);

        Assert.assertEquals(4, field.count());

        // + 1 2 3 1
        field.insert(PLUS, 0);
        System.out.println(field);

        verify(listener).insert(0, PLUS);
        verify(listener).react(4, 0, 1, atom(2), 0);
        Assert.assertEquals(3, field.count());
    }

    @Test
    public void testDarkPlus() {
        final IFieldListener listener = mock(IFieldListener.class);

        // 1 2 3 4
        final Field field = new Field(asList(atom(1), atom(2), atom(3), atom(4)), listener);
        System.out.println(field);

        // 1 2 (+) 3 4
        field.insert(DARK_PLUS, 2);
        System.out.println(field);

        verify(listener).insert(2, DARK_PLUS);
        verify(listener).react(1, 2, 3, atom(6), 1);
        Assert.assertEquals(3, field.count());
    }
}
