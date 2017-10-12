/*
 * $Id: Skeleton.java 2167 2017-07-27 12:36:26Z euzenat $
 *
 * Copyright (C) INRIA, 2006-2008, 2010, 2017
 *
 * Modifications to the initial code base are copyright of their
 * respective authors, or their employers as appropriate.  Authorship
 * of the modifications may be determined from the ChangeLog placed at
 * the end of this file.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

// Alignment API classes
package com.camendoza94.semanticinterface;

import fr.inrialpes.exmo.align.impl.BasicParameters;
import fr.inrialpes.exmo.align.impl.method.StringDistAlignment;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import org.semanticweb.owl.align.*;

// SAX standard classes
import org.xml.sax.SAXException;

// Java standard classes
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.util.Properties;

/**
 * The Skeleton of code for embeding the alignment API
 *
 * Takes two files as arguments and align them.
 */

public class Skeleton {

    public static void main( String[] args ) {
        URI onto1 = null; //TODO Connect to Fuseki and get both ontologies
        URI onto2 = null;
        Properties params = new BasicParameters();

        try {
            // Loading ontologies
            if (args.length >= 2) {
                onto1 = new URI( args[0] );
                onto2 = new URI( args[1] );
            } else {
                System.err.println("Need two arguments to proceed");
                return ;
            }

            // Aligning
            AlignmentProcess a1 = new StringDistAlignment();
            a1.init ( onto1, onto2 );
            a1.align( (Alignment)null, params );

            // Outputing
            PrintWriter writer = new PrintWriter (
                    new BufferedWriter(
                            new OutputStreamWriter( System.out, StandardCharsets.UTF_8.name() )), true);
            AlignmentVisitor renderer = new RDFRendererVisitor(writer);
            a1.render(renderer); //TODO Upload it to Fuseki
            writer.flush();
            writer.close();

        } catch (Exception e) { e.printStackTrace(); };
    }
}