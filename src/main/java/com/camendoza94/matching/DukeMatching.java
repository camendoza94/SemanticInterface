package com.camendoza94.matching;


import no.priv.garshol.duke.*;
import no.priv.garshol.duke.matchers.AbstractMatchListener;
import no.priv.garshol.duke.utils.LinkFileWriter;
import org.xml.sax.SAXException;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

//Modified code from Duke https://github.com/larsga/Duke
public class DukeMatching {
    public static void matching() throws IOException, SAXException {
        Configuration config = ConfigLoader.load("src/data/semantic.xml");
        Processor proc = new Processor(config);
        LinkFileListener linkfile = new LinkFileListener("src/data/link.csv", config.getIdentityProperties());
        proc.addMatchListener(linkfile);
        proc.link();
        proc.close();
    }

    static class LinkFileListener extends AbstractLinkFileListener {
        private Writer out;
        private LinkFileWriter writer;

        LinkFileListener(String linkfile, Collection<Property> idprops)
                throws IOException {
            super(idprops);
            this.out = new FileWriter(linkfile, false);
            this.writer = new LinkFileWriter(out);
        }

        public void link(String id1, String id2) throws IOException {
            writer.write(id1, id2, true, 1.0);
            out.flush(); // make sure we preserve the data
        }
    }

    static abstract class AbstractLinkFileListener extends AbstractMatchListener {
        private Collection<Property> idprops;

        AbstractLinkFileListener(Collection<Property> idprops) {
            this.idprops = idprops;
        }

        public abstract void link(String id1, String id2) throws IOException;

        public void matches(Record r1, Record r2, double confidence) {
            try {
                for (Property p : idprops)
                    for (String id1 : r1.getValues(p.getName()))
                        for (String id2 : r2.getValues(p.getName()))
                            link(id1, id2);
            } catch (IOException e) {
                throw new DukeException(e);
            }
        }
    }
}
