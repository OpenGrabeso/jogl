/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Batik" and  "Apache Software Foundation" must  not  be
    used to  endorse or promote  products derived from  this software without
    prior written permission. For written permission, please contact
    apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation. For more  information on the
 Apache Software Foundation, please see <http://www.apache.org/>.

*/

package com.opengrabeso.ogltext.font.typecast.ot;

import java.io.DataInputStream;
import java.io.IOException;

import com.opengrabeso.ogltext.font.typecast.ot.table.*;
import com.opengrabeso.ogltext.font.typecast.ot.table.VheaTable;


/**
 * The TrueType font.
 * @version $Id: OTFont.java,v 1.6 2007-01-31 01:49:18 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>, Sven Gothel
 */
public class OTFont {

    private final OTFontCollection _fc;
    private com.opengrabeso.ogltext.font.typecast.ot.table.TableDirectory _tableDirectory = null;
    private com.opengrabeso.ogltext.font.typecast.ot.table.Table[] _tables;
    private com.opengrabeso.ogltext.font.typecast.ot.table.Os2Table _os2;
    private com.opengrabeso.ogltext.font.typecast.ot.table.CmapTable _cmap;
    private com.opengrabeso.ogltext.font.typecast.ot.table.GlyfTable _glyf;
    private com.opengrabeso.ogltext.font.typecast.ot.table.HeadTable _head;
    private com.opengrabeso.ogltext.font.typecast.ot.table.HheaTable _hhea;
    private com.opengrabeso.ogltext.font.typecast.ot.table.HdmxTable _hdmx;
    private com.opengrabeso.ogltext.font.typecast.ot.table.HmtxTable _hmtx;
    private com.opengrabeso.ogltext.font.typecast.ot.table.LocaTable _loca;
    private com.opengrabeso.ogltext.font.typecast.ot.table.MaxpTable _maxp;
    private com.opengrabeso.ogltext.font.typecast.ot.table.NameTable _name;
    private com.opengrabeso.ogltext.font.typecast.ot.table.PostTable _post;
    private VheaTable _vhea;

    /**
     * Constructor
     */
    public OTFont(final OTFontCollection fc) {
        _fc = fc;
    }
    public StringBuilder getName(final int nameIndex, StringBuilder sb) {
        if(null == sb) {
            sb = new StringBuilder();
        }
        return _name.getRecordsRecordString(sb, nameIndex);
    }

    public StringBuilder getAllNames(StringBuilder sb, final String separator) {
        if(null != _name) {
            if(null == sb) {
                sb = new StringBuilder();
            }
            for(int i=0; i<_name.getNumberOfNameRecords(); i++) {
                _name.getRecord(i).getRecordString(sb).append(separator);
            }
        }
        return sb;
    }

    public com.opengrabeso.ogltext.font.typecast.ot.table.Table getTable(final int tableType) {
        for (int i = 0; i < _tables.length; i++) {
            if ((_tables[i] != null) && (_tables[i].getType() == tableType)) {
                return _tables[i];
            }
        }
        return null;
    }

    public com.opengrabeso.ogltext.font.typecast.ot.table.Os2Table getOS2Table() {
        return _os2;
    }

    public com.opengrabeso.ogltext.font.typecast.ot.table.CmapTable getCmapTable() {
        return _cmap;
    }

    public com.opengrabeso.ogltext.font.typecast.ot.table.HeadTable getHeadTable() {
        return _head;
    }

    public com.opengrabeso.ogltext.font.typecast.ot.table.HheaTable getHheaTable() {
        return _hhea;
    }

    public com.opengrabeso.ogltext.font.typecast.ot.table.HdmxTable getHdmxTable() {
        return _hdmx;
    }

    public com.opengrabeso.ogltext.font.typecast.ot.table.HmtxTable getHmtxTable() {
        return _hmtx;
    }

    public com.opengrabeso.ogltext.font.typecast.ot.table.LocaTable getLocaTable() {
        return _loca;
    }

    public com.opengrabeso.ogltext.font.typecast.ot.table.MaxpTable getMaxpTable() {
        return _maxp;
    }

    public com.opengrabeso.ogltext.font.typecast.ot.table.NameTable getNameTable() {
        return _name;
    }

    public com.opengrabeso.ogltext.font.typecast.ot.table.PostTable getPostTable() {
        return _post;
    }

    public VheaTable getVheaTable() {
        return _vhea;
    }

    public int getAscent() {
        return _hhea.getAscender();
    }

    public int getDescent() {
        return _hhea.getDescender();
    }

    public int getNumGlyphs() {
        return _maxp.getNumGlyphs();
    }

    public OTGlyph getGlyph(final int i) {

        final GlyfDescript _glyfDescr = _glyf.getDescription(i);
        return (null != _glyfDescr)
            ? new OTGlyph(
                _glyfDescr,
                _hmtx.getLeftSideBearing(i),
                _hmtx.getAdvanceWidth(i))
            : null;
    }

    public com.opengrabeso.ogltext.font.typecast.ot.table.TableDirectory getTableDirectory() {
        return _tableDirectory;
    }

    private com.opengrabeso.ogltext.font.typecast.ot.table.Table readTable(
            final DataInputStream dis,
            final int tablesOrigin,
            final int tag) throws IOException {
        dis.reset();
        final com.opengrabeso.ogltext.font.typecast.ot.table.DirectoryEntry entry = _tableDirectory.getEntryByTag(tag);
        if (entry == null) {
            return null;
        }
        dis.skip(tablesOrigin + entry.getOffset());
        return com.opengrabeso.ogltext.font.typecast.ot.table.TableFactory.create(_fc, this, entry, dis);
    }

    /**
     * @param dis OpenType/TrueType font file data.
     * @param directoryOffset The Table Directory offset within the file.  For a
     * regular TTF/OTF file this will be zero, but for a TTC (Font Collection)
     * the offset is retrieved from the TTC header.  For a Mac font resource,
     * offset is retrieved from the resource headers.
     * @param tablesOrigin The point the table offsets are calculated from.
     * Once again, in a regular TTF file, this will be zero.  In a TTC is is
     * also zero, but within a Mac resource, it is the beggining of the
     * individual font resource data.
     */
    protected void read(
            final DataInputStream dis,
            final int directoryOffset,
            final int tablesOrigin) throws IOException {

        // Load the table directory
        dis.reset();
        dis.skip(directoryOffset);
        _tableDirectory = new TableDirectory(dis);
        _tables = new com.opengrabeso.ogltext.font.typecast.ot.table.Table[_tableDirectory.getNumTables()];

        // Load some prerequisite tables
        _head = (HeadTable) readTable(dis, tablesOrigin, com.opengrabeso.ogltext.font.typecast.ot.table.Table.head);
        _hhea = (HheaTable) readTable(dis, tablesOrigin, com.opengrabeso.ogltext.font.typecast.ot.table.Table.hhea);
        _maxp = (MaxpTable) readTable(dis, tablesOrigin, com.opengrabeso.ogltext.font.typecast.ot.table.Table.maxp);
        _loca = (LocaTable) readTable(dis, tablesOrigin, com.opengrabeso.ogltext.font.typecast.ot.table.Table.loca);
        _vhea = (VheaTable) readTable(dis, tablesOrigin, com.opengrabeso.ogltext.font.typecast.ot.table.Table.vhea);

        int index = 0;
        _tables[index++] = _head;
        _tables[index++] = _hhea;
        _tables[index++] = _maxp;
        if (_loca != null) {
            _tables[index++] = _loca;
        }
        if (_vhea != null) {
            _tables[index++] = _vhea;
        }

        // Load all other tables
        for (int i = 0; i < _tableDirectory.getNumTables(); i++) {
            final DirectoryEntry entry = _tableDirectory.getEntry(i);
            if (entry.getTag() == com.opengrabeso.ogltext.font.typecast.ot.table.Table.head
                    || entry.getTag() == com.opengrabeso.ogltext.font.typecast.ot.table.Table.hhea
                    || entry.getTag() == com.opengrabeso.ogltext.font.typecast.ot.table.Table.maxp
                    || entry.getTag() == com.opengrabeso.ogltext.font.typecast.ot.table.Table.loca
                    || entry.getTag() == com.opengrabeso.ogltext.font.typecast.ot.table.Table.vhea) {
                continue;
            }
            dis.reset();
            dis.skip(tablesOrigin + entry.getOffset());
            _tables[index] = TableFactory.create(_fc, this, entry, dis);
            ++index;
        }

        // Get references to commonly used tables (these happen to be all the
        // required tables)
        _cmap = (CmapTable) getTable(com.opengrabeso.ogltext.font.typecast.ot.table.Table.cmap);
        _hdmx = (HdmxTable) getTable(com.opengrabeso.ogltext.font.typecast.ot.table.Table.hdmx);
        _hmtx = (HmtxTable) getTable(com.opengrabeso.ogltext.font.typecast.ot.table.Table.hmtx);
        _name = (NameTable) getTable(com.opengrabeso.ogltext.font.typecast.ot.table.Table.name);
        _os2 = (Os2Table) getTable(com.opengrabeso.ogltext.font.typecast.ot.table.Table.OS_2);
        _post = (PostTable) getTable(com.opengrabeso.ogltext.font.typecast.ot.table.Table.post);

        // If this is a TrueType outline, then we'll have at least the
        // 'glyf' table (along with the 'loca' table)
        _glyf = (GlyfTable) getTable(Table.glyf);
    }

    @Override
    public String toString() {
        if (_tableDirectory != null) {
            return _tableDirectory.toString();
        } else {
            return "Empty font";
        }
    }
}
