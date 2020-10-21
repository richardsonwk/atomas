package atomas.image;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.google.common.math.DoubleMath;

import org.junit.Test;

import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.shapes.FitData;
import boofcv.alg.shapes.ShapeFittingOps;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import georegression.struct.curve.EllipseRotated_F64;

/**
 * JUnit 4.x test cases for {@link FieldImage}.
 */
public class FieldImageTest {
    private static final String ROOT = "C:\\Users\\wkr\\Desktop\\personal\\atomas-screenshots";

    @Test
    public void test() throws IOException, UnrecognizableFieldException {
        final FieldImage image = FieldImage.read(Path.of(ROOT, "typical.png"));

        // FIXME
    }

    public static void main(String args[]) {
        BufferedImage image0 = UtilImageIO.loadImage(ROOT + "/packed.png");
        BufferedImage image =
            image0.getSubimage(0, image0.getHeight() / 4, image0.getWidth(), 55 * image0.getHeight() / 100);

        GrayU8 gray = ConvertBufferedImage.convertFrom(image, (GrayU8) null);
        GrayU8 edgeImage = gray.createSameShape();

        // Create a canny edge detector which will dynamically compute the threshold based on maximum edge intensity
        // It has also been configured to save the trace as a graph.  This is the graph created while performing
        // hysteresis thresholding.
        CannyEdge<GrayU8, GrayS16> canny = FactoryEdgeDetectors.canny(1, true, true, GrayU8.class, GrayS16.class);

        // The edge image is actually an optional parameter.  If you don't need it just pass in null
        canny.process(gray, 0.1f, 0.1f, edgeImage);

        // First get the contour created by canny
        //List<EdgeContour> edgeContours = canny.getContours();
        // The 'edgeContours' is a tree graph that can be difficult to process.  An alternative is to extract
        // the contours from the binary image, which will produce a single loop for each connected cluster of pixels.
        // Note that you are only interested in external contours.
        List<Contour> contours = BinaryImageOps.contourExternal(edgeImage, ConnectRule.EIGHT);

        // Fit an ellipse to each external contour and draw the results
        Graphics2D g2 = image.createGraphics();
        g2.setStroke(new BasicStroke(3));
        g2.setColor(Color.RED);

        int count = 0;
        for (Contour c : contours) {
            FitData<EllipseRotated_F64> ellipse = ShapeFittingOps.fitEllipse_I32(c.external, 0, false, null);
            if (DoubleMath.fuzzyEquals(ellipse.shape.a, ellipse.shape.b, 1)
                && DoubleMath.fuzzyEquals(ellipse.shape.a, image0.getWidth() / 8 / 2, 10)) {

                ++count;
                VisualizeShapes.drawEllipse(ellipse.shape, g2);
            }
        }

        System.out.println(count);

        //		ShowImages.showWindow(VisualizeBinaryData.renderBinary(filtered, false, null),"Binary",true);
        ShowImages.showWindow(image, "Ellipses", true);

        // display the results
        //BufferedImage visualBinary = VisualizeBinaryData.renderBinary(edgeImage, false, null);
        //        BufferedImage visualCannyContour = VisualizeBinaryData.renderContours(edgeContours, null,
        //            gray.width, gray.height, null);
        //        BufferedImage visualEdgeContour = new BufferedImage(gray.width, gray.height, BufferedImage.TYPE_INT_RGB);
        //VisualizeBinaryData.render(contours, (int[]) null, visualEdgeContour);

        //ListDisplayPanel panel = new ListDisplayPanel();
        //panel.addImage(visualBinary, "Binary Edges from Canny");
        //        panel.addImage(visualCannyContour, "Canny Trace Graph");
        //        panel.addImage(visualEdgeContour, "Contour from Canny Binary");
        //ShowImages.showWindow(panel, "Canny Edge", true);
    }
}
