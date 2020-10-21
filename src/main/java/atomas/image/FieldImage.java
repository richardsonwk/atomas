package atomas.image;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import javax.imageio.ImageIO;

import net.jcip.annotations.Immutable;

import atomas.Atom;
import atomas.Field;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;

/**
 * Represents an image of the Atomas {@link Field}.
 */
public class FieldImage {
    /**
     * Private constructor to require use of factory method.
     */
    private FieldImage() {
        // FIXME: DO WE EVEN NEED THIS? MAYBE THIS IS MORE OF A UTILITY.
        // BUT MAYBE WE WANT THE FIELD *AND* THE CENTER ATOM.
    }

    /**
     * @param imagePath the path to the image
     * @return a new image (never {@code null})
     * @throws IllegalArgumentException if the argument is {@code null}
     * @throws IOException if the file can't be read
     * @throws UnrecognizableFieldException if the image was read but does not appear to contain a {@link Field}
     */
    public static FieldImage read(final Path imagePath) throws IOException, UnrecognizableFieldException {
        if (imagePath == null) {
            throw new IllegalArgumentException("imagePath == null");
        }

        final BufferedImage image = ImageIO.read(imagePath.toFile());

        getAtomCircles(image);

        // FIXME
        //throw new UnrecognizableFieldException("Some kinda failure");
        return new FieldImage();
    }

    /**
     * <a href="https://boofcv.org/index.php?title=Example_Fit_Ellipse">See BoofCV sample.</a>
     *
     * @param image the image on which to operate (must not be {@code null})
     * @return the list of circles in the image that correspond to atoms (never {@code null} and will not contain {@code
     *     null})
     */
    private static List<AtomCircle> getAtomCircles(final BufferedImage image) {
        assert image != null;

        final GrayF32 input = ConvertBufferedImage.convertFromSingle(image, null, GrayF32.class);

        // FIXME
        ShowImages.showWindow(input, ShowImages.Colorization.MAGNITUDE, "input", true);

        // create a binary image by thresholding
        // the mean pixel value is often a reasonable threshold when creating a binary image
        //final double mean = ImageStatistics.mean(input);
        //final GrayU8 binary = new GrayU8(input.width, input.height);
        //ThresholdImageOps.threshold(input, binary, (float) mean, true);
        //ShowImages.showWindow(binary, ShowImages.Colorization.MAGNITUDE, "binary", true);

        // reduce noise with some filtering
        //final GrayU8 filtered = BinaryImageOps.dilate8(BinaryImageOps.erode8(binary, 1, null), 1, null);
        //ShowImages.showWindow(filtered, ShowImages.Colorization.MAGNITUDE, "filtered", true);

//        final List<Contour> contours = BinaryImageOps.contour(filtered, ConnectRule.EIGHT, null);
//
//        final List<EllipseRotated_F64> ellipses = contours.stream()
//            .map(contour -> ShapeFittingOps.fitEllipse_I32(contour.external, 0, false, null))
//            .map(fitData -> fitData.shape)
//            .collect(toList());
//
//        final Graphics2D g2d = image.createGraphics();
//        g2d.setStroke(new BasicStroke(3));
//        g2d.setColor(Color.RED);
//        ellipses.forEach(ellipse -> VisualizeShapes.drawEllipse(ellipse, g2d));
//        ShowImages.showWindow(image, "Ellipses", true);
//        try {
//            Thread.sleep(20 * 1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        return ellipses.stream()
//            .peek(shape -> System.out.println("semimajor = " + shape.a))
//            .filter(shape -> DoubleMath.fuzzyEquals(shape.getA(), shape.getB(), 1))
//            .map(shape -> new AtomCircle(
//                new Point((int) shape.center.x, (int) shape.center.y),
//                (int) Math.rint(shape.getA())))
//            .collect(toList());
        return null;
    }

    /**
     * Represents the circle of an {@link Atom} in the {@link FieldImage}.
     */
    @Immutable
    private static final class AtomCircle {
        /**
         * @see #getCenter()
         */
        private final Point mCenter;

        /**
         * @see #getRadius()
         */
        private final int mRadius;

        // FIXME: PROBABLY COLOR...

        /**
         * @param center see {@link #getCenter()} (must not be {@code null})
         * @param radius see {@link #getRadius()} (must be positive)
         */
        public AtomCircle(final Point center, final int radius) {
            assert center != null;
            assert radius > 0;

            mCenter = center;
            mRadius = radius;
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            final AtomCircle typedOther = (AtomCircle) other;

            return mRadius == typedOther.mRadius && mCenter.equals(typedOther.mCenter);
        }

        /**
         * @return the center of the circle in pixels (never {@code null})
         */
        public Point getCenter() {
            return new Point(mCenter);
        }

        /**
         * @return the radius of the circle in pixels (always positive)
         */
        public int getRadius() {
            return mRadius;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mCenter, mRadius);
        }

        @Override
        public String toString() {
            return "(" + mCenter.x + ", " + mCenter.y + ") " + mRadius;
        }
    }
}
