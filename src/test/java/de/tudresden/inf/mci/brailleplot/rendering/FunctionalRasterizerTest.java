package de.tudresden.inf.mci.brailleplot.rendering;

import de.tudresden.inf.mci.brailleplot.configparser.ConfigurationParser;
import de.tudresden.inf.mci.brailleplot.configparser.Format;
import de.tudresden.inf.mci.brailleplot.configparser.JavaPropertiesConfigurationParser;
import de.tudresden.inf.mci.brailleplot.configparser.Printer;
import de.tudresden.inf.mci.brailleplot.layout.RasterCanvas;
import de.tudresden.inf.mci.brailleplot.layout.SixDotBrailleRasterCanvas;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;


public class FunctionalRasterizerTest {

    public static final String mDefaultConfig = getResource("config/rasterizer_test_default.properties").getAbsolutePath();
    public static final String mBaseConfig = getResource("config/base_format.properties").getAbsolutePath();
    public static Printer mPrinter;
    public static Format mFormat;

    public static File getResource(String fileName) {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        File resourceFile = new File(classLoader.getResource(fileName).getFile());
        return resourceFile;
    }

    @BeforeAll
    public static void initialize() {
        Assertions.assertDoesNotThrow(
                () -> {
                    ConfigurationParser parser = new JavaPropertiesConfigurationParser(mBaseConfig, mDefaultConfig);
                    mPrinter = parser.getPrinter();
                    mFormat = parser.getFormat("test");
                }
        );
    }

    // Invalid use test cases.

    @Test
    public void testInvalidDirectCall() {
        // Create FunctionalRasterizer for BrailleText
        FunctionalRasterizer<BrailleText> textRasterizer = new FunctionalRasterizer<>(BrailleText.class, (data, canvas) -> {
            // dummy
        });

        // The FunctionalRasterizer should not be called directly, it is meant to be called by its RenderingBase
        // which decides which rasterizer to use based on the Renderable type.
        // Directly passing the wrong Renderable type must cause exception:
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            RasterCanvas testCanvas = new SixDotBrailleRasterCanvas(mPrinter, mFormat);
            // Pass Image to BrailleText rasterizer.
            textRasterizer.rasterize(new Image(getResource("examples/img/dummy.bmp")), testCanvas);
        });
    }
}
