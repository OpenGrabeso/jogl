/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package com.github.opengrabeso.ogltext.font.typecast.ot.table;

import java.io.IOException;
import java.io.DataInput;

/**
 *
 * @version $Id: DsigTable.java,v 1.1.1.1 2004-12-05 23:14:37 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class DsigTable implements Table {

    private final com.github.opengrabeso.ogltext.font.typecast.ot.table.DirectoryEntry de;
    private final int version;
    private final int numSigs;
    private final int flag;
    private final com.github.opengrabeso.ogltext.font.typecast.ot.table.DsigEntry[] dsigEntry;
    private final com.github.opengrabeso.ogltext.font.typecast.ot.table.SignatureBlock[] sigBlocks;

    /** Creates new DsigTable */
    protected DsigTable(final com.github.opengrabeso.ogltext.font.typecast.ot.table.DirectoryEntry de, final DataInput di) throws IOException {
        this.de = (com.github.opengrabeso.ogltext.font.typecast.ot.table.DirectoryEntry) de.clone();
        version = di.readInt();
        numSigs = di.readUnsignedShort();
        flag = di.readUnsignedShort();
        dsigEntry = new com.github.opengrabeso.ogltext.font.typecast.ot.table.DsigEntry[numSigs];
        sigBlocks = new com.github.opengrabeso.ogltext.font.typecast.ot.table.SignatureBlock[numSigs];
        for (int i = 0; i < numSigs; i++) {
            dsigEntry[i] = new DsigEntry(di);
        }
        for (int i = 0; i < numSigs; i++) {
            sigBlocks[i] = new SignatureBlock(di);
        }
    }

    /**
     * Get the table type, as a table directory value.
     * @return The table type
     */
    @Override
    public int getType() {
        return DSIG;
    }

    /**
     * Get a directory entry for this table.  This uniquely identifies the
     * table in collections where there may be more than one instance of a
     * particular table.
     * @return A directory entry
     */
    @Override
    public DirectoryEntry getDirectoryEntry() {
        return de;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder().append("DSIG\n");
        for (int i = 0; i < numSigs; i++) {
            sb.append(sigBlocks[i].toString());
        }
        return sb.toString();
    }
}
