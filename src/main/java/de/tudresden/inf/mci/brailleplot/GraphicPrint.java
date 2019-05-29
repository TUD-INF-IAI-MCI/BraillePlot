package de.tudresden.inf.mci.brailleplot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


/**
 * Placeholder.
 */
public class GraphicPrint extends AbstractDocumentBuilder {

    private final byte[] mEnterIMageMode = new byte[] {0x1B, 0x09};
    private final byte[] mExitImageMode = new byte[] {0x1B, 0x0A};
    private final byte[] mSetImageType = new byte[] {0x1B, 0x0B};

    public GraphicPrint(final byte[] data) {
        this.mDocument = this.assemble(data);
    }

    /**
     * Assemble the Document. Hides the Protocol from the user.
     * @return
     */
    public byte[] assemble(final byte[] data) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(mEnterIMageMode);
            stream.write(mSetImageType);

            //


            stream.write(mExitImageMode);
        } catch (IOException e) {
            e.getMessage();
        }
        mDocument = stream.toByteArray();
        return mDocument;
    }

}
